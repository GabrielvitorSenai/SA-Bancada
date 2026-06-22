package com.tecdes.appsabancada.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tipo;
    private int tampa;
    private int orderProduction;
    private String statusOrderProduction;

    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp timeStamp;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Bloco> blocos;

    

    public Pedido() {
        this.blocos = new ArrayList<>();
    }

    // Métodos de acesso
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }
    
    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public int getTampa() {
        return tampa;
    }

    public void setTampa(int tampa) {
        this.tampa = tampa;
    }
    
    public int getOrderProduction() {
        return orderProduction;
    }

    public void setOrderProduction(int orderProduction) {
        this.orderProduction = orderProduction;
    }

    public String getStatusOrderProduction() {
        return statusOrderProduction;
    }

    public void setStatusOrderProduction(String statusOrderProduction) {
        this.statusOrderProduction = statusOrderProduction;
    }

    public Timestamp getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Timestamp timeStamp) {
        this.timeStamp = timeStamp;
    }

    public List<Bloco> getBlocos() {
        return blocos;
    }

    public void setBlocos(List<Bloco> blocos) {
        this.blocos = blocos;
        if (blocos != null) {
            for (Bloco bloco : blocos) {
                bloco.setPedido(this);
            }
        }
    }
}

