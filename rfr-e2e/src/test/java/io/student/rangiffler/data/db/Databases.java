package io.student.rangiffler.data.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.student.rangiffler.config.TestConfig;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Test databases with their data sources and transaction managers.
 */
public enum Databases {

    AUTH(TestConfig.dbUrl() + "/rangiffler-auth"),
    API(TestConfig.dbUrl() + "/rangiffler-api");

    private final DataSource dataSource;
    private final PlatformTransactionManager transactionManager;

    /**
     * Creates a pooled data source and a transaction manager for the JDBC URL.
     */
    Databases(String jdbcUrl) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(TestConfig.dbUser());
        config.setPassword(TestConfig.dbPassword());
        config.setMaximumPoolSize(3);
        this.dataSource = new HikariDataSource(config);
        this.transactionManager = new DataSourceTransactionManager(dataSource);
    }

    public DataSource dataSource() {
        return dataSource;
    }

    public PlatformTransactionManager transactionManager() {
        return transactionManager;
    }
}
