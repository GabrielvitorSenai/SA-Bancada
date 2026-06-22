package com.tecdes.appsabancada.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.tecdes.appsabancada.entity.Expedicao;

public interface ExpedicaoRepository extends JpaRepository<Expedicao, Long> {

    @Query("""
           SELECT e.posicaoExpedicao
           FROM Expedicao e
           WHERE e.posicaoExpedicao IS NOT NULL
           """)
    List<Integer> findAllPosicoesOcupadas();

    Optional<Expedicao> findByPosicaoExpedicao(Integer posicaoExpedicao);

    Optional<Expedicao> findByPedidoId(Long pedidoId);
}
