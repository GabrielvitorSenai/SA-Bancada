package com.smart.appsa.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smart.appsa.Entity.Estoque;
import com.smart.appsa.dto.EstoqueRequestDTO;
import com.smart.appsa.dto.PosicaoEstoqueDTO;
import com.smart.appsa.service.EstoqueService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/estoque")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EstoqueController {

    private final EstoqueService estoqueService;

    @GetMapping
    public ResponseEntity<List<Estoque>> listarTodos() {
        return ResponseEntity.ok(estoqueService.listarTodos());
    }

    @GetMapping("/disponivel")
    public ResponseEntity<List<Estoque>> listarDisponiveis() {
        return ResponseEntity.ok(estoqueService.listarDisponiveis());
    }

    @GetMapping("/posicoes")
    public ResponseEntity<List<PosicaoEstoqueDTO>> mapaEstoque() {
        return ResponseEntity.ok(estoqueService.mapaEstoque());
    }

    @PostMapping
    public ResponseEntity<Estoque> adicionarEstoque(@RequestBody EstoqueRequestDTO dto) {
        return ResponseEntity.ok(estoqueService.adicionarAoEstoque(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Estoque> atualizarEstoque(
            @PathVariable Long id,
            @RequestBody EstoqueRequestDTO dto
    ) {
        return ResponseEntity.ok(estoqueService.atualizarEstoque(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removerEstoque(@PathVariable Long id) {
        estoqueService.removerEstoque(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/posicao/{posicao}")
    public ResponseEntity<Void> removerPorPosicao(@PathVariable Integer posicao) {
        estoqueService.removerPorPosicao(posicao);
        return ResponseEntity.noContent().build();
    }
}