package com.smart.appsa.service;

import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.smart.appsa.Entity.Bloco;
import com.smart.appsa.Entity.Lamina;
import com.smart.appsa.Entity.Pedido;
import com.smart.appsa.Exception.BusinessException;
import com.smart.appsa.clpcomm.PlcConnectionService;
import com.smart.appsa.clpcomm.PlcConnector;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SmartService {

    /**
     * Mapa de memória do DB9:
     *
     *   DBB 0        → flags de controle (CLP gerencia)
     *   DBW 2..61    → dados do pedido — 60 bytes = 30 words (escrevemos aqui)
     *   DBX 62.0     → trigger "iniciar produção" (pulso SET→RST)
     *   DBX 64.0-1   → flags de status do CLP
     */
    private static final int DB_PEDIDO     = 9;
    private static final int OFFSET_DADOS  = 2;    // DB9.DBW2
    private static final int TAMANHO_DADOS = 60;   // 30 words × 2 bytes

    private static final int BYTE_CTRL    = 0;
    private static final int BYTE_TRIGGER = 62;
    private static final int BYTE_STATUS  = 64;

    private final PlcConnectionService plcConnectionService;

    @Value("${smart40.clp.ip}")
    private String ipClp;

    // ─────────────────────────────────────────────────────────────────────────
    // API pública
    // ─────────────────────────────────────────────────────────────────────────

    public void enviarParaProducao(Pedido pedido) {
        PlcConnector connector = plcConnectionService.getConnection(ipClp);
        if (connector == null) {
            throw new BusinessException("Não foi possível conectar ao CLP: " + ipClp);
        }

        byte[] buffer = converterPedidoParaBytes(pedido);
        printHex(buffer);

        try {
            // 1 — grava os 60 bytes no DB9 a partir do offset 2
            connector.writeBlock(DB_PEDIDO, OFFSET_DADOS, buffer.length, buffer);
            System.out.printf("[ CLP ] Pedido #%d gravado → DB%d.DBW%d (%d bytes)%n",
                    pedido.getNumeroPedido(), DB_PEDIDO, OFFSET_DADOS, buffer.length);

            // 2 — dispara a execução na bancada
            iniciarExecucaoPedido(connector);

        } catch (BusinessException be) {
            throw be;
        } catch (Exception ex) {
            throw new BusinessException("Erro na comunicação com o CLP: " + ex.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Conversão Pedido → buffer de 60 bytes
    //
    // Layout (alinhado com PedidoInfoDTO e o DB9 real da bancada):
    //
    //  Andar 1 — words 0..8  (offset DB = 2 + 0*18 = 2)
    //  Andar 2 — words 9..17 (offset DB = 2 + 1*18 = 20)
    //  Andar 3 — words 18..26(offset DB = 2 + 2*18 = 38)
    //
    //  Por andar (9 words = 18 bytes):
    //    Word +0  → Cor_Andar            (corBloco: 1=preto 2=vermelho 3=azul)
    //    Word +1  → Posicao_Estoque_Andar (posição física no armazém, 1..28)
    //    Word +2  → Cor_Lamina_1          (posição Esquerda; 0 = sem lâmina)
    //    Word +3  → Cor_Lamina_2          (posição Frente)
    //    Word +4  → Cor_Lamina_3          (posição Direita)
    //    Word +5  → Padrao_Lamina_1       (0=nenhum 1=casa 2=navio 3=estrela)
    //    Word +6  → Padrao_Lamina_2
    //    Word +7  → Padrao_Lamina_3
    //    Word +8  → Processamento_Andar   (0=aguardando; o CLP atualiza)
    //
    //  Dados gerais — words 27..29 (offset DB = 2 + 54 = 56):
    //    Word 27  → Numero_Pedido
    //    Word 28  → Andares (= tipoPedido: 1, 2 ou 3)
    //    Word 29  → Posicao_Expedicao
    //
    //  Total: 3 × 9 words + 3 words = 30 words = 60 bytes ✓
    // ─────────────────────────────────────────────────────────────────────────

    private byte[] converterPedidoParaBytes(Pedido pedido) {
        ByteBuffer buf = ByteBuffer.allocate(TAMANHO_DADOS);

        List<Bloco> blocos = sortedBlocos(pedido);

        for (int i = 0; i < 3; i++) {
            if (i < blocos.size()) {
                Bloco bloco = blocos.get(i);
                Map<Integer, Lamina> lamPorPos = laminasPorPosicao(bloco);

                putW(buf, bloco.getCorBloco());              // Cor_Andar
                putW(buf, posicaoEstoque(bloco));             // Posicao_Estoque_Andar
                putW(buf, corLamina(lamPorPos, 1));           // Cor_Lamina_1 (Esquerda)
                putW(buf, corLamina(lamPorPos, 2));           // Cor_Lamina_2 (Frente)
                putW(buf, corLamina(lamPorPos, 3));           // Cor_Lamina_3 (Direita)
                putW(buf, padraoLamina(lamPorPos, 1));        // Padrao_Lamina_1
                putW(buf, padraoLamina(lamPorPos, 2));        // Padrao_Lamina_2
                putW(buf, padraoLamina(lamPorPos, 3));        // Padrao_Lamina_3
                putW(buf, 0);                                  // Processamento_Andar (CLP)
            } else {
                // Andar não existente → 9 words zerados
                for (int j = 0; j < 9; j++) putW(buf, 0);
            }
        }

        // Dados gerais — sempre no final (words 27-29)
        putW(buf, pedido.getNumeroPedido());        // Numero_Pedido
        putW(buf, pedido.getTipoPedido());           // Andares
        putW(buf, pedido.getPosicaoExpedicao());     // Posicao_Expedicao

        return buf.array();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Trigger de início de produção (pulso de borda de subida → descida)
    // ─────────────────────────────────────────────────────────────────────────

    private void iniciarExecucaoPedido(PlcConnector plc) throws Exception {
        // Garante estado limpo antes do pulso
        plc.writeBit(DB_PEDIDO, BYTE_CTRL,    0, false);
        plc.writeBit(DB_PEDIDO, BYTE_STATUS,  0, false);
        plc.writeBit(DB_PEDIDO, BYTE_STATUS,  1, false);
        plc.writeBit(DB_PEDIDO, BYTE_TRIGGER, 0, false);

        // Borda de subida → CLP detecta e inicia o programa de produção
        System.out.printf("[ CLP ] SET  DB%d.DBX%d.0  (trigger inicio)%n", DB_PEDIDO, BYTE_TRIGGER);
        plc.writeBit(DB_PEDIDO, BYTE_TRIGGER, 0, true);

        Thread.sleep(800);  // aguarda o scan do CLP detectar a borda

        // Borda de descida
        System.out.printf("[ CLP ] RST  DB%d.DBX%d.0  (trigger inicio)%n", DB_PEDIDO, BYTE_TRIGGER);
        plc.writeBit(DB_PEDIDO, BYTE_TRIGGER, 0, false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers internos
    // ─────────────────────────────────────────────────────────────────────────

    /** Ordena blocos pelo andar (andar 1 = base, andar N = topo). */
    private List<Bloco> sortedBlocos(Pedido pedido) {
        if (pedido.getBlocos() == null) return List.of();
        return pedido.getBlocos().stream()
                .sorted(Comparator.comparing(Bloco::getAndar,
                        Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    /** Mapeia posição (1=Esq, 2=Frente, 3=Dir) → Lâmina para acesso O(1). */
    private Map<Integer, Lamina> laminasPorPosicao(Bloco bloco) {
        Map<Integer, Lamina> map = new HashMap<>();
        if (bloco.getLaminas() != null) {
            bloco.getLaminas().forEach(l -> {
                if (l.getPosicao() != null) map.put(l.getPosicao(), l);
            });
        }
        return map;
    }

    /**
     * Retorna a posição física do estoque vinculada ao bloco.
     * Retorna 0 se o vínculo não estiver presente (não deve ocorrer em pedidos novos).
     */
    private int posicaoEstoque(Bloco bloco) {
        if (bloco.getEstoque() == null
                || bloco.getEstoque().getPosicaoEstoque() == null
                || bloco.getEstoque().getPosicaoEstoque().getPosicao() == null) {
            System.err.println("AVISO: bloco sem posição de estoque vinculada — enviando 0 ao CLP.");
            return 0;
        }
        return bloco.getEstoque().getPosicaoEstoque().getPosicao();
    }

    private int corLamina(Map<Integer, Lamina> map, int posicao) {
        Lamina l = map.get(posicao);
        return (l != null && l.getCorLamina() != null) ? l.getCorLamina() : 0;
    }

    private int padraoLamina(Map<Integer, Lamina> map, int posicao) {
        Lamina l = map.get(posicao);
        return (l != null && l.getPadraoLamina() != null) ? l.getPadraoLamina() : 0;
    }

    private void putW(ByteBuffer buf, Integer value) {
        buf.putShort((short) (value == null ? 0 : value));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Debug: imprime o buffer como tabela de words com rótulos
    // ─────────────────────────────────────────────────────────────────────────

    public void printHex(byte[] bytes) {
        System.out.println("╔══ DB9 PAYLOAD — 60 bytes a partir de DB9.DBW" + OFFSET_DADOS + " ══╗");
        for (int i = 0; i < bytes.length; i += 2) {
            int word = ((bytes[i] & 0xFF) << 8) | (bytes[i + 1] & 0xFF);
            System.out.printf("  W%02d  DB9.DBW%02d  =  %5d   ← %s%n",
                    i / 2, OFFSET_DADOS + i, word, descricaoWord(i / 2));
        }
        System.out.println("╚════════════════════════════════════════════════════╝");
    }

    private String descricaoWord(int wi) {
        if (wi < 27) {
            int andar = wi / 9 + 1;
            String[] campos = {
                "Cor_Andar", "Posicao_Estoque",
                "Cor_Lamina_1(Esq)", "Cor_Lamina_2(Frente)", "Cor_Lamina_3(Dir)",
                "Padrao_Lamina_1",   "Padrao_Lamina_2",      "Padrao_Lamina_3",
                "Processamento"
            };
            return "Andar " + andar + " → " + campos[wi % 9];
        }
        return switch (wi) {
            case 27 -> "Numero_Pedido";
            case 28 -> "Andares (tipoPedido)";
            case 29 -> "Posicao_Expedicao";
            default  -> "—";
        };
    }
}