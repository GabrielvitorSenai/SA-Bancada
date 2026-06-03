package com.smart.appsa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smart.appsa.Entity.PosicaoEstoque;

public interface PosicaoEstoqueRepository extends JpaRepository<PosicaoEstoque, Long> {

    Optional<PosicaoEstoque> findByPosicao(Integer posicao);
}