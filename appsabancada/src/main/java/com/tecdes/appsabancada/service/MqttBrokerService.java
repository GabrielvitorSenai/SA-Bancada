package com.tecdes.appsabancada.service;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.springframework.stereotype.Service;

import com.tecdes.appsabancada.controller.MqttSseController;

import io.moquette.broker.Server;
import io.moquette.broker.config.IConfig;
import io.moquette.broker.config.MemoryConfig;

@Service
public class MqttBrokerService {

    private Server mqttBroker;
    private BrokerListener brokerListener;

    private final MqttSseController mqttSseController;

    public MqttBrokerService(MqttSseController mqttSseController) {
        this.mqttSseController = mqttSseController;
    }

    public void startBroker() throws IOException {
        if (mqttBroker != null) {
            System.out.println("Broker MQTT já está rodando.");
            return;
        }

        Properties configProps = new Properties();
        configProps.setProperty("port", "1883");
        configProps.setProperty("host", "0.0.0.0");

        IConfig memoryConfig = new MemoryConfig(configProps);

        mqttBroker = new Server();

        // Prepara o listener responsável por encaminhar eventos
        brokerListener = new BrokerListener();
        BrokerListener.setSseController(mqttSseController);

        // Ativa o broker com interceptação de mensagens
        mqttBroker.startServer(memoryConfig, List.of(brokerListener));

        System.out.println("MQTT Broker started.");
    }

    public void stopBroker() {
        if (mqttBroker != null) {
            mqttBroker.stopServer();
            mqttBroker = null;
            brokerListener = null;
            System.out.println("MQTT Broker stopped.");
        } else {
            System.out.println("MQTT Broker is not running.");
        }
    }
}
