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
import java.util.concurrent.TimeUnit;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

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

    private static final int DEFAULT_QUEUE_THRESHOLD_FACTOR = 5;

    private boolean colorize;
    private boolean countQueue;
    private int queueThresholdFactor = DEFAULT_QUEUE_THRESHOLD_FACTOR;

    @DataBoundConstructor
    public ExecutorsMonitor() {}

    /**
     * XStream reconstructs already-persisted instances without calling the constructor, so
     * the {@link #queueThresholdFactor} field initializer above never runs for configs saved
     * before that field existed -- it silently comes back as the Java default of 0 instead.
     * Since 0 (and 1) are not valid values anyway (see {@link #getQueueThresholdFactor()}),
     * treat them as "unset" here and repair them to the real default.
     */
    protected Object readResolve() {
        if (queueThresholdFactor <= 1) {
            queueThresholdFactor = DEFAULT_QUEUE_THRESHOLD_FACTOR;
        }
        return this;
    }

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
        for (Queue.Item item : getQueueItems()) {
            if (item instanceof Queue.BuildableItem buildableItem && node.canTake(buildableItem) == null) {
                count++;
            }
        }
        return count;
    }

    private static final long QUEUE_SNAPSHOT_TTL_MILLIS = TimeUnit.SECONDS.toMillis(10);

    /**
     * Not part of this monitor's persisted state: a purely in-memory, best-effort cache of
     * the live build queue, shared by all nodes rendered on the same page. {@code static} so
     * it is never touched by XStream; {@code transient} and {@code volatile} to underline
     * that intent and keep the lock-free refresh below visible across threads.
     */
    private static transient volatile QueueSnapshot queueSnapshot;

    /**
     * Returns a recent (at most {@link #QUEUE_SNAPSHOT_TTL_MILLIS} old) snapshot of
     * {@link Queue#getItems()}, refreshing it first if it is missing or stale.
     *
     * <p>Nodes overview pages with many nodes would otherwise call {@code Queue.getItems()}
     * once per node/row, each call taking the live queue's internal lock and copying its
     * item list. Reusing a short-lived snapshot collapses that to about once per page load.
     * The refresh below is intentionally not synchronized: at worst a couple of threads race
     * to refresh an expired snapshot at the same time, which is far cheaper than adding a lock
     * of our own around the real queue's lock.
     */
    private static Queue.Item[] getQueueItems() {
        QueueSnapshot snapshot = queueSnapshot;
        long now = System.currentTimeMillis();
        if (snapshot == null || now - snapshot.timestamp >= QUEUE_SNAPSHOT_TTL_MILLIS) {
            snapshot = new QueueSnapshot(Queue.getInstance().getItems(), now);
            queueSnapshot = snapshot;
        }
        return snapshot.items;
    }

    /** Ephemeral cache entry, see {@link #queueSnapshot}. Never persisted. */
    private static final class QueueSnapshot {
        private final transient Queue.Item[] items;
        private final transient long timestamp;

        QueueSnapshot(Queue.Item[] items, long timestamp) {
            this.items = items;
            this.timestamp = timestamp;
        }
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

        @POST
        public FormValidation doCheckQueueThresholdFactor(@QueryParameter String value) {
            // Configuring node monitors requires Jenkins.MANAGE (see ComputerSet#doConfigSubmit);
            // this check just avoids performing validation for users who could not submit it anyway.
            if (!Jenkins.get().hasPermission(Jenkins.MANAGE)) {
                return FormValidation.ok();
            }
            try {
                if (Integer.parseInt(value) <= 1) {
                    return FormValidation.error("Must be greater than 1, invalid values will default to "
                            + ExecutorsMonitor.DEFAULT_QUEUE_THRESHOLD_FACTOR);
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
