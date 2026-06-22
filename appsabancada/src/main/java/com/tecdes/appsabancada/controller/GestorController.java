package com.tecdes.appsabancada.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tecdes.appsabancada.model.Estoque;
import com.tecdes.appsabancada.model.Expedicao;
import com.tecdes.appsabancada.repository.EstoqueRepository;
import com.tecdes.appsabancada.repository.ExpedicaoRepository;

@RestController
public class GestorController {

    @Autowired
    private EstoqueRepository estoqueRepository;

    @Autowired
    private ExpedicaoRepository expedicaoRepository;

    @GetMapping("/blocos-estoque")
    public Map<String, Integer> getValores() {
        List<Estoque> lista = estoqueRepository.findAll(Sort.by("posicaoEstoque")); // ordenado por posição
        Map<String, Integer> valores = new LinkedHashMap<>();

        for (Estoque est : lista) {
            String chave = "P" + est.getPosicaoEstoque(); // exemplo: posicao_1, posicao_2...
            valores.put(chave, est.getCor());
        }

        return valores;
    }
    
    @GetMapping("/pedidos-expedicao")
    public Map<String, Integer> carregarValoresExpedicao() {
        List<Expedicao> lista = expedicaoRepository.findAll(Sort.by(Sort.Direction.ASC, "posicaoExpedicao"));

        Map<String, Integer> valores = new LinkedHashMap<>();

        for (Expedicao exp : lista) {
            String chave = "P" + exp.getPosicaoExpedicao(); // Ex: posicao_1, posicao_2...
            valores.put(chave, exp.getOrderNumber());
        }

        return valores;
    }
}
