package com.tecdes.appsabancada.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.tecdes.appsabancada.service.MqttBrokerService;

@Controller
public class MqttController {

    private final MqttBrokerService mqttBrokerService;
    private boolean brokerRunning = false;

    // Dependência recebida pelo construtor
    public MqttController(MqttBrokerService mqttBrokerService) {
        this.mqttBrokerService = mqttBrokerService;
    }

    @PostMapping("/mqtt/toggle")
    @ResponseBody
    public Map<String, Object> toggleBroker() {
        Map<String, Object> response = new HashMap<>();
        try {
            if (brokerRunning) {
                mqttBrokerService.stopBroker();
                brokerRunning = false;
            } else {
                mqttBrokerService.startBroker();
                brokerRunning = true;
            }
            response.put("success", true);
            response.put("brokerRunning", brokerRunning);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        return response;
    }
}

