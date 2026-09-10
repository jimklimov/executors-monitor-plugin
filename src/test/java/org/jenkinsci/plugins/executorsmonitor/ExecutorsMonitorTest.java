package org.jenkinsci.plugins.executorsmonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jenkinsci.plugins.executorsmonitor.ExecutorsMonitor.Executors;
import org.junit.jupiter.api.Test;

class ExecutorsMonitorTest {

    @Test
    void toStringIsBusySlashTotalSlashConfigured() {
        assertEquals("2/5/5", new Executors(2, 5, 5).toString());
    }

    @Test
    void toStringWithNoExecutors() {
        assertEquals("0/0/0", new Executors(0, 0, 0).toString());
    }

    @Test
    void exposesBusyTotalAndConfigured() {
        Executors executors = new Executors(3, 4, 2);
        assertEquals(3, executors.getBusy());
        assertEquals(4, executors.getTotal());
        assertEquals(2, executors.getConfigured());
    }

    @Test
    void queuedIsNullWhenNotRequested() {
        assertEquals(null, new Executors(2, 5, 5).getQueued());
    }

    @Test
    void toStringAppendsQueuedToken() {
        assertEquals("3/3/2 q:5", new Executors(3, 3, 2, 5).toString());
    }

    @Test
    void exposesQueued() {
        assertEquals(5, new Executors(3, 3, 2, 5).getQueued());
    }

    @Test
    void readResolveRepairsUnsetQueueThresholdFactor() {
        ExecutorsMonitor monitor = new ExecutorsMonitor();
        monitor.setQueueThresholdFactor(0);
        monitor.readResolve();
        assertEquals(5, monitor.getQueueThresholdFactor());
    }

    @Test
    void readResolveKeepsValidQueueThresholdFactor() {
        ExecutorsMonitor monitor = new ExecutorsMonitor();
        monitor.setQueueThresholdFactor(3);
        monitor.readResolve();
        assertEquals(3, monitor.getQueueThresholdFactor());
    }
}
