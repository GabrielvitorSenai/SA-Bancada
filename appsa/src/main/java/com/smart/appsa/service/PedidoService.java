package com.smart.appsa.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smart.appsa.Entity.Bloco;
import com.smart.appsa.Entity.Estoque;
import com.smart.appsa.Entity.Expedicao;
import com.smart.appsa.Entity.Pedido;
import com.smart.appsa.Entity.PosicaoEstoque;
import com.smart.appsa.Exception.BusinessException;
import com.smart.appsa.repository.EstoqueRepository;
import com.smart.appsa.repository.ExpedicaoRepository;
import com.smart.appsa.repository.PedidoRepository;
import com.smart.appsa.repository.PosicaoEstoqueRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final EstoqueRepository estoqueRepository;
    private final PosicaoEstoqueRepository posicaoEstoqueRepository;
    private final ExpedicaoRepository expedicaoRepository;
    private final SmartService smartService;
    private final ExpedicaoClpService expedicaoClpService;

    @Transactional
    public Pedido criarPedido(Pedido pedido) {
        validarTipoPedido(pedido);
        validarLaminas(pedido);
        validarEstoque(pedido);

        pedido.getBlocos().forEach(bloco -> {
            bloco.setPedido(pedido);

            if (bloco.getLaminas() != null) {
                bloco.getLaminas().forEach(lamina -> lamina.setBloco(bloco));
            }
        });

        if (pedido.getStatus() == null) {
            pedido.setStatus(1);
        }

        return pedidoRepository.save(pedido);
    }

    private void validarTipoPedido(Pedido pedido) {
        if (pedido == null) {
            throw new BusinessException("Pedido não informado.");
        }

        if (pedido.getTipoPedido() == null) {
            throw new BusinessException("Tipo do pedido não informado.");
        }

        if (pedido.getBlocos() == null || pedido.getBlocos().isEmpty()) {
            throw new BusinessException("Pedido precisa possuir pelo menos um bloco.");
        }

        int quantidadeBlocos = pedido.getBlocos().size();

        if (pedido.getTipoPedido() == 3 && quantidadeBlocos != 3) {
            throw new BusinessException("Pedidos triplos exigem exatamente 3 blocos.");
        }

        if (pedido.getTipoPedido() == 2 && quantidadeBlocos != 2) {
            throw new BusinessException("Pedidos duplos exigem exatamente 2 blocos.");
        }

        if (pedido.getTipoPedido() == 1 && quantidadeBlocos != 1) {
            throw new BusinessException("Pedidos simples exigem exatamente 1 bloco.");
        }

        if (pedido.getTipoPedido() < 1 || pedido.getTipoPedido() > 3) {
            throw new BusinessException("Tipo de pedido inválido. Use 1, 2 ou 3.");
        }
    }

    private void validarLaminas(Pedido pedido) {
        for (Bloco bloco : pedido.getBlocos()) {
            if (bloco.getLaminas() == null) {
                throw new BusinessException("Cada bloco precisa possuir a lista de lâminas.");
            }

            if (bloco.getLaminas().size() > 3) {
                throw new BusinessException("Cada bloco pode possuir no máximo 3 lâminas.");
            }
        }
    }

    private void validarEstoque(Pedido pedido) {
        Set<Integer> posicoesUsadasNestePedido = new HashSet<>();

        List<Bloco> blocosOrdenados = pedido.getBlocos()
                .stream()
                .sorted(Comparator.comparing(
                        Bloco::getAndar,
                        Comparator.nullsLast(Integer::compareTo)
                ))
                .toList();

        for (Bloco bloco : blocosOrdenados) {
            Integer corSolicitada = bloco.getCorBloco();

            if (corSolicitada == null || corSolicitada < 1 || corSolicitada > 3) {
                throw new BusinessException("Cor de bloco inválida. Use 1=preto, 2=vermelho ou 3=azul.");
            }

            Estoque estoqueSelecionado = estoqueRepository
                    .findByCorOrderByPosicaoEstoque_PosicaoAsc(corSolicitada)
                    .stream()
                    .filter(e -> e.getPosicaoEstoque() != null)
                    .filter(e -> e.getPosicaoEstoque().getPosicao() != null)
                    .filter(e -> e.getQuantidade() != null && e.getQuantidade() > 0)
                    .filter(e -> !posicoesUsadasNestePedido.contains(e.getPosicaoEstoque().getPosicao()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(
                            "Não existe posição disponível no estoque para a cor: " + corSolicitada
                    ));

            PosicaoEstoque posicao = estoqueSelecionado.getPosicaoEstoque();
            Integer posicaoFisica = posicao.getPosicao();

            posicoesUsadasNestePedido.add(posicaoFisica);

            estoqueSelecionado.setQuantidade(0);
            estoqueSelecionado.setCor(0);
            estoqueRepository.save(estoqueSelecionado);

            posicao.setDisponivel(true);
            posicaoEstoqueRepository.save(posicao);

            bloco.setEstoque(estoqueSelecionado);

            System.out.printf(
                    "[ESTOQUE] Bloco cor %d vinculado à posição física %d%n",
                    corSolicitada,
                    posicaoFisica
            );
        }
    }

    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    @Transactional
    public Pedido enviarParaProducao(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Pedido não encontrado."));

        if (pedido.getStatus() != null && pedido.getStatus() >= 2) {
            throw new BusinessException("Pedido já em produção ou concluído.");
        }

        smartService.enviarParaProducao(pedido);

        pedido.setStatus(2);
        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido atualizarStatus(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Pedido não encontrado."));

        if (pedido.getStatus() != null && pedido.getStatus() == 3) {
            throw new BusinessException("Pedido já concluído.");
        }

        expedicaoClpService.gravarPedidoFinalizadoNaMemoria(pedido);

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
