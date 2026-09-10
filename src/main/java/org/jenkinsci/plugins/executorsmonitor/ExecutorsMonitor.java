package org.jenkinsci.plugins.executorsmonitor;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.node_monitors.AbstractNodeMonitorDescriptor;
import hudson.node_monitors.NodeMonitor;
import hudson.util.FormValidation;
import java.io.Serializable;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

/**
 * Reports the "busy/total/configured" executor counts of each node on the Nodes overview page.
 *
 * "total" is the number of executors currently allowed to be live on the computer,
 * which may temporarily differ from "configured" (the node's configured executor
 * count) if that count was lowered while some of its executors were still busy.
 *
 * This is purely informational: unlike the disk space or response time monitors,
 * it does not define any threshold and never takes a node offline.
 */
public class ExecutorsMonitor extends NodeMonitor {

    private boolean colorize;
    private boolean countQueue;
    private int queueThresholdFactor = 5;

    @DataBoundConstructor
    public ExecutorsMonitor() {}

    /**
     * Whether the busy/total counts should be colorized (blue/green/red) depending on
     * how busy the node is. Defaults to {@code false}, i.e. the counts are rendered
     * in the default color unless this is explicitly enabled.
     */
    public boolean isColorize() {
        return colorize;
    }

    @DataBoundSetter
    public void setColorize(boolean colorize) {
        this.colorize = colorize;
    }

    /**
     * Whether to also report the number of buildable queue items that this node could
     * pick up, similar to the per-agent build queue shown by the computer-queue-plugin.
     * Defaults to {@code false}, since it requires scanning the whole build queue for
     * every node on every page load.
     */
    public boolean isCountQueue() {
        return countQueue;
    }

    @DataBoundSetter
    public void setCountQueue(boolean countQueue) {
        this.countQueue = countQueue;
    }

    /**
     * When {@link #isColorize()} and {@link #isCountQueue()} are both enabled, the queued
     * count is colorized blue when it is 0, green while it does not exceed the configured
     * executor count, uncolored while it does not exceed {@code configured * N}, and red
     * beyond that. Must be greater than 1 if set; defaults to 5.
     */
    public int getQueueThresholdFactor() {
        return queueThresholdFactor;
    }

    @DataBoundSetter
    public void setQueueThresholdFactor(int queueThresholdFactor) {
        this.queueThresholdFactor = queueThresholdFactor;
    }

    @Override
    public String getColumnCaption() {
        String caption = getDescriptor().getDisplayName();
        if (!countQueue) {
            return caption;
        }
        return caption.endsWith(")") ? caption.substring(0, caption.length() - 1) + "/queued)" : caption + "/queued";
    }

    @Override
    public Object data(Computer c) {
        // Executor counts are already held in memory on the controller,
        // so there is no need to go through the periodic monitor()/get()
        // caching used by common monitors that need to contact the node.
        Integer queued = countQueue ? countQueuedFor(c) : null;
        return new Executors(c.countBusy(), c.countExecutors(), c.getNumExecutors(), queued);
    }

    /**
     * Counts the buildable queue items that this computer's node could take, using the same
     * detection method as the computer-queue-plugin's per-agent build queue widget. An item
     * counted here is not necessarily exclusive to this node: another suitable node might
     * pick it up first (e.g. if it comes online, or finishes some other work earlier).
     */
    private static int countQueuedFor(Computer c) {
        Node node = c.getNode();
        if (node == null) {
            return 0;
        }
        int count = 0;
        for (Queue.Item item : Queue.getInstance().getItems()) {
            if (item instanceof Queue.BuildableItem buildableItem && node.canTake(buildableItem) == null) {
                count++;
            }
        }
        return count;
    }

    @Extension
    @Symbol("executors")
    public static final class DescriptorImpl extends AbstractNodeMonitorDescriptor<Executors> {

        @Override
        protected Executors monitor(Computer c) {
            // Not used to render the Nodes overview column (see ExecutorsMonitor#data),
            // so the optional queued-items count is intentionally left out here.
            return new Executors(c.countBusy(), c.countExecutors(), c.getNumExecutors());
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.executorsmonitor_displayName();
        }

        @Override
        public boolean canTakeOffline() {
            return false;
        }

        public FormValidation doCheckQueueThresholdFactor(@QueryParameter String value) {
            try {
                if (Integer.parseInt(value) <= 1) {
                    return FormValidation.error("Must be greater than 1");
                }
            } catch (NumberFormatException e) {
                return FormValidation.error("Must be a number");
            }
            return FormValidation.ok();
        }
    }

    public static final class Executors implements Serializable {
        private static final long serialVersionUID = 1L;

        private final int busy;
        private final int total;
        private final int configured;
        private final Integer queued;

        Executors(int busy, int total, int configured) {
            this(busy, total, configured, null);
        }

        Executors(int busy, int total, int configured, Integer queued) {
            this.busy = busy;
            this.total = total;
            this.configured = configured;
            this.queued = queued;
        }

        public int getBusy() {
            return busy;
        }

        /**
         * The number of {@link hudson.model.Executor}s currently allowed to be live on the computer.
         * May temporarily differ from {@link #getConfigured()} if the configured
         * executor count was lowered while tasks were still running on it.
         */
        public int getTotal() {
            return total;
        }

        /**
         * The number of executors configured on the node itself, regardless of how
         * many are momentarily live on the computer.
         */
        public int getConfigured() {
            return configured;
        }

        /**
         * The number of buildable queue items that this node could take, or {@code null} if
         * that count was not requested (see {@link ExecutorsMonitor#isCountQueue()}).
         */
        public Integer getQueued() {
            return queued;
        }

        @Override
        public String toString() {
            String s = busy + "/" + total + "/" + configured;
            return queued != null ? s + " q:" + queued : s;
        }
    }
}
