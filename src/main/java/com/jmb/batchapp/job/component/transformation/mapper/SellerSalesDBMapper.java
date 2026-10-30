package com.jmb.batchapp.job.component.transformation.mapper;

import com.jmb.batchapp.dictionary.SalesSummaryQueries;
import com.jmb.batchapp.persistence.manager.DatabaseConnManager;
import com.jmb.batchapp.schema.SalesSummarySchema;
import org.apache.spark.api.java.function.MapPartitionsFunction;
import org.apache.spark.sql.Row;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Writes summary rows to SALES_SUMMARY. Each (seller, date, item) is replaced rather than appended, so a
 * re-run of the same job is idempotent. Returns the rows written (the original returned a consumed iterator).
 */
public class SellerSalesDBMapper implements MapPartitionsFunction<Row, Row> {

    private DatabaseConnManager connManager;

    public SellerSalesDBMapper(DatabaseConnManager connManager) {
        this.connManager = connManager;
    }

    @Override
    public Iterator<Row> call(Iterator<Row> rowIterator) throws Exception {
        return doRowsBatchInsert(rowIterator);
    }

    private Iterator<Row> doRowsBatchInsert(Iterator<Row> rowIterator) throws Exception {
        List<Row> written = new ArrayList<>();
        try (Connection connection = connManager.getDataSource().getConnection();
             PreparedStatement delete = connection.prepareStatement(SalesSummaryQueries.DELETE_SALES_SUMMARY);
             PreparedStatement stmt = connection.prepareStatement(SalesSummaryQueries.INSERT_SALES_SUMMARY)) {
            while(rowIterator.hasNext()) {
                Row row = rowIterator.next();
                String seller = row.getAs(SalesSummarySchema.SELLER_ID.name());
                java.sql.Date date = row.getAs(SalesSummarySchema.SALES_DATE.name());
                String item = row.getAs(SalesSummarySchema.PRODUCT.name());
                delete.setString(1, seller);
                delete.setDate(2, date);
                delete.setString(3, item);
                delete.addBatch();
                stmt.setString(1, seller);
                stmt.setDate(2, date);
                stmt.setString(3, item);
                stmt.setInt(4, ((Number) row.getAs(SalesSummarySchema.QUANTITY.name())).intValue());
                stmt.addBatch();
                written.add(row);
            }
            delete.executeBatch();
            stmt.executeBatch();
        }
        return written.iterator();
    }
}
