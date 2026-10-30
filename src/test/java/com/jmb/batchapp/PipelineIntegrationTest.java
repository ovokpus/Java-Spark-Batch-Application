package com.jmb.batchapp;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the real jobs end to end on local Spark against the in-memory H2 database:
 * ingest src/main/resources/data/ingestion/sales.json, then summarise one date/product.
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PipelineIntegrationTest {

    @Autowired
    private SparkBatchApp app;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @Order(1)
    void ingesterJobLoadsEverySaleItemFromJson() {
        app.run("jobName=ingesterJob", "clientId=client1", "readFormat=json", "fileName=sales", "mode=overwrite");

        // 9 seller/day entries with 24 line items in sales.json
        assertEquals(24, jdbc.queryForObject("SELECT COUNT(*) FROM SALES", Integer.class));
        assertEquals(387, jdbc.queryForObject("SELECT SUM(QUANTITY) FROM SALES", Integer.class));
        Map<String, Object> run = latestRun();
        assertEquals("SUCCEEDED", run.get("STATUS"));
        assertEquals(24L, ((Number) run.get("ROWS_WRITTEN")).longValue());
    }

    @Test
    @Order(2)
    void salesSummaryJobAggregatesPerSellerAndIsIdempotent() {
        String[] args = {"jobName=salesSummaryJob", "sellerId=Joe,Beth,Janet", "date=20201112000000", "product=Beer"};
        app.run(args);
        app.run(args);  // a re-run replaces rather than duplicates

        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT SELLER_ID, TOTAL_QUANTITY FROM SALES_SUMMARY WHERE SALES_DATE = DATE '2020-11-12' AND ITEM = 'Beer' ORDER BY SELLER_ID");
        // Joe 19+16+14, Beth 15; Janet sold no Beer that day, so she is skipped (the original threw on empty reduce)
        assertEquals(2, rows.size());
        assertEquals("Beth", rows.get(0).get("SELLER_ID"));
        assertEquals(15, ((Number) rows.get(0).get("TOTAL_QUANTITY")).intValue());
        assertEquals("Joe", rows.get(1).get("SELLER_ID"));
        assertEquals(49, ((Number) rows.get(1).get("TOTAL_QUANTITY")).intValue());
        assertEquals(2L, ((Number) latestRun().get("ROWS_WRITTEN")).longValue());
    }

    @Test
    @Order(3)
    void invalidParametersAreRecordedAsFailedRun() {
        assertThrows(IllegalArgumentException.class, () ->
                app.run("jobName=salesSummaryJob", "sellerId=Joe", "date=not-a-date", "product=Beer"));
        Map<String, Object> run = latestRun();
        assertEquals("FAILED", run.get("STATUS"));
        assertTrue(String.valueOf(run.get("ERROR")).contains("Invalid date"));
    }

    private Map<String, Object> latestRun() {
        return jdbc.queryForMap("SELECT * FROM JOB_RUN ORDER BY STARTED_AT DESC LIMIT 1");
    }
}
