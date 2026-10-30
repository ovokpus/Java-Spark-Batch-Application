package com.jmb.batchapp.persistence.manager;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class configures a Database Connection Manager which uses a Connection Pool and provides functionality
 * to retrieve connections based on this. It is serialized into Spark tasks, so the pool is kept in a
 * static per-JVM cache keyed by URL instead of being created on every call (the original leaked a new
 * pool per partition).
 *
 * @author JuanMBruno
 */
@Component
public class DatabaseConnManager implements Serializable {

    private static final Map<String, HikariDataSource> POOLS = new ConcurrentHashMap<>();

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name}")
    private String driver;

    public DataSource getDataSource() {
        return POOLS.computeIfAbsent(dbUrl + "|" + username, key -> {
            HikariDataSource hikariDataSource = new HikariDataSource();
            hikariDataSource.setJdbcUrl(dbUrl);
            hikariDataSource.setUsername(username);
            hikariDataSource.setPassword(password);
            hikariDataSource.setDriverClassName(driver);
            hikariDataSource.setMaximumPoolSize(4);
            return hikariDataSource;
        });
    }

    //Getters and Setters
    public String getDbUrl() {
        return dbUrl;
    }

    public void setDbUrl(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }
}
