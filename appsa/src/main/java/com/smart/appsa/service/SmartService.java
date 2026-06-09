package com.smart.appsa.service;

import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.List;

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

    private static final int DB_PEDIDO = 9;
    private static final int OFFSET_DADOS = 2;
    private static final int TAMANHO_BLOCO_BYTES = 60;

    private final PlcConnectionService plcConnectionService;

    @Value("${smart40.clp.ip}")
    private String ipClp;

    public void enviarParaProducao(Pedido pedido) {
        byte[] buffer = converterPedidoParaBytes(pedido);

        printHex(buffer);

        PlcConnector connector = plcConnectionService.getConnection(ipClp);

        if (connector == null) {
            throw new BusinessException("Não foi possível conectar ao CLP: " + ipClp);
        }

        try {
            connector.writeBlock(DB_PEDIDO, OFFSET_DADOS, buffer.length, buffer);

            System.out.println("Dados enviados para o CLP: " + ipClp);

            iniciarExecucaoPedido(ipClp);

        } catch (Exception ex) {
            throw new BusinessException("Erro ao enviar dados para o CLP: " + ex.getMessage());
        }
    }

    private byte[] converterPedidoParaBytes(Pedido pedido) {
        ByteBuffer buffer = ByteBuffer.allocate(TAMANHO_BLOCO_BYTES);

        /*
         * Layout sugerido:
         *
         * 1 - numeroPedido
         * 2 - tipoPedido
         * 3 - corTampa
         * 4 - posicaoExpedicao
         *
         * Depois até 3 blocos:
         * - corBloco
         * - andar
         * - corLamina 1
         * - padraoLamina 1
         * - corLamina 2
         * - padraoLamina 2
         * - corLamina 3
         * - padraoLamina 3
         *
         * O restante fica zerado até completar 60 bytes.
         */

        putShort(buffer, pedido.getNumeroPedido());
        putShort(buffer, pedido.getTipoPedido());
        putShort(buffer, pedido.getCorTampa());
        putShort(buffer, pedido.getPosicaoExpedicao());

        List<Bloco> blocos = pedido.getBlocos() == null
                ? List.of()
                : pedido.getBlocos()
                        .stream()
                        .sorted(Comparator.comparing(
                                Bloco::getAndar,
                                Comparator.nullsLast(Integer::compareTo)
                        ))
                        .toList();

        for (int i = 0; i < 3; i++) {
            if (i < blocos.size()) {
                Bloco bloco = blocos.get(i);

                putShort(buffer, bloco.getCorBloco());
                putShort(buffer, bloco.getAndar());

                List<Lamina> laminas = bloco.getLaminas() == null
                        ? List.of()
                        : bloco.getLaminas()
                                .stream()
                                .sorted(Comparator.comparing(
                                        Lamina::getPosicao,
                                        Comparator.nullsLast(Integer::compareTo)
                                ))
                                .toList();

                for (int j = 0; j < 3; j++) {
                    if (j < laminas.size()) {
                        Lamina lamina = laminas.get(j);

                        putShort(buffer, lamina.getCorLamina());
                        putShort(buffer, lamina.getPadraoLamina());
                    } else {
                        putShort(buffer, 0);
                        putShort(buffer, 0);
                    }
                }

            } else {
                // Bloco vazio
                putShort(buffer, 0); // corBloco
                putShort(buffer, 0); // andar

                for (int j = 0; j < 3; j++) {
                    putShort(buffer, 0); // corLamina
                    putShort(buffer, 0); // padraoLamina
                }
            }
        }

        while (buffer.remaining() >= 2) {
            putShort(buffer, 0);
        }

        return buffer.array();
    }

    private void putShort(ByteBuffer buffer, Integer valor) {
        buffer.putShort((short) (valor == null ? 0 : valor));
    }

    public void printHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();

        System.out.println("--- BLOCO DE BYTES (HEXADECIMAL) ---");

        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format("%02X ", bytes[i]));

            if ((i + 1) % 10 == 0) {
                sb.append("\n");
            }
        }

        System.out.println(sb);
        System.out.println("------------------------------------");
    }

    public void iniciarExecucaoPedido(String ipClp) {
        PlcConnector plcConnector = plcConnectionService.getConnection(ipClp);

        if (plcConnector == null) {
            throw new BusinessException("Não foi possível conectar ao CLP: " + ipClp);
        }

        try {
            plcConnector.writeBit(9, 0, 0, false);
            plcConnector.writeBit(9, 64, 0, false);
            plcConnector.writeBit(9, 64, 1, false);
            plcConnector.writeBit(9, 62, 0, false);

            System.out.println("SETAR FLAG INICIAR PEDIDO");
            plcConnector.writeBit(9, 62, 0, true);

            Thread.sleep(800);

            System.out.println("RESETAR FLAG INICIAR PEDIDO");
            plcConnector.writeBit(9, 62, 0, false);

        } catch (Exception ex) {
            throw new BusinessException("Erro ao iniciar execução do pedido no CLP: " + ex.getMessage());
        }
    }
}