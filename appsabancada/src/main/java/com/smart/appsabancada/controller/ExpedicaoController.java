package com.tecdes.appsabancada.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.tecdes.appsabancada.entity.Expedicao;
import com.tecdes.appsabancada.service.ExpedicaoClpService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/expedicao")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ExpedicaoController {

    private final ExpedicaoClpService expedicaoClpService;

    @GetMapping
    public ResponseEntity<List<Expedicao>> listar() {
        return ResponseEntity.ok(expedicaoClpService.listarExpedicao());
    }

    @PostMapping
    public ResponseEntity<Expedicao> ocupar(@RequestBody Expedicao expedicao) {
        return ResponseEntity.ok(expedicaoClpService.salvarExpedicao(expedicao));
    }

    @DeleteMapping("/{posicao}")
    public ResponseEntity<Void> limparPosicaoAlias(@PathVariable Integer posicao) {
        expedicaoClpService.limparPosicaoExpedicao(posicao);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{posicao}/limpar")
    public ResponseEntity<Void> limparPosicaoPut(@PathVariable Integer posicao) {
        expedicaoClpService.limparPosicaoExpedicao(posicao);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/posicao/{posicao}")
    public ResponseEntity<Void> limparPosicao(@PathVariable Integer posicao) {
        expedicaoClpService.limparPosicaoExpedicao(posicao);
        return ResponseEntity.noContent().build();
    }
}
