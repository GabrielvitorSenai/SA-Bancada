package com.smart.appsa.Entity;

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
    private Long idBloco;

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
    private List<Lamina> laminas = new ArrayList<>();
}