package com.jmb.batchapp.dictionary;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalesSummaryQueriesTest {

    @Test
    void parsesOriginalAndIsoDateFormats() {
        assertEquals("2020-11-11", SalesSummaryQueries.toIsoDate("20201111000000"));
        assertEquals("2020-11-13", SalesSummaryQueries.toIsoDate("2020-11-13"));
        assertThrows(IllegalArgumentException.class, () -> SalesSummaryQueries.toIsoDate("11/11/2020"));
    }

    @Test
    void buildsQuotedInClauseAndEscapesQuotes() {
        assertEquals("('Joe','Beth')", SalesSummaryQueries.buildInClause("Joe, Beth"));
        assertEquals("('O''Brien')", SalesSummaryQueries.buildInClause("O'Brien"));
        assertThrows(IllegalArgumentException.class, () -> SalesSummaryQueries.buildInClause(" "));
    }

    @Test
    void buildsPortableQueryWithoutH2Functions() {
        String q = SalesSummaryQueries.buildSalesQuery("20201112000000", "Beer", "Joe");
        assertTrue(q.contains("SALES_DATE = DATE '2020-11-12'"), q);
        assertTrue(q.contains("PRODUCT = 'Beer'"), q);
        assertTrue(q.endsWith("SELLER_ID IN ('Joe')"), q);
        assertTrue(!q.contains("PARSEDATETIME"), q);
    }
}
