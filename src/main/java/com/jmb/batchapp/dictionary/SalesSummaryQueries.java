package com.jmb.batchapp.dictionary;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class SalesSummaryQueries {

    public static final String SALES_TABLE = "SALES";
    public static final String SALES_SUMMARY_TABLE = "SALES_SUMMARY";
    public static final String STRING_TOKEN = "%s";

    /**
     * Portable SQL (H2 and PostgreSQL). The original used H2's PARSEDATETIME; the date is now parsed and
     * validated in Java and passed as an ANSI DATE literal.
     */
    public static final String SALES_PER_SELLER_ID_DATE_PROD = "SELECT SELLER_ID, SALES_DATE, PRODUCT, QUANTITY FROM "
            + SALES_TABLE + " WHERE " + " SALES_DATE = DATE '" + STRING_TOKEN + "' "
            + " AND PRODUCT = '" + STRING_TOKEN + "' "
            + "AND SELLER_ID IN " + STRING_TOKEN;

    public static final String IN_CLAUSE = "";

    public static final String INSERT_SALES_SUMMARY = "INSERT INTO " + SALES_SUMMARY_TABLE + " (SELLER_ID, SALES_DATE, " +
            "ITEM, TOTAL_QUANTITY) VALUES (?,?,?,?)";

    public static final String DELETE_SALES_SUMMARY = "DELETE FROM " + SALES_SUMMARY_TABLE
            + " WHERE SELLER_ID = ? AND SALES_DATE = ? AND ITEM = ?";

    private static final DateTimeFormatter JOB_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static String buildSalesQuery(String date, String product, String commaSeparatedSellers) {
        return String.format(SALES_PER_SELLER_ID_DATE_PROD, toIsoDate(date), escape(product), buildInClause(commaSeparatedSellers));
    }

    /** Accepts the job's original yyyyMMddHHmmss format (e.g. 20201111000000) or ISO yyyy-MM-dd. */
    public static String toIsoDate(String date) {
        if (date == null) {
            throw new IllegalArgumentException("date parameter is required (yyyyMMddHHmmss or yyyy-MM-dd)");
        }
        try {
            return LocalDateTime.parse(date, JOB_DATE).toLocalDate().toString();
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(date).toString();
            } catch (DateTimeParseException e2) {
                throw new IllegalArgumentException("Invalid date '" + date + "' (expected yyyyMMddHHmmss or yyyy-MM-dd)");
            }
        }
    }

    public static String buildInClause(String commaSeparatedValues) {
        if (commaSeparatedValues == null || commaSeparatedValues.trim().isEmpty()) {
            throw new IllegalArgumentException("sellerId parameter is required (comma separated)");
        }
        StringBuilder builder = new StringBuilder();
        String regexComma = ",", regexApostrophe = "'", openBracket = "(", closeBracket = ")";
        String[] sellerIds = commaSeparatedValues.split(regexComma);
        builder.append(openBracket);
        for(int i = 0; i < sellerIds.length; i++) {
            builder.append(regexApostrophe + escape(sellerIds[i].trim()) + regexApostrophe);
            if(i + 1 != sellerIds.length) {
                builder.append(regexComma);
            }
        }
        builder.append(closeBracket);
        return builder.toString();
    }

    /** Doubles single quotes so a parameter cannot break out of its SQL string literal. */
    static String escape(String value) {
        if (value == null) {
            throw new IllegalArgumentException("product parameter is required");
        }
        return value.replace("'", "''");
    }
}
