package com.smart.appsa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smart.appsa.Entity.Estoque;
import com.smart.appsa.Entity.PosicaoEstoque;

public interface EstoqueRepository extends JpaRepository<Estoque, Long> {

    Optional<Estoque> findByCor(Integer cor);

    List<Estoque> findByCorNot(Integer cor);

    Optional<Estoque> findByPosicaoEstoque(PosicaoEstoque posicaoEstoque);

    boolean existsByPosicaoEstoque(PosicaoEstoque posicaoEstoque);
}