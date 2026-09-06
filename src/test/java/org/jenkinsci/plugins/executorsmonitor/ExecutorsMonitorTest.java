package org.jenkinsci.plugins.executorsmonitor;

import static org.junit.Assert.assertEquals;

import org.jenkinsci.plugins.executorsmonitor.ExecutorsMonitor.Executors;
import org.junit.Test;

public class ExecutorsMonitorTest {

    @Test
    public void toStringIsBusySlashTotal() {
        assertEquals("2/5", new Executors(2, 5).toString());
    }

    @Test
    public void toStringWithNoExecutors() {
        assertEquals("0/0", new Executors(0, 0).toString());
    }

    @Test
    public void exposesBusyAndTotal() {
        Executors executors = new Executors(3, 4);
        assertEquals(3, executors.getBusy());
        assertEquals(4, executors.getTotal());
    }
}
