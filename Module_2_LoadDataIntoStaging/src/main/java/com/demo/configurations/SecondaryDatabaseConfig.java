package com.demo.configurations;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.demo.repository.secondary", // Package chứa các repository cho database thứ hai
        entityManagerFactoryRef = "secondaryEntityManagerFactory", // Tham chiếu đến EntityManagerFactory của database này
        transactionManagerRef = "secondaryTransactionManager" // Tham chiếu đến TransactionManager của database này
)
public class SecondaryDatabaseConfig {
    @Bean(name = "secondaryDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.secondary") // Sử dụng prefix để lấy thông tin từ file cấu hình
    public DataSource secondaryDataSource() {
        return DataSourceBuilder.create().build(); // Tạo DataSource cho database thứ hai
    }

    @Bean(name = "secondaryEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean secondaryEntityManagerFactory(EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(secondaryDataSource()) // Sử dụng DataSource đã tạo
                .packages("com.example.model.secondary") // Package chứa các entity của database thứ hai
                .persistenceUnit("secondary") // Đặt tên cho persistence unit
                .build();
    }

    @Bean(name = "secondaryTransactionManager")
    public PlatformTransactionManager secondaryTransactionManager(
            @Qualifier("secondaryEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory); // Tạo TransactionManager từ EntityManagerFactory
    }
}
