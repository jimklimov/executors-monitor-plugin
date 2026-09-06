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
 * Reports the "busy/total" executor counts of each node on the Nodes overview page.
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
        return new Executors(c.countBusy(), c.countExecutors());
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends AbstractNodeMonitorDescriptor<Executors> {

        @Override
        protected Executors monitor(Computer c) {
            return new Executors(c.countBusy(), c.countExecutors());
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

        Executors(int busy, int total) {
            this.busy = busy;
            this.total = total;
        }

        public int getBusy() {
            return busy;
        }

        public int getTotal() {
            return total;
        }

        @Override
        public String toString() {
            return busy + "/" + total;
        }
    }
}
