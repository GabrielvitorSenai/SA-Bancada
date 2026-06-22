package com.tecdes.appsabancada.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.tecdes.appsabancada.entity.Pedido;
import com.tecdes.appsabancada.service.PedidoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PedidoController {

    private final PedidoService pedidoService;

    @GetMapping
    public ResponseEntity<List<Pedido>> listarTodos() {
        return ResponseEntity.ok(pedidoService.listarPedidos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pedido> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<Pedido> criarPedido(@RequestBody Pedido pedido) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pedidoService.criarPedido(pedido));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removerPedido(@PathVariable Long id) {
        pedidoService.removerPedido(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/produzir")
    public ResponseEntity<Pedido> enviarParaProducao(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.enviarParaProducao(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Pedido> atualizarStatus(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.atualizarStatus(id));
    }

    @PutMapping("/{id}/finalizar")
    public ResponseEntity<Pedido> finalizar(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.finalizarPedido(id));
    }
}
