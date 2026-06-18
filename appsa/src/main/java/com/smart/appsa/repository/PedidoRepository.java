package com.smart.appsa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.smart.appsa.Entity.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Optional<Pedido> findByNumeroPedido(Integer numeroPedido);

    @Query("SELECT COALESCE(MAX(p.numeroPedido), 0) FROM Pedido p")
    Integer findMaxNumeroPedido();
}
