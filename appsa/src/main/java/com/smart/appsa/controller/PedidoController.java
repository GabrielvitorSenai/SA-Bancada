package com.smart.appsa.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smart.appsa.Entity.Pedido;
import com.smart.appsa.service.PedidoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PedidoController {

    private final PedidoService pedidoService;

    @GetMapping
    public ResponseEntity<List<Pedido>> listarTodos() {

        return ResponseEntity.ok(
                pedidoService.listarPedidos());
    }

    @PostMapping
    public ResponseEntity<Pedido> criarPedido(
            @RequestBody Pedido pedido) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pedidoService.criarPedido(pedido));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Pedido> atualizarStatus(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                pedidoService.atualizarStatus(id));
    }
}