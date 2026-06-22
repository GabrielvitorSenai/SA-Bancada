package com.tecdes.appsabancada.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class ApiUrlConfig {

    @Autowired
    private Environment environment;

    private String estoqueApiUrl;
    private String expedicaoApiUrl;

    @PostConstruct
    public void init() {
        this.estoqueApiUrl = environment.getProperty("estoque.api.url");
        this.expedicaoApiUrl = environment.getProperty("expedicao.api.url");

        if (estoqueApiUrl == null || expedicaoApiUrl == null) {
            throw new IllegalStateException("URLs de estoque ou expedição não foram carregadas corretamente.");
        }

        System.out.println("✔ Estoque URL: " + estoqueApiUrl);
        System.out.println("✔ Expedição URL: " + expedicaoApiUrl);
    }

    public String getEstoqueApiUrl() {
        return estoqueApiUrl;
    }

    public String getExpedicaoApiUrl() {
        return expedicaoApiUrl;
    }
}



