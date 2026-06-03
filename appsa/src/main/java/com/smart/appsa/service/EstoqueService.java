package com.smart.appsa.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.smart.appsa.Entity.Estoque;
import com.smart.appsa.Entity.PosicaoEstoque;
import com.smart.appsa.Exception.BusinessException;
import com.smart.appsa.dto.PosicaoEstoqueDTO;
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

    /**
     * Monta o Mapa Visual do Estoque (Etapa 3): retorna sempre as 28 posições
     * físicas, indicando a cor do bloco armazenado em cada uma (0 = vazia).
     */
    public List<PosicaoEstoqueDTO> mapaEstoque() {

        Map<Integer, Estoque> itensPorPosicao = new HashMap<>();
        for (Estoque estoque : estoqueRepository.findAll()) {
            PosicaoEstoque pos = estoque.getPosicaoEstoque();
            if (pos != null && pos.getPosicao() != null) {
                itensPorPosicao.put(pos.getPosicao(), estoque);
            }
        }

        List<PosicaoEstoqueDTO> mapa = new ArrayList<>();
        for (PosicaoEstoque posicao : posicaoRepository.findAll()) {

            Estoque item = itensPorPosicao.get(posicao.getPosicao());

            mapa.add(PosicaoEstoqueDTO.builder()
                    .posicao(posicao.getPosicao())
                    .disponivel(posicao.getDisponivel())
                    .idItem(item != null ? item.getId() : null)
                    .cor(item != null && item.getCor() != null ? item.getCor() : 0)
                    .build());
        }

        mapa.sort(Comparator.comparing(PosicaoEstoqueDTO::getPosicao));
        return mapa;
    }
}
