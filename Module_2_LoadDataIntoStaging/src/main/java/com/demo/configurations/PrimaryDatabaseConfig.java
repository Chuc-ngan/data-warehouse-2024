package com.demo.configurations;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.demo.repository.primary", // Package chứa các repository cho database thứ nhất
        entityManagerFactoryRef = "primaryEntityManagerFactory", // Tham chiếu đến EntityManagerFactory của database này
        transactionManagerRef = "primaryTransactionManager" // Tham chiếu đến TransactionManager của database này
)
public class PrimaryDatabaseConfig {
    @Primary
    @Bean(name = "primaryDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.primary") // Sử dụng prefix để lấy thông tin từ file cấu hình
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build(); // Tạo DataSource cho database thứ nhất
    }

    @Primary
    @Bean(name = "primaryEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean primaryEntityManagerFactory(EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(primaryDataSource()) // Sử dụng DataSource đã tạo
                .packages("com.demo.entities") // Package chứa các entity của database thứ nhất
                .persistenceUnit("primary") // Đặt tên cho persistence unit
                .build();
    }

    @Primary
    @Bean(name = "primaryTransactionManager")
    public PlatformTransactionManager primaryTransactionManager(
            @Qualifier("primaryEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory); // Tạo TransactionManager từ EntityManagerFactory
    }
}
