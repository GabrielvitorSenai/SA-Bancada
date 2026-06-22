package com.tecdes.appsabancada.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Expedicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int posicaoExpedicao;
    private int orderNumber;

    // Métodos de acesso

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public int getPosicaoExpedicao() {
        return posicaoExpedicao;
    }
    public void setPosicaoExpedicao(int posicaoExpedicao) {
        this.posicaoExpedicao = posicaoExpedicao;
    }
    public int getOrderNumber() {
        return orderNumber;
    }
    public void setOrderNumber(int orderNumber) {
        this.orderNumber = orderNumber;
    }
      
}
