package com.smart.appsa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.smart.appsa.Entity.Expedicao;

public interface ExpedicaoRepository extends JpaRepository<Expedicao, Long> {

    @Query("""
           SELECT e.posicaoExpedicao
           FROM Expedicao e
           WHERE e.posicaoExpedicao IS NOT NULL
           """)
    List<Integer> findAllPosicoesOcupadas();

    Optional<Expedicao> findByPosicaoExpedicao(Integer posicaoExpedicao);
}