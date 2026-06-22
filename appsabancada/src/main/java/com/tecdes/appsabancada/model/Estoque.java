package com.tecdes.appsabancada.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Estoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int posicaoEstoque;
    private int cor;

    // Métodos de acesso

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public int getPosicaoEstoque() {
        return posicaoEstoque;
    }
    public void setPosicaoEstoque(int posicaoEstoque) {
        this.posicaoEstoque = posicaoEstoque;
    }
    public int getCor() {
        return cor;
    }
    public void setCor(int cor) {
        this.cor = cor;
    }
       
}

