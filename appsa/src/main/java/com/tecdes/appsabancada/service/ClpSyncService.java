package com.tecdes.appsabancada.service;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.tecdes.appsabancada.entity.Estoque;
import com.tecdes.appsabancada.entity.Expedicao;
import com.tecdes.appsabancada.entity.Pedido;
import com.tecdes.appsabancada.entity.PosicaoEstoque;
import com.tecdes.appsabancada.exception.BusinessException;
import com.tecdes.appsabancada.clpcomm.PlcConnectionService;
import com.tecdes.appsabancada.clpcomm.PlcConnector;
import com.tecdes.appsabancada.repository.EstoqueRepository;
import com.tecdes.appsabancada.repository.ExpedicaoRepository;
import com.tecdes.appsabancada.repository.PedidoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClpSyncService {

    private final PlcConnectionService plcConnectionService;
    private final EstoqueRepository estoqueRepository;
    private final ExpedicaoRepository expedicaoRepository;
    private final PedidoRepository pedidoRepository;

    @Value("${smart40.clp.estoque-ip:10.74.241.10}")
    private String ipClpEstoquePadrao;

    @Value("${smart40.clp.expedicao-ip:10.74.241.40}")
    private String ipClpExpedicaoPadrao;

    /**
     * Sincroniza o mapa de estoque do banco para o CLP de estoque.
     *
     * DB9:
     * offset 68 até 95 = 28 bytes, uma posição por byte.
     * 0 = livre; 1 = preto; 2 = vermelho; 3 = azul.
     */
    public Map<String, Object> sincronizarEstoque(String ipClpEstoque) {
        String ip = normalizarIp(ipClpEstoque, ipClpEstoquePadrao);

        PlcConnector plc = plcConnectionService.getConnection(ip);

        if (plc == null) {
            throw new BusinessException("Não foi possível conectar ao CLP de estoque: " + ip);
        }

        byte[] mapa = new byte[28];

        for (Estoque item : estoqueRepository.findAll()) {
            PosicaoEstoque posicao = item.getPosicaoEstoque();

            if (posicao == null || posicao.getPosicao() == null) {
                continue;
            }

            int numeroPosicao = posicao.getPosicao();

            if (numeroPosicao < 1 || numeroPosicao > 28) {
                continue;
            }

            boolean ocupado = item.getCor() != null
                    && item.getCor() > 0
                    && item.getQuantidade() != null
                    && item.getQuantidade() > 0;

            mapa[numeroPosicao - 1] = (byte) (ocupado ? item.getCor() : 0);
        }

        try {
            plc.writeBlock(9, 68, mapa.length, mapa);
        } catch (Exception e) {
            throw new BusinessException("Erro ao sincronizar estoque no CLP: " + e.getMessage());
        }

        Map<String, Object> resposta = new HashMap<>();
        resposta.put("ip", ip);
        resposta.put("db", 9);
        resposta.put("offset", 68);
        resposta.put("bytes", mapa.length);
        resposta.put("mensagem", "Estoque sincronizado no CLP.");

        return resposta;
    }

    /**
     * Sincroniza o mapa de expedição do banco para o CLP de expedição.
     *
     * DB9:
     * offset 6 até 29 = 12 words, uma OP por posição.
     */
    public Map<String, Object> sincronizarExpedicao(String ipClpExpedicao) {
        String ip = normalizarIp(ipClpExpedicao, ipClpExpedicaoPadrao);

        PlcConnector plc = plcConnectionService.getConnection(ip);

        if (plc == null) {
            throw new BusinessException("Não foi possível conectar ao CLP de expedição: " + ip);
        }

        ByteBuffer buffer = ByteBuffer.allocate(24);

        int[] posicoes = new int[12];

        for (Expedicao item : expedicaoRepository.findAll()) {
            if (item.getPosicaoExpedicao() == null
                    || item.getPosicaoExpedicao() < 1
                    || item.getPosicaoExpedicao() > 12) {
                continue;
            }

            int numeroOp = resolverNumeroOp(item);
            posicoes[item.getPosicaoExpedicao() - 1] = numeroOp;
        }

        for (int valor : posicoes) {
            buffer.putShort((short) valor);
        }

        byte[] mapa = buffer.array();

        try {
            plc.writeBlock(9, 6, mapa.length, mapa);
        } catch (Exception e) {
            throw new BusinessException("Erro ao sincronizar expedição no CLP: " + e.getMessage());
        }

        Map<String, Object> resposta = new HashMap<>();
        resposta.put("ip", ip);
        resposta.put("db", 9);
        resposta.put("offset", 6);
        resposta.put("bytes", mapa.length);
        resposta.put("mensagem", "Expedição sincronizada no CLP.");

        return resposta;
    }

    public Map<String, Object> sincronizarTudo(Map<String, String> ips) {
        Map<String, Object> resposta = new HashMap<>();

        String ipEstoque = ips != null ? ips.get("estoque") : null;
        String ipExpedicao = ips != null ? ips.get("expedicao") : null;

        try {
            resposta.put("estoque", sincronizarEstoque(ipEstoque));
        } catch (Exception e) {
            resposta.put("estoqueErro", e.getMessage());
        }

        try {
            resposta.put("expedicao", sincronizarExpedicao(ipExpedicao));
        } catch (Exception e) {
            resposta.put("expedicaoErro", e.getMessage());
        }

        return resposta;
    }

    private String normalizarIp(String informado, String padrao) {
        if (informado == null || informado.isBlank()) {
            return padrao;
        }

        return informado.trim();
    }

    private int resolverNumeroOp(Expedicao item) {
        if (item.getPedidoId() == null) {
            return 0;
        }

        return pedidoRepository.findById(item.getPedidoId())
                .map(Pedido::getNumeroPedido)
                .filter(n -> n != null && n > 0)
                .orElse(item.getPedidoId().intValue());
    }
}
