package org.jenkinsci.plugins.executorsmonitor;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Computer;
import hudson.node_monitors.AbstractNodeMonitorDescriptor;
import hudson.node_monitors.NodeMonitor;
import java.io.Serializable;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

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

    @Override
    public Object data(Computer c) {
        // Executor counts are already held in memory on the controller,
        // so there is no need to go through the periodic monitor()/get()
        // caching used by common monitors that need to contact the node.
        return new Executors(c.countBusy(), c.countExecutors(), c.getNumExecutors());
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends AbstractNodeMonitorDescriptor<Executors> {

        @Override
        protected Executors monitor(Computer c) {
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
    }

    public static final class Executors implements Serializable {
        private static final long serialVersionUID = 1L;

        private final int busy;
        private final int total;
        private final int configured;

        Executors(int busy, int total, int configured) {
            this.busy = busy;
            this.total = total;
            this.configured = configured;
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

        @Override
        public String toString() {
            return busy + "/" + total + "/" + configured;
        }
    }
}
