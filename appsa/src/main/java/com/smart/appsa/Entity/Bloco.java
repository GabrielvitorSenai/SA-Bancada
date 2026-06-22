package com.tecdes.appsabancada.entity;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bloco")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bloco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer andar;

    private Integer corBloco;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    @ManyToOne
    @JoinColumn(name = "estoque_id")
    private Estoque estoque;

    @OneToMany(mappedBy = "bloco", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Lamina> laminas = new ArrayList<>();

    public Long getIdBloco() { return id; }
    public void setIdBloco(Long idBloco) { this.id = idBloco; }
}
