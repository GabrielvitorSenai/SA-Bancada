package com.tecdes.appsabancada.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String home() {
        return "connection";
    }

    @GetMapping("/conexao")
    public String conexao() {
        return "connection";
    }

    @GetMapping("/estoque")
    public String estoque() {
        return "estoque";
    }

    @GetMapping("/pedidos/novo")
    public String pedidoNovo() {
        return "pedido-novo";
    }

    @GetMapping("/ordens")
    public String ordens() {
        return "ordens";
    }

    @GetMapping("/expedicao")
    public String expedicao() {
        return "expedicao";
    }

    @GetMapping("/monitoramento")
    public String monitoramento() {
        return "monitoramento";
    }
}