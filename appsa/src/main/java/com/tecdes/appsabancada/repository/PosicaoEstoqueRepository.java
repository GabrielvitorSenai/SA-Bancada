package com.tecdes.appsabancada.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tecdes.appsabancada.entity.PosicaoEstoque;

public interface PosicaoEstoqueRepository extends JpaRepository<PosicaoEstoque, Long> {

    Optional<PosicaoEstoque> findByPosicao(Integer posicao);
}