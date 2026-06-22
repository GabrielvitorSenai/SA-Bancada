package com.tecdes.appsabancada.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class PedidoInfoDTO {
    
    // --- Andar 1 ---
    @JsonProperty("Cor_Andar_1")
    private int corAndar1;

    @JsonProperty("Posicao_Estoque_Andar_1")
    private int posicaoEstoqueAndar1;

    @JsonProperty("Cor_Lamina_1_Andar_1")
    private int corLamina1Andar1;

    @JsonProperty("Cor_Lamina_2_Andar_1")
    private int corLamina2Andar1;

    @JsonProperty("Cor_Lamina_3_Andar_1")
    private int corLamina3Andar1;

    @JsonProperty("Padrao_Lamina_1_Andar_1")
    private int padraoLamina1Andar1;

    @JsonProperty("Padrao_Lamina_2_Andar_1")
    private int padraoLamina2Andar1;

    @JsonProperty("Padrao_Lamina_3_Andar_1")
    private int padraoLamina3Andar1;

    @JsonProperty("Processamento_Andar_1")
    private int processamentoAndar1;

    // --- Andar 2 ---
    @JsonProperty("Cor_Andar_2")
    private int corAndar2;

    @JsonProperty("Posicao_Estoque_Andar_2")
    private int posicaoEstoqueAndar2;

    @JsonProperty("Cor_Lamina_1_Andar_2")
    private int corLamina1Andar2;

    @JsonProperty("Cor_Lamina_2_Andar_2")
    private int corLamina2Andar2;

    @JsonProperty("Cor_Lamina_3_Andar_2")
    private int corLamina3Andar2;

    @JsonProperty("Padrao_Lamina_1_Andar_2")
    private int padraoLamina1Andar2;

    @JsonProperty("Padrao_Lamina_2_Andar_2")
    private int padraoLamina2Andar2;

    @JsonProperty("Padrao_Lamina_3_Andar_2")
    private int padraoLamina3Andar2;

    @JsonProperty("Processamento_Andar_2")
    private int processamentoAndar2;

    // --- Andar 3 ---
    @JsonProperty("Cor_Andar_3")
    private int corAndar3;

    @JsonProperty("Posicao_Estoque_Andar_3")
    private int posicaoEstoqueAndar3;

    @JsonProperty("Cor_Lamina_1_Andar_3")
    private int corLamina1Andar3;

    @JsonProperty("Cor_Lamina_2_Andar_3")
    private int corLamina2Andar3;

    @JsonProperty("Cor_Lamina_3_Andar_3")
    private int corLamina3Andar3;

    @JsonProperty("Padrao_Lamina_1_Andar_3")
    private int padraoLamina1Andar3;

    @JsonProperty("Padrao_Lamina_2_Andar_3")
    private int padraoLamina2Andar3;

    @JsonProperty("Padrao_Lamina_3_Andar_3")
    private int padraoLamina3Andar3;

    @JsonProperty("Processamento_Andar_3")
    private int processamentoAndar3;

    // --- Dados Gerais do Pedido ---

    @JsonProperty("Numero_Pedido")
    private int numeroPedido;

    @JsonProperty("Andares")
    private int andares;

    @JsonProperty("Posicao_Expedicao")
    private int posicaoExpedicao;

}
