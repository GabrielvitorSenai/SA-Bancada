package com.smart.appsa.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smart.appsa.Entity.Estoque;
import com.smart.appsa.service.EstoqueService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/estoque")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EstoqueController {

    private final EstoqueService estoqueService;

    @GetMapping("/disponivel")
    public ResponseEntity<List<Estoque>> listarDisponiveis() {

        return ResponseEntity.ok(
                estoqueService.listarDisponiveis());
    }

    @PostMapping
    public ResponseEntity<Estoque> adicionarEstoque(
            @RequestBody Estoque estoque) {

        return ResponseEntity.ok(
                estoqueService.adicionarAoEstoque(estoque));
    }
}