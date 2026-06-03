package com.smart.appsa.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProducaoViewController {

    @GetMapping("/")
    public String home() {
        return "redirect:/producao";
    }

    @GetMapping("/producao")
    public String formularioProducao(Model model) {
        model.addAttribute("tituloPagina", "Produção de Pedidos");
        return "producao";
    }
}
