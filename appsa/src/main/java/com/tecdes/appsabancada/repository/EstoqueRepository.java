package com.tecdes.appsabancada.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tecdes.appsabancada.entity.Estoque;
import com.tecdes.appsabancada.entity.PosicaoEstoque;

public interface EstoqueRepository extends JpaRepository<Estoque, Long> {

    Optional<Estoque> findByCor(Integer cor);

    List<Estoque> findByCorNot(Integer cor);

    Optional<Estoque> findByPosicaoEstoque(PosicaoEstoque posicaoEstoque);

    boolean existsByPosicaoEstoque(PosicaoEstoque posicaoEstoque);

    // ===== Camada de leitura dos CLPs (MonitorService / EstoqueClpService) =====
    // Lista os itens de uma cor ordenados pela posição física (PosicaoEstoque.posicao, 1..28).
    // O underscore força a navegação para o campo aninhado da entidade relacionada.
    List<Estoque> findByCorOrderByPosicaoEstoque_PosicaoAsc(Integer cor);
}
