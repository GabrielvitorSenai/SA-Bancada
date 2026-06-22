package com.tecdes.appsabancada.controller;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class MqttSseController {

    // Coleção segura para clientes SSE conectados
    private final Set<SseEmitter> clients = new CopyOnWriteArraySet<>();

    // Duração máxima da conexão SSE
    private static final long TIMEOUT = 30 * 60 * 1000L;

    /**
     * Endpoint para o cliente abrir conexão SSE.
     */
    @GetMapping(value = "/mqtt/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        clients.add(emitter);

        // Descarta a inscrição quando a conexão encerra
        emitter.onCompletion(() -> clients.remove(emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            clients.remove(emitter);
        });
        emitter.onError((e) -> clients.remove(emitter));

        return emitter;
    }

    /**
     * Método para enviar mensagem para todos os clientes conectados. A mensagem
     * deve estar no formato "clientId|variable:value"
     */
    public void broadcastMessage(String clientId, String variable, String value) {
        String message = clientId + "|" + variable + ":" + value;
        for (SseEmitter client : clients) {
            try {
                client.send(SseEmitter.event()
                        .name("mqtt-data")
                        .data(message));
            } catch (IOException e) {
                clients.remove(client);
            }
        }
    }
}
