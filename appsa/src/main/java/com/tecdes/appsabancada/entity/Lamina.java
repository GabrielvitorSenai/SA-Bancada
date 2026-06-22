package com.tecdes.appsabancada.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lamina")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lamina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer posicao;

    private Integer corLamina;

    private Integer padraoLamina;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "bloco_id")
    private Bloco bloco;

    public Long getIdLamina() { return id; }
    public void setIdLamina(Long idLamina) { this.id = idLamina; }
}
