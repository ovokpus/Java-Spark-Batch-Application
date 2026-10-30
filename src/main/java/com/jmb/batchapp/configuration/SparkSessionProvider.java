package com.jmb.batchapp.configuration;

import org.apache.spark.sql.SparkSession;

import java.util.Objects;

/**
 * Class provides Jobs with a Spark Session. It implements a Singleton pattern to provide just one
 * session per job.
 *
 * @author JuanMBruno
 */
public final class SparkSessionProvider {

    private SparkSessionProvider(){}

    private static SparkSession sparkSession;

    public static synchronized SparkSession provideSession(String masterMode) {
        if(Objects.isNull(sparkSession)) {
            SparkSession.Builder sessionBuilder =  SparkSession.builder()
                    .appName("SparkBatchApp")
                    // Small local jobs: avoid 200 shuffle partitions and the UI port.
                    .config("spark.sql.shuffle.partitions", System.getProperty("spark.sql.shuffle.partitions", "4"))
                    .config("spark.ui.enabled", System.getProperty("spark.ui.enabled", "false"));
            if(Objects.nonNull(masterMode)) {
                sessionBuilder = sessionBuilder.master(masterMode);
            }
            sparkSession = sessionBuilder.getOrCreate();
        }
        return sparkSession;
    }

    public static synchronized void stop() {
        if (Objects.nonNull(sparkSession)) {
            sparkSession.stop();
            sparkSession = null;
        }
    }
}
