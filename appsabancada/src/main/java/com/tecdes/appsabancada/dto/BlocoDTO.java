package com.tecdes.appsabancada.dto;

import java.util.List;

public class BlocoDTO {

    private int andar;
    private int posicaoEstoque;
    private int corBloco;
    private List<LaminaDTO> laminas;

    // Métodos de acesso
    public int getAndar() {
        return andar;
    }

    public void setAndar(int andar) {
        this.andar = andar;
    }

    public int getPosicaoEstoque() {
        return posicaoEstoque;
    }

    public void setPosicaoEstoque(int posicaoEstoque) {
        this.posicaoEstoque = posicaoEstoque;
    }

    public int getCorBloco() {
        return corBloco;
    }

    public void setCorBloco(int corBloco) {
        this.corBloco = corBloco;
    }

    public List<LaminaDTO> getLaminas() {
        return laminas;
    }

    public void setLaminas(List<LaminaDTO> laminas) {
        this.laminas = laminas;
    }
}

