package com.smart.appsa.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.smart.appsa.config.ApiUrlConfig;

@Service
public class ApiIntegrationService {

    @Autowired 
    private ApiUrlConfig apiUrlConfig;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean salvarEstoque(Map<String, Integer> dados) {
        return postJson(apiUrlConfig.getEstoqueApiUrl() + "/salvar", dados);
    }

    public boolean salvarExpedicao(Map<String, Integer> dados) {
        return postJson(apiUrlConfig.getExpedicaoApiUrl() + "/salvar", dados);
    }

    private boolean postJson(String url, Map<String, Integer> dados) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Integer>> request = new HttpEntity<>(dados, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("✅ Resposta da API [" + url + "]: " + response.getBody());
                return true;
            } else {
                System.out.println("⚠️ Falha da API [" + url + "], código HTTP: " + response.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            System.out.println("❌ Erro ao chamar API: " + url);
            e.printStackTrace();
            return false;
        }
    }
}

