package com.smart.appsa.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private Long idLamina;

    private Integer posicao;

    private Integer corLamina;

    private Integer padraoLamina;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "bloco_id")
    private Bloco bloco;
}