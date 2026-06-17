package com.smart.appsa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smart.appsa.Entity.Bloco;
import com.smart.appsa.Entity.Estoque;

public interface BlocoRepository extends JpaRepository<Bloco, Long> {

    // Usado ao remover um item de estoque: precisamos desvincular os blocos
    // que apontam para ele antes do delete, senão a FK (bloco.estoque_id) barra.
    List<Bloco> findByEstoque(Estoque estoque);
}
