package com.smart.appsa.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smart.appsa.Entity.Expedicao;

public interface ExpedicaoRepository extends JpaRepository<Expedicao, Long> {
}