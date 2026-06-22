package com.tecdes.appsabancada.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * URLs base usadas pelo ApiIntegrationService (camada de leitura dos CLPs).
 *
 * No projeto antigo (com.tecdes.sistema_bancada) a persistência de estoque e
 * expedição era feita por uma API REST externa. No appsa a persistência é local
 * (JPA + MySQL), então estes valores só importam se você optar por manter o
 * ApiIntegrationService chamando uma API. Caso contrário, veja INTEGRACAO.md
 * (seção "Persistência") para trocar por gravação direta nos repositories.
 *
 * Defina em application.properties (opcional — há defaults):
 *   api.estoque-url=http://localhost:18089/api/estoque
 *   api.expedicao-url=http://localhost:18089/api/expedicao
 */
@Component
public class ApiUrlConfig {

    @Value("${api.estoque-url:http://localhost:18089/api/estoque}")
    private String estoqueApiUrl;

    @Value("${api.expedicao-url:http://localhost:18089/api/expedicao}")
    private String expedicaoApiUrl;

    public String getEstoqueApiUrl() {
        return estoqueApiUrl;
    }

    public String getExpedicaoApiUrl() {
        return expedicaoApiUrl;
    }
}
