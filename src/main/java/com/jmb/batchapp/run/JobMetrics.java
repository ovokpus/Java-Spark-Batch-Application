package com.jmb.batchapp.run;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/** Counters a job's writers report into, so each run's row count can be recorded in JOB_RUN. */
@Component
public class JobMetrics {

    private final AtomicLong rowsWritten = new AtomicLong();

    public void reset() {
        rowsWritten.set(0);
    }

    public void addRowsWritten(long rows) {
        rowsWritten.addAndGet(rows);
    }

    public long getRowsWritten() {
        return rowsWritten.get();
    }
}
