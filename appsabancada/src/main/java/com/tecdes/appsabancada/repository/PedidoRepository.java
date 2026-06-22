package com.tecdes.appsabancada.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.tecdes.appsabancada.model.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @Query("SELECT COALESCE(MAX(p.orderProduction), 0) FROM Pedido p")
    int findMaxOrderProduction();

    Optional<Pedido> findTopByTipoAndStatusOrderProductionOrderByTimeStampDesc(String tipo, String statusOrderProduction);
}
