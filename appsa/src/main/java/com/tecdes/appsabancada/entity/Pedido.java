package com.tecdes.appsabancada.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pedido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Integer numeroPedido;

    private Integer status;

    private Integer corTampa;

    private Integer tipoPedido;

    private LocalDateTime timeStamp;

    private Integer posicaoExpedicao;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Bloco> blocos = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (timeStamp == null) timeStamp = LocalDateTime.now();
    }

    public Long getIdPedido() { return id; }
    public void setIdPedido(Long idPedido) { this.id = idPedido; }
}
