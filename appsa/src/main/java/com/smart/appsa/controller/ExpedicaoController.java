package com.smart.appsa.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.smart.appsa.Entity.Expedicao;
import com.smart.appsa.service.ExpedicaoClpService;

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

    @DeleteMapping("/posicao/{posicao}")
    public ResponseEntity<Void> limparPosicao(@PathVariable Integer posicao) {
        expedicaoClpService.limparPosicaoExpedicao(posicao);
        return ResponseEntity.noContent().build();
    }
}
