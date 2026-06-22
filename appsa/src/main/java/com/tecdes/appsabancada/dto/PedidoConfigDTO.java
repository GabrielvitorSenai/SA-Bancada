package com.tecdes.appsabancada.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class PedidoConfigDTO {

    @JsonProperty("Id_Pedido")
    private Long idPedido;
    @JsonProperty("Tipo_Pedido")
    private int tipoPedido;
    @JsonProperty("Tampa_Pedido")
    private int tampaPedido;
    @JsonProperty("Ip_CLP")
    private String ipClp;
}
