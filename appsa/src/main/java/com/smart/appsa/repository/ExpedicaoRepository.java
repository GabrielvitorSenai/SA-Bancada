package com.smart.appsa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.smart.appsa.Entity.Expedicao;

public interface ExpedicaoRepository extends JpaRepository<Expedicao, Long> {

    // ===== Camada de leitura dos CLPs (MonitorService / ExpedicaoClpService) =====
    // Retorna as posições do magazine de expedição que já estão ocupadas (1..12).
    @Query("SELECT e.posicaoExpedicao FROM Expedicao e WHERE e.posicaoExpedicao IS NOT NULL")
    List<Integer> findAllPosicoesOcupadas();
}
