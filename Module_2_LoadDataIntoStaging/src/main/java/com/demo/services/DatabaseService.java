package com.demo.services;
import com.demo.repository.ConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.Map;

@Service
public class DatabaseService {

    @Autowired
    private ConfigRepository configRepository;

    public DataSource connectToStagingDatabase() {
        Map<String, Object> config = configRepository.getStagingConfig();
        String url = "jdbc:mysql://" + config.get("staging_source_host") + "/" + config.get("staging_source_name");
        String username = (String) config.get("staging_source_username");
        String password = (String) config.get("staging_source_password");

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        return dataSource;
    }
}
