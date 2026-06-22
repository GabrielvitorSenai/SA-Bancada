package com.tecdes.appsabancada.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.tecdes.appsabancada.model.Expedicao;

public interface ExpedicaoRepository extends JpaRepository<Expedicao, Long> {

    Optional<Expedicao> findByOrderNumber(int orderNumber);

    Optional<Expedicao> findByPosicaoExpedicao(int posicaoExpedicao);

    Optional<Expedicao> findFirstByOrderNumberEqualsOrderByPosicaoExpedicaoAsc(int orderNumber);

    @Query("SELECT e.posicaoExpedicao FROM Expedicao e")
    List<Integer> findAllPosicoesOcupadas();
}