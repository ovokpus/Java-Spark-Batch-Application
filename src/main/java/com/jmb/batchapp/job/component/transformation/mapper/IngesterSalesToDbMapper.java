package com.jmb.batchapp.job.component.transformation.mapper;

import com.jmb.batchapp.dictionary.IngesterQueries;
import com.jmb.batchapp.persistence.manager.DatabaseConnManager;
import com.jmb.batchapp.schema.SalesSchema;
import org.apache.spark.api.java.function.MapPartitionsFunction;
import org.apache.spark.sql.Row;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Batch-inserts a partition's rows into SALES and returns the rows written. The original returned the
 * already-consumed iterator, so the downstream count() was always 0.
 */
public class IngesterSalesToDbMapper implements MapPartitionsFunction<Row, Row> {

    private DatabaseConnManager connManager;

    public IngesterSalesToDbMapper(DatabaseConnManager connManager) {
        this.connManager = connManager;
    }

    @Override
    public Iterator<Row> call(Iterator<Row> iterator) throws Exception {
        return doRowsBatchInsert(iterator);
    }

    private Iterator<Row> doRowsBatchInsert(Iterator<Row> iterator) throws SQLException {
        List<Row> written = new ArrayList<>();
        try (Connection connection = connManager.getDataSource().getConnection();
             PreparedStatement stmt = connection.prepareStatement(IngesterQueries.INSERT_QUERY_SALES)) {
            while(iterator.hasNext()) {
                int index = 1;
                Row row = iterator.next();
                stmt.setString(index++, row.getAs(SalesSchema.SELLER_ID.name()));
                stmt.setDate(index++, row.getAs(SalesSchema.DATE.name()));
                stmt.setString(index++, row.getAs(SalesSchema.PRODUCT.name()));
                stmt.setLong(index++, row.getAs(SalesSchema.QUANTITY.name()));
                stmt.addBatch();
                written.add(row);
            }
            stmt.executeBatch();
        }
        return written.iterator();
    }
}
