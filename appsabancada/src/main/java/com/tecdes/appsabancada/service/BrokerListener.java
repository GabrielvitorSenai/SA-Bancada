package com.tecdes.appsabancada.service;

import java.nio.charset.StandardCharsets;

import com.tecdes.appsabancada.controller.MqttSseController;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.messages.InterceptPublishMessage;
import io.netty.buffer.ByteBuf;

public class BrokerListener extends AbstractInterceptHandler {

    private static MqttSseController sseController;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public static void setSseController(MqttSseController controller) {
        sseController = controller;
    }

    @Override
    public String getID() {
        return "mqtt-broker-listener";
    }

    @Override
    public void onPublish(InterceptPublishMessage msg) {
        ByteBuf buf = msg.getPayload();
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        String payload = new String(bytes, StandardCharsets.UTF_8);

        String clientId = msg.getClientID();

        System.out.println("Mensagem recebida do cliente [" + clientId + "]: " + payload);

        try {
            // ✅ Correção: adicionar aspas ao redor de valores de texto simples
            // Exemplo: {"value":AOPER} -> {"value":"AOPER"}
            payload = payload.replaceAll("(\"value\":)([A-Za-z]+)", "$1\"$2\"");

            JsonNode rootNode = objectMapper.readTree(payload);
            if (rootNode.isArray() && rootNode.size() > 0) {
                JsonNode firstObj = rootNode.get(0);
                String variable = firstObj.get("variable").asText();
                String value = firstObj.get("value").asText();

                if (sseController != null) {
                    sseController.broadcastMessage(clientId, variable, value);
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao processar mensagem MQTT: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
