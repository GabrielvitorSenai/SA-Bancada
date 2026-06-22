package com.tecdes.appsabancada.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tecdes.appsabancada.entity.Estoque;
import com.tecdes.appsabancada.entity.PosicaoEstoque;
import com.tecdes.appsabancada.exception.BusinessException;
import com.tecdes.appsabancada.dto.EstoqueRequestDTO;
import com.tecdes.appsabancada.dto.PosicaoEstoqueDTO;
import com.tecdes.appsabancada.repository.EstoqueRepository;
import com.tecdes.appsabancada.repository.PosicaoEstoqueRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EstoqueService {

    private final EstoqueRepository estoqueRepository;
    private final PosicaoEstoqueRepository posicaoRepository;

    public List<Estoque> listarTodos() {
        return estoqueRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(e -> {
                    if (e.getPosicaoEstoque() == null || e.getPosicaoEstoque().getPosicao() == null) {
                        return Integer.MAX_VALUE;
                    }
                    return e.getPosicaoEstoque().getPosicao();
                }))
                .toList();
    }

    public List<Estoque> listarDisponiveis() {
        return estoqueRepository.findByCorNot(0)
                .stream()
                .filter(e -> e.getQuantidade() != null && e.getQuantidade() > 0)
                .toList();
    }

    @Transactional
    public Estoque adicionarAoEstoque(EstoqueRequestDTO dto) {
        validarDto(dto);

        PosicaoEstoque posicao = posicaoRepository
                .findByPosicao(dto.getPosicao())
                .orElseThrow(() -> new BusinessException("Posição de estoque não existe."));

        Estoque estoque = estoqueRepository
                .findByPosicaoEstoque(posicao)
                .orElseGet(Estoque::new);

        boolean ocupado = estoque.getId() != null
                && estoque.getCor() != null
                && estoque.getCor() > 0
                && estoque.getQuantidade() != null
                && estoque.getQuantidade() > 0;

        if (ocupado) {
            throw new BusinessException("Posição de estoque já está ocupada.");
        }

        estoque.setCor(dto.getCor());
        estoque.setQuantidade(dto.getQuantidade());
        estoque.setPosicaoEstoque(posicao);

        posicao.setDisponivel(false);
        posicaoRepository.save(posicao);

        return estoqueRepository.save(estoque);
    }

    @Transactional
    public Estoque atualizarEstoque(Long id, EstoqueRequestDTO dto) {
        Estoque estoque = estoqueRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Item de estoque não encontrado."));

        if (dto.getCor() != null) {
            validarCor(dto.getCor());
            estoque.setCor(dto.getCor());
        }

        if (dto.getQuantidade() != null) {
            validarQuantidade(dto.getQuantidade());
            estoque.setQuantidade(dto.getQuantidade());
        }

        atualizarDisponibilidadePosicao(estoque);

        return estoqueRepository.save(estoque);
    }

    @Transactional
    public void removerEstoque(Long id) {
        Estoque estoque = estoqueRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Item de estoque não encontrado."));

        limparItemEstoque(estoque);
    }

    @Transactional
    public void removerPorPosicao(Integer numeroPosicao) {
        PosicaoEstoque posicao = posicaoRepository
                .findByPosicao(numeroPosicao)
                .orElseThrow(() -> new BusinessException("Posição de estoque não existe."));

        Estoque estoque = estoqueRepository
                .findByPosicaoEstoque(posicao)
                .orElseThrow(() -> new BusinessException("Não existe item nessa posição."));

        limparItemEstoque(estoque);
    }

    private void limparItemEstoque(Estoque estoque) {
        estoque.setCor(0);
        estoque.setQuantidade(0);
        estoqueRepository.save(estoque);

        atualizarDisponibilidadePosicao(estoque);
    }

    private void atualizarDisponibilidadePosicao(Estoque estoque) {
        PosicaoEstoque posicao = estoque.getPosicaoEstoque();

        if (posicao == null) {
            return;
        }

        boolean ocupado = estoque.getCor() != null
                && estoque.getCor() > 0
                && estoque.getQuantidade() != null
                && estoque.getQuantidade() > 0;

        posicao.setDisponivel(!ocupado);
        posicaoRepository.save(posicao);
    }

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

            boolean ocupado = item != null
                    && item.getCor() != null
                    && item.getCor() > 0
                    && item.getQuantidade() != null
                    && item.getQuantidade() > 0;

            mapa.add(PosicaoEstoqueDTO.builder()
                    .posicao(posicao.getPosicao())
                    .disponivel(!ocupado)
                    .idItem(ocupado ? item.getId() : null)
                    .cor(ocupado ? item.getCor() : 0)
                    .build());
        }

        mapa.sort(Comparator.comparing(PosicaoEstoqueDTO::getPosicao));

        return mapa;
    }

    private void validarDto(EstoqueRequestDTO dto) {
        if (dto == null) {
            throw new BusinessException("Dados do estoque não informados.");
        }

        validarCor(dto.getCor());
        validarQuantidade(dto.getQuantidade());

        if (dto.getPosicao() == null || dto.getPosicao() < 1 || dto.getPosicao() > 28) {
            throw new BusinessException("Posição inválida. Informe uma posição entre 1 e 28.");
        }
    }

    private void validarCor(Integer cor) {
        if (cor == null || cor < 1 || cor > 3) {
            throw new BusinessException("Cor inválida. Use 1=preto, 2=vermelho ou 3=azul.");
        }
    }

    private void validarQuantidade(Integer quantidade) {
        if (quantidade == null || quantidade < 1) {
            throw new BusinessException("Quantidade deve ser maior que zero.");
        }
    }
}
