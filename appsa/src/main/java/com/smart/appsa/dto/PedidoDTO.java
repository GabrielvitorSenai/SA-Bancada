package com.smart.appsa.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoDTO {

    private Integer tipoPedido;

    private Integer corTampa;

    private Integer numeroPedido;

    private Integer status;

    private Integer posicaoExpedicao;

    private List<BlocoDTO> blocos;
}