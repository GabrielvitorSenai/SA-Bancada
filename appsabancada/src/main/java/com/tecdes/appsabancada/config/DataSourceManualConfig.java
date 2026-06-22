package com.tecdes.appsabancada.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

@Configuration
public class DataSourceManualConfig {

    @Autowired
    private Environment environment;

    @Bean
    @Primary
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .url(environment.getProperty("spring.datasource.url"))
                .username(environment.getProperty("spring.datasource.username"))
                .password(environment.getProperty("spring.datasource.password"))
                .build();
    }
}
