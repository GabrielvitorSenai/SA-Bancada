package com.smart.appsa.dto;

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
public class LaminaDTO {

    private Integer posicao;

    private Integer corLamina;

    private Integer padraoLamina;
}
