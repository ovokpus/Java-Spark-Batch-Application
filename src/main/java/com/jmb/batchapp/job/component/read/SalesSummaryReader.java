package com.jmb.batchapp.job.component.read;

import com.jmb.batchapp.configuration.SparkSessionProvider;
import com.jmb.batchapp.dictionary.SalesSummaryQueries;
import com.jmb.batchapp.job.context.SalesSummaryContext;
import com.jmb.batchapp.parameter.SalesSummaryJobParams;
import com.jmb.batchapp.persistence.manager.DatabaseConnManager;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;

/**
 * Reader class implementation for the {@link com.jmb.batchapp.job.implementation.SalesSummaryJob}
 *
 * @author JuanMBruno
 */
@Component
public class SalesSummaryReader implements Reader<Dataset<Row>> {

    @Autowired
    private SalesSummaryContext salesSummaryContext;

    private DatabaseConnManager databaseConnManager;

    @Autowired
    public SalesSummaryReader(DatabaseConnManager databaseConnManager) {
        this.databaseConnManager = databaseConnManager;
    }

    //Read using SparkSQL component, DataFrameReader class, fires a query to the SALES table
    @Override
    public Dataset<Row> read() {
        String sellers = salesSummaryContext.getParams().getParamValue(SalesSummaryJobParams.SELLER_ID);
        String date = salesSummaryContext.getParams().getParamValue(SalesSummaryJobParams.DATE);
        String product = salesSummaryContext.getParams().getParamValue(SalesSummaryJobParams.PRODUCT);
        //Validate params and construct query
        String salesRetrieveQuery = SalesSummaryQueries.buildSalesQuery(date, product, sellers);
        return getDatasetFromDbTable(salesRetrieveQuery);
    }

    private Dataset<Row> getDatasetFromDbTable(String query) {
        Properties properties = new Properties();
        properties.put("user", databaseConnManager.getUsername());
        properties.put("password", databaseConnManager.getPassword());
        properties.put("driver", databaseConnManager.getDriver());
        Dataset<Row> df = SparkSessionProvider.provideSession(salesSummaryContext.getSparkMode())
                .read().jdbc(databaseConnManager.getDbUrl(), "(" + query + ") sales_alias", properties);
        // PostgreSQL reports lower-case column names (H2 upper-case); Row.getAs is case-sensitive.
        return df.toDF(Arrays.stream(df.columns()).map(c -> c.toUpperCase(Locale.ROOT)).toArray(String[]::new));
    }
}
