package com.smart.appsa.service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smart.appsa.Entity.Bloco;
import com.smart.appsa.Entity.Estoque;
import com.smart.appsa.Entity.Pedido;
import com.smart.appsa.Entity.PosicaoEstoque;
import com.smart.appsa.Exception.BusinessException;
import com.smart.appsa.repository.EstoqueRepository;
import com.smart.appsa.repository.PedidoRepository;
import com.smart.appsa.repository.PosicaoEstoqueRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final EstoqueRepository estoqueRepository;
    private final PosicaoEstoqueRepository posicaoEstoqueRepository;
    private final SmartService smartService;
    private final ExpedicaoClpService expedicaoClpService;

    @Transactional
    public Pedido criarPedido(Pedido pedido) {
        validarEstruturaPedido(pedido);

        gerarNumeroPedidoSeNecessario(pedido);

        /*
         * Reserva uma posição física para cada bloco.
         * A escolha é feita pela primeira posição do mapa de estoque que contém a cor.
         */
        selecionarEstoqueParaBlocos(pedido);

        vincularRelacionamentos(pedido);

        if (pedido.getStatus() == null) {
            pedido.setStatus(1); // 1 = pendente
        }

        return pedidoRepository.save(pedido);
    }

    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    public Pedido buscarPorId(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Pedido não encontrado."));
    }

    @Transactional
    public void removerPedido(Long id) {
        Pedido pedido = buscarPorId(id);

        if (pedido.getStatus() != null && pedido.getStatus() >= 2) {
            throw new BusinessException("Não é possível remover pedido em produção ou concluído.");
        }

        pedidoRepository.delete(pedido);
    }

    @Transactional
    public Pedido enviarParaProducao(Long id) {
        Pedido pedido = buscarPorId(id);

        if (pedido.getStatus() != null && pedido.getStatus() >= 2) {
            throw new BusinessException("Pedido já em produção ou concluído.");
        }

        smartService.enviarParaProducao(pedido);

        pedido.setStatus(2); // 2 = em produção
        return pedidoRepository.save(pedido);
    }

    /**
     * Conclusão manual pelo front.
     * O método grava a OP na expedição/CLP e marca o pedido como concluído.
     */
    @Transactional
    public Pedido atualizarStatus(Long id) {
        Pedido pedido = buscarPorId(id);

        if (pedido.getStatus() != null && pedido.getStatus() == 3) {
            throw new BusinessException("Pedido já concluído.");
        }

        expedicaoClpService.gravarPedidoFinalizadoNaMemoria(pedido);

        pedido.setStatus(3);
        return pedidoRepository.save(pedido);
    }

    private void gerarNumeroPedidoSeNecessario(Pedido pedido) {
        if (pedido.getNumeroPedido() == null || pedido.getNumeroPedido() <= 0) {
            Integer max = pedidoRepository.findMaxNumeroPedido();
            pedido.setNumeroPedido((max == null ? 0 : max) + 1);
        }

        pedidoRepository.findByNumeroPedido(pedido.getNumeroPedido()).ifPresent(p -> {
            throw new BusinessException("Já existe pedido com o número de OP: " + pedido.getNumeroPedido());
        });
    }

    private void validarEstruturaPedido(Pedido pedido) {
        if (pedido == null) {
            throw new BusinessException("Pedido não informado.");
        }

        if (pedido.getTipoPedido() == null || pedido.getTipoPedido() < 1 || pedido.getTipoPedido() > 3) {
            throw new BusinessException("Tipo de pedido inválido. Use 1, 2 ou 3.");
        }

        if (pedido.getCorTampa() == null || pedido.getCorTampa() < 1) {
            throw new BusinessException("Cor da tampa não informada.");
        }

        if (pedido.getBlocos() == null || pedido.getBlocos().isEmpty()) {
            throw new BusinessException("Pedido precisa possuir pelo menos um bloco.");
        }

        int quantidadeBlocos = pedido.getBlocos().size();

        if (pedido.getTipoPedido() != quantidadeBlocos) {
            throw new BusinessException("Quantidade de blocos não confere com o tipo de pedido.");
        }

        for (Bloco bloco : pedido.getBlocos()) {
            if (bloco.getCorBloco() == null || bloco.getCorBloco() < 1 || bloco.getCorBloco() > 3) {
                throw new BusinessException("Cor de bloco inválida. Use 1=preto, 2=vermelho ou 3=azul.");
            }

            if (bloco.getLaminas() == null) {
                throw new BusinessException("Lista de lâminas não informada.");
            }

            if (bloco.getLaminas().size() > 3) {
                throw new BusinessException("Cada bloco pode possuir no máximo 3 lâminas.");
            }
        }
    }

    /**
     * Regra:
     * sempre pega a primeira posição física disponível daquela cor,
     * respeitando a ordem do mapa de estoque.
     */
    private void selecionarEstoqueParaBlocos(Pedido pedido) {
        Set<Integer> posicoesUsadasNestePedido = new HashSet<>();

        List<Bloco> blocosOrdenados = pedido.getBlocos()
                .stream()
                .sorted(Comparator.comparing(
                        Bloco::getAndar,
                        Comparator.nullsLast(Integer::compareTo)
                ))
                .toList();

        for (Bloco bloco : blocosOrdenados) {
            Integer cor = bloco.getCorBloco();

            Estoque estoqueSelecionado = estoqueRepository
                    .findByCorOrderByPosicaoEstoque_PosicaoAsc(cor)
                    .stream()
                    .filter(e -> e.getPosicaoEstoque() != null)
                    .filter(e -> e.getPosicaoEstoque().getPosicao() != null)
                    .filter(e -> e.getQuantidade() != null && e.getQuantidade() > 0)
                    .filter(e -> !posicoesUsadasNestePedido.contains(e.getPosicaoEstoque().getPosicao()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(
                            "Não existe posição disponível no estoque para a cor: " + cor
                    ));

            PosicaoEstoque posicao = estoqueSelecionado.getPosicaoEstoque();
            Integer posicaoFisica = posicao.getPosicao();

            posicoesUsadasNestePedido.add(posicaoFisica);

            /*
             * Depois que o bloco foi reservado para o pedido, a posição fica vazia
             * para novos pedidos. O registro NÃO é deletado para preservar o vínculo
             * Bloco -> Estoque -> PosicaoEstoque, usado pelo SmartService no envio ao CLP.
             */
            estoqueSelecionado.setQuantidade(0);
            estoqueSelecionado.setCor(0);
            estoqueRepository.save(estoqueSelecionado);

            posicao.setDisponivel(true);
            posicaoEstoqueRepository.save(posicao);

            bloco.setEstoque(estoqueSelecionado);

            System.out.printf("[ESTOQUE] Cor %d vinculada à posição física %d%n", cor, posicaoFisica);
        }
    }

    private void vincularRelacionamentos(Pedido pedido) {
        pedido.getBlocos().forEach(bloco -> {
            bloco.setPedido(pedido);

            if (bloco.getLaminas() != null) {
                bloco.getLaminas().forEach(lamina -> lamina.setBloco(bloco));
            }
        });
    }
}
