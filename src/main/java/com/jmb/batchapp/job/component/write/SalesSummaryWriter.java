package com.jmb.batchapp.job.component.write;

import com.jmb.batchapp.job.component.transformation.mapper.SellerSalesDBMapper;
import com.jmb.batchapp.persistence.manager.DatabaseConnManager;
import com.jmb.batchapp.run.JobMetrics;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.catalyst.encoders.RowEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SalesSummaryWriter implements Writer<Dataset<Row>>{

    private static final Logger LOGGER = LoggerFactory.getLogger(SalesSummaryWriter.class);

    private DatabaseConnManager databaseConnManager;
    private JobMetrics metrics;

    @Autowired
    public SalesSummaryWriter(DatabaseConnManager databaseConnManager, JobMetrics metrics) {
        this.databaseConnManager = databaseConnManager;
        this.metrics = metrics;
    }

    @Override
    public void write(Dataset<Row> input) {
        Dataset<Row> processedDataset = input.mapPartitions(new SellerSalesDBMapper(databaseConnManager),
                RowEncoder.apply(input.schema()));
        long rows = processedDataset.count();
        metrics.addRowsWritten(rows);
        LOGGER.info("Total Amount of Rows Processed in SalesSummaryWriter " + rows);
    }
}
