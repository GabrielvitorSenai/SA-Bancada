package com.tecdes.appsabancada.dto;

import java.sql.Timestamp;
import java.util.List;

public class PedidoDTO {

    private Long id;
    private String tipo;
    private int tampa;

    private String ipClp;
    private String statusOrderProduction;
    private Timestamp timeStamp;
    private List<BlocoDTO> blocos;

    // Métodos de acesso
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public int getTampa() {
        return tampa;
    }

    public void setTampa(int tampa) {
        this.tampa = tampa;
    }
    
    public String getIpClp() {
        return ipClp;
    }

    public void setIpClp(String ipClp) {
        this.ipClp = ipClp;
    }

    public String getStatusOrderProduction() {
        return statusOrderProduction;
    }

    public void setStatusOrderProduction(String statusOrderProduction) {
        this.statusOrderProduction = statusOrderProduction;
    }

    public Timestamp getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Timestamp timeStamp) {
        this.timeStamp = timeStamp;
    }

    public List<BlocoDTO> getBlocos() {
        return blocos;
    }

    public void setBlocos(List<BlocoDTO> blocos) {
        this.blocos = blocos;
    }
}
