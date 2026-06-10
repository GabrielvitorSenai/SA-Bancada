package com.smart.appsa.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smart.appsa.Entity.Bloco;
import com.smart.appsa.Entity.Estoque;
import com.smart.appsa.Entity.Expedicao;
import com.smart.appsa.Entity.Pedido;
import com.smart.appsa.Exception.BusinessException;
import com.smart.appsa.repository.EstoqueRepository;
import com.smart.appsa.repository.ExpedicaoRepository;
import com.smart.appsa.repository.PedidoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final EstoqueRepository estoqueRepository;
    private final ExpedicaoRepository expedicaoRepository;
    private final SmartService smartService;

    @Transactional
    public Pedido criarPedido(Pedido pedido) {
        validarTipoPedido(pedido);
        validarLaminas(pedido);
        validarEstoque(pedido);   // ← já vincula bloco.estoque antes de salvar

        pedido.getBlocos().forEach(bloco -> {
            bloco.setPedido(pedido);
            bloco.getLaminas().forEach(lamina -> lamina.setBloco(bloco));
        });

        return pedidoRepository.save(pedido);
    }

    private void validarTipoPedido(Pedido pedido) {
        if (pedido.getTipoPedido() == 3 && pedido.getBlocos().size() != 3) {
            throw new BusinessException("Pedidos triplos exigem exatamente 3 blocos.");
        }
        if (pedido.getTipoPedido() == 2 && pedido.getBlocos().size() != 2) {
            throw new BusinessException("Pedidos duplos exigem exatamente 2 blocos.");
        }
        if (pedido.getTipoPedido() == 1 && pedido.getBlocos().size() != 1) {
            throw new BusinessException("Pedidos simples exigem exatamente 1 bloco.");
        }
    }

    private void validarLaminas(Pedido pedido) {
        for (Bloco bloco : pedido.getBlocos()) {
            if (bloco.getLaminas().size() > 3) {
                throw new BusinessException("Cada bloco pode possuir no máximo 3 lâminas.");
            }
        }
    }

    /**
     * Valida disponibilidade, decrementa a quantidade e vincula o item de estoque
     * ao bloco via {@code bloco.setEstoque()}.
     *
     * Esse vínculo é indispensável para que o SmartService consiga ler a posição
     * física (Posicao_Estoque_Andar_X) e gravá-la corretamente no DB9 do CLP.
     * Sem ele, o braço robótico recebe posição 0 e não sabe onde buscar o bloco.
     */
    private void validarEstoque(Pedido pedido) {
        for (Bloco bloco : pedido.getBlocos()) {
            Estoque estoque = estoqueRepository
                    .findByCor(bloco.getCorBloco())
                    .orElseThrow(() -> new BusinessException(
                            "Não existe estoque para a cor do bloco."));

            if (estoque.getQuantidade() <= 0) {
                throw new BusinessException(
                        "Quantidade insuficiente em estoque para a cor: " + bloco.getCorBloco());
            }

            estoque.setQuantidade(estoque.getQuantidade() - 1);
            estoqueRepository.save(estoque);

            // FIX ① — vincula o estoque ao bloco para que o SmartService
            //           possa navegar até PosicaoEstoque.posicao sem NPE.
            bloco.setEstoque(estoque);
        }
    }

    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    /**
     * Envia o pedido para a fila de produção (status 1 → 2).
     *
     * FIX ② — @Transactional mantém a sessão JPA aberta durante toda a chamada,
     * incluindo a execução do SmartService. Isso permite o carregamento lazy de
     * Bloco.laminas, Bloco.estoque e Estoque.posicaoEstoque sem
     * LazyInitializationException.
     *
     * FIX ③ — guarda status >= 2 impede reenvio de pedido já em produção.
     */
    @Transactional
    public Pedido enviarParaProducao(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Pedido não encontrado."));

        if (pedido.getStatus() != null && pedido.getStatus() >= 2) {
            throw new BusinessException("Pedido já em produção ou concluído.");
        }

        // Comunica com o CLP (pode lançar BusinessException se offline)
        smartService.enviarParaProducao(pedido);

        pedido.setStatus(2);
        return pedidoRepository.save(pedido);
    }

    /**
     * Conclui o pedido (status → 3) e gera o registro na Expedição.
     */
    @Transactional
    public Pedido atualizarStatus(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Pedido não encontrado."));

        if (pedido.getStatus() != null && pedido.getStatus() == 3) {
            throw new BusinessException("Pedido já concluído.");
        }

        pedido.setStatus(3);
        Pedido pedidoAtualizado = pedidoRepository.save(pedido);

        Expedicao expedicao = Expedicao.builder()
                .pedidoId(pedido.getIdPedido())
                .dataSaida(LocalDateTime.now())
                .posicaoExpedicao(pedido.getPosicaoExpedicao())
                .build();

        expedicaoRepository.save(expedicao);

        return pedidoAtualizado;
    }
}