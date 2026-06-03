package com.smart.appsa.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smart.appsa.Entity.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
}