package com.tecdes.appsabancada.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tecdes.appsabancada.model.Estoque;

public interface EstoqueRepository extends JpaRepository<Estoque, Long> {
    Optional<Estoque> findByPosicaoEstoque(int posicaoEstoque);
    Optional<Estoque> findFirstByCorOrderByPosicaoEstoqueAsc(int cor);
    List<Estoque> findByCorOrderByPosicaoEstoqueAsc(int cor);
    
}

