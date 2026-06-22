package com.tecdes.appsabancada.dto;

import lombok.Data;

@Data
public class EstoqueRequestDTO {

    private Integer cor;

    private Integer quantidade;

    private Integer posicao;
}