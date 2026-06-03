package com.smart.appsa.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.smart.appsa.Entity.Estoque;
import com.smart.appsa.Entity.PosicaoEstoque;
import com.smart.appsa.Exception.BusinessException;
import com.smart.appsa.repository.EstoqueRepository;
import com.smart.appsa.repository.PosicaoEstoqueRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EstoqueService {

    private final EstoqueRepository estoqueRepository;
    private final PosicaoEstoqueRepository posicaoRepository;

    public Estoque adicionarAoEstoque(Estoque estoque) {

        PosicaoEstoque posicao = posicaoRepository
                .findByPosicao(estoque.getPosicaoEstoque().getPosicao())
                .orElseThrow(() -> new BusinessException(
                        "Posição de estoque não existe."));

        if (!posicao.getDisponivel()) {
            throw new BusinessException(
                    "Posição de estoque indisponível.");
        }

        estoque.setPosicaoEstoque(posicao);

        posicao.setDisponivel(false);

        return estoqueRepository.save(estoque);
    }

    public List<Estoque> listarDisponiveis() {
        return estoqueRepository.findByCorNot(0);
    }
}