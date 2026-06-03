package com.smart.appsa.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smart.appsa.Entity.Expedicao;
import com.smart.appsa.repository.ExpedicaoRepository;

import lombok.RequiredArgsConstructor;

/**
 * Etapa 3 - Painel de Expedição.
 * Expõe os registros de expedição (pedidos concluídos aguardando retirada)
 * para que o dashboard monte o grid das 12 posições.
 */
@RestController
@RequestMapping("/api/expedicao")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ExpedicaoController {

    private final ExpedicaoRepository expedicaoRepository;

    @GetMapping
    public ResponseEntity<List<Expedicao>> listar() {
        return ResponseEntity.ok(expedicaoRepository.findAll());
    }
}
