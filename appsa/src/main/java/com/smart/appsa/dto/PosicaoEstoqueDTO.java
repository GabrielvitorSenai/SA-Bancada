package com.tecdes.appsabancada.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Representa uma das 28 posições físicas do estoque para o Mapa Visual da Etapa 3.
 * cor = 0 (vazio), 1 (preto), 2 (vermelho) ou 3 (azul).
 * idItem = id do item de estoque ocupando a posição (null se vazia).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosicaoEstoqueDTO {

    private Long idItem;

    private Integer posicao;

    private Integer cor;

    private Boolean disponivel;
}
