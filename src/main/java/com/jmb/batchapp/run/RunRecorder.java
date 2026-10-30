package com.jmb.batchapp.run;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

/** Records one row per job execution in JOB_RUN (start, end, status, rows written, error). */
@Component
public class RunRecorder {

    private static final int MAX_TEXT = 1000;

    private final JdbcTemplate jdbc;

    @Autowired
    public RunRecorder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public String start(String jobName, String[] args) {
        String id = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO JOB_RUN (ID, JOB_NAME, PARAMS, STATUS, STARTED_AT) VALUES (?, ?, ?, 'RUNNING', ?)",
                id, jobName, truncate(String.join(" ", args)), Timestamp.from(Instant.now()));
        return id;
    }

    public void succeed(String id, long rowsWritten) {
        jdbc.update("UPDATE JOB_RUN SET STATUS = 'SUCCEEDED', FINISHED_AT = ?, ROWS_WRITTEN = ? WHERE ID = ?",
                Timestamp.from(Instant.now()), rowsWritten, id);
    }

    public void fail(String id, Throwable error) {
        jdbc.update("UPDATE JOB_RUN SET STATUS = 'FAILED', FINISHED_AT = ?, ERROR = ? WHERE ID = ?",
                Timestamp.from(Instant.now()), truncate(error.getClass().getSimpleName() + ": " + error.getMessage()), id);
    }

    static String truncate(String s) {
        return s == null || s.length() <= MAX_TEXT ? s : s.substring(0, MAX_TEXT);
    }
}
