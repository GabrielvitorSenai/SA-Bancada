package com.smart.appsa.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smart.appsa.clpcomm.PlcConnectionService;
import com.smart.appsa.clpcomm.PlcConnector;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/clp")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ClpStatusController {

    private final PlcConnectionService plcConnectionService;

    @Value("${smart40.clp.ip}")
    private String ipClp;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        try {
            PlcConnector connector = plcConnectionService.getConnection(ipClp);

            boolean conectado = connector != null;

            return ResponseEntity.ok(Map.of(
                    "conectado", conectado,
                    "ip", ipClp,
                    "mensagem", conectado ? "Bancada conectada" : "Bancada offline",
                    "dataHora", LocalDateTime.now().toString()
            ));

        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "conectado", false,
                    "ip", ipClp,
                    "mensagem", "Erro ao conectar com a bancada: " + e.getMessage(),
                    "dataHora", LocalDateTime.now().toString()
            ));
        }
    }
}