package com.tecdes.appsabancada.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

public class ExternalFileEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "externalConfig";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String pathValue = environment.getProperty("db.config.path");

        if (pathValue == null || pathValue.isBlank()) {
            System.out.println(">> Nenhum arquivo externo de banco foi informado. Usando application.properties.");
            return;
        }

        Path configPath = Paths.get(pathValue).toAbsolutePath().normalize();
        System.out.println(">> Verificando arquivo externo: " + configPath);

        if (!Files.exists(configPath)) {
            System.out.println(">> Aviso: arquivo externo não encontrado. O sistema continuará usando as configurações do application.properties.");
            return;
        }

        try (FileInputStream fis = new FileInputStream(configPath.toFile())) {
            Properties props = new Properties();
            props.load(fis);
            environment.getPropertySources().addFirst(new PropertiesPropertySource(PROPERTY_SOURCE_NAME, props));
            System.out.println(">> Configurações externas carregadas com sucesso.");
        } catch (IOException e) {
            System.out.println(">> Aviso: não foi possível carregar o arquivo externo. O sistema continuará com as propriedades padrão.");
            System.out.println(">> Detalhe: " + e.getMessage());
        }
    }
}
