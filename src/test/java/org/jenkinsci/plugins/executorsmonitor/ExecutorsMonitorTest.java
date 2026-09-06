package org.jenkinsci.plugins.executorsmonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jenkinsci.plugins.executorsmonitor.ExecutorsMonitor.Executors;
import org.junit.jupiter.api.Test;

class ExecutorsMonitorTest {

    @Test
    void toStringIsBusySlashTotal() {
        assertEquals("2/5", new Executors(2, 5).toString());
    }

    @Test
    void toStringWithNoExecutors() {
        assertEquals("0/0", new Executors(0, 0).toString());
    }

    @Test
    void exposesBusyAndTotal() {
        Executors executors = new Executors(3, 4);
        assertEquals(3, executors.getBusy());
        assertEquals(4, executors.getTotal());
    }
}
