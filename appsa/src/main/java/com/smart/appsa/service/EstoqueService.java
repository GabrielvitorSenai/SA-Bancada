package com.smart.appsa.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smart.appsa.Entity.Bloco;
import com.smart.appsa.Entity.Estoque;
import com.smart.appsa.Entity.PosicaoEstoque;
import com.smart.appsa.Exception.BusinessException;
import com.smart.appsa.dto.EstoqueRequestDTO;
import com.smart.appsa.dto.PosicaoEstoqueDTO;
import com.smart.appsa.repository.BlocoRepository;
import com.smart.appsa.repository.EstoqueRepository;
import com.smart.appsa.repository.PosicaoEstoqueRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EstoqueService {

    private final EstoqueRepository estoqueRepository;
    private final PosicaoEstoqueRepository posicaoRepository;
    private final BlocoRepository blocoRepository;

    public List<Estoque> listarTodos() {
        return estoqueRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(e -> e.getPosicaoEstoque().getPosicao()))
                .toList();
    }

    public List<Estoque> listarDisponiveis() {
        return estoqueRepository.findByCorNot(0);
    }

    @Transactional
    public Estoque adicionarAoEstoque(EstoqueRequestDTO dto) {
        validarDto(dto);

        PosicaoEstoque posicao = posicaoRepository
                .findByPosicao(dto.getPosicao())
                .orElseThrow(() -> new BusinessException("Posição de estoque não existe."));

        if (Boolean.FALSE.equals(posicao.getDisponivel())) {
            throw new BusinessException("Posição de estoque já está ocupada.");
        }

        if (estoqueRepository.existsByPosicaoEstoque(posicao)) {
            throw new BusinessException("Já existe item cadastrado nessa posição.");
        }

        Estoque estoque = Estoque.builder()
                .cor(dto.getCor())
                .quantidade(dto.getQuantidade())
                .posicaoEstoque(posicao)
                .build();

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

        return estoqueRepository.save(estoque);
    }

    @Transactional
    public void removerEstoque(Long id) {
        Estoque estoque = estoqueRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Item de estoque não encontrado."));

        PosicaoEstoque posicao = estoque.getPosicaoEstoque();

        desvincularBlocos(estoque);   // FIX: evita violação de FK (bloco.estoque_id)
        estoqueRepository.delete(estoque);

        if (posicao != null) {
            posicao.setDisponivel(true);
            posicaoRepository.save(posicao);
        }
    }

    @Transactional
    public void removerPorPosicao(Integer numeroPosicao) {
        PosicaoEstoque posicao = posicaoRepository
                .findByPosicao(numeroPosicao)
                .orElseThrow(() -> new BusinessException("Posição de estoque não existe."));

        Optional<Estoque> estoqueOpt = estoqueRepository.findByPosicaoEstoque(posicao);

        if (estoqueOpt.isEmpty()) {
            throw new BusinessException("Não existe item nessa posição.");
        }

        Estoque estoque = estoqueOpt.get();

        desvincularBlocos(estoque);   // FIX: evita violação de FK (bloco.estoque_id)
        estoqueRepository.delete(estoque);

        posicao.setDisponivel(true);
        posicaoRepository.save(posicao);
    }

    /**
     * Antes de remover um item de estoque, quebra o vínculo dos blocos que o
     * referenciam (bloco.estoque). Sem isso, o MySQL barra o delete por causa da
     * FK `bloco.estoque_id`.
     *
     * Os blocos (que pertencem a pedidos já existentes) são preservados — apenas
     * deixam de apontar para o estoque removido. A rastreabilidade da posição
     * física daquele bloco se perde, então só remova posições cujo estoque não
     * esteja mais em uso por um pedido ativo.
     */
    private void desvincularBlocos(Estoque estoque) {
        List<Bloco> blocos = blocoRepository.findByEstoque(estoque);
        if (!blocos.isEmpty()) {
            blocos.forEach(b -> b.setEstoque(null));
            blocoRepository.saveAll(blocos);
        }
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

            mapa.add(PosicaoEstoqueDTO.builder()
                    .posicao(posicao.getPosicao())
                    .disponivel(item == null)
                    .idItem(item != null ? item.getId() : null)
                    .cor(item != null && item.getCor() != null ? item.getCor() : 0)
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
        if (cor == null || cor < 1) {
            throw new BusinessException("Cor inválida.");
        }
    }

    private void validarQuantidade(Integer quantidade) {
        if (quantidade == null || quantidade < 1) {
            throw new BusinessException("Quantidade deve ser maior que zero.");
        }
    }
}
