package com.smart.appsa.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smart.appsa.Entity.Expedicao;
import com.smart.appsa.Entity.Pedido;
import com.smart.appsa.Exception.BusinessException;
import com.smart.appsa.clpcomm.PlcConnectionService;
import com.smart.appsa.clpcomm.PlcConnector;
import com.smart.appsa.repository.ExpedicaoRepository;
import com.smart.appsa.repository.PedidoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExpedicaoClpService {

    private final PlcConnectionService plcConnectionService;
    private final ExpedicaoRepository expedicaoRepository;
    private final PedidoRepository pedidoRepository;

    @Value("${smart40.clp.expedicao-ip:10.74.241.40}")
    private String ipClpExpedicao;

    private boolean recebidoOpExp = false;
    private boolean recebidoExpedicao = false;
    private boolean iniciarGuardarExp = false;
    private int posicaoGuardarExp = 0;

    private final int[] orderExpedicao = new int[12];

    private int numeroOPExp = 0;
    private boolean cancelOPExp = false;
    private boolean finishOPExp = false;
    private boolean startOPExp = false;

    private boolean ocupadoExp = false;
    private boolean aguardandoExp = false;
    private boolean manualExp = false;
    private boolean emergenciaExp = false;

    private boolean pedirPosicaoExp = false;
    private int posicaoGuardadoExpedicao = 0;
    private int posicaoRemovidoExpedicao = 0;
    private boolean adicionarExpedicao = false;
    private boolean removerExpedicao = false;
    private int opGuardadoExpedicao = 0;

    public List<Expedicao> listarExpedicao() {
        return expedicaoRepository.findAll();
    }

    /**
     * Leitura cíclica do CLP de expedição.
     * Esse método é chamado pelo ClpController através do PlcReaderDB.
     */
    @Transactional
    public void processData(String ip, byte[] dadosClp4) {
        if (dadosClp4 == null || dadosClp4.length < 46) {
            return;
        }

        PlcConnector plcConnectorExp = plcConnectionService.getConnection(ip);

        if (plcConnectorExp == null) {
            return;
        }

        lerVariaveis(dadosClp4);

        tratarStatusOperacao(plcConnectorExp);
        tratarPedidoDePosicao(plcConnectorExp);
        tratarAdicionarExpedicao(plcConnectorExp);
        tratarRemoverExpedicao(plcConnectorExp);
        tratarFinalizacaoAutomatica();
    }

    private void lerVariaveis(byte[] dadosClp4) {
        recebidoOpExp = (dadosClp4[0] & 0x01) != 0;

        recebidoExpedicao = (dadosClp4[2] & 0x01) != 0;
        iniciarGuardarExp = (dadosClp4[2] & 0x02) != 0;
        posicaoGuardarExp = ((dadosClp4[4] & 0xFF) << 8) | (dadosClp4[5] & 0xFF);

        int x = 0;
        for (int c = 0; c < 24; c += 2) {
            orderExpedicao[x] = ((dadosClp4[c + 6] & 0xFF) << 8) | (dadosClp4[c + 7] & 0xFF);
            x++;
        }

        numeroOPExp = ((dadosClp4[30] & 0xFF) << 8) | (dadosClp4[31] & 0xFF);
        cancelOPExp = (dadosClp4[32] & 0x01) != 0;
        finishOPExp = (dadosClp4[32] & 0x02) != 0;
        startOPExp = (dadosClp4[32] & 0x04) != 0;

        ocupadoExp = (dadosClp4[34] & 0x01) != 0;
        aguardandoExp = (dadosClp4[34] & 0x02) != 0;
        manualExp = (dadosClp4[34] & 0x04) != 0;
        emergenciaExp = (dadosClp4[34] & 0x08) != 0;

        pedirPosicaoExp = (dadosClp4[36] & 0x01) != 0;
        posicaoGuardadoExpedicao = ((dadosClp4[38] & 0xFF) << 8) | (dadosClp4[39] & 0xFF);
        posicaoRemovidoExpedicao = ((dadosClp4[40] & 0xFF) << 8) | (dadosClp4[41] & 0xFF);
        adicionarExpedicao = (dadosClp4[42] & 0x01) != 0;
        removerExpedicao = (dadosClp4[42] & 0x02) != 0;
        opGuardadoExpedicao = ((dadosClp4[44] & 0xFF) << 8) | (dadosClp4[45] & 0xFF);
    }

    private void tratarStatusOperacao(PlcConnector plc) {
        if (!MonitorService.readOnly && !startOPExp && !finishOPExp && !cancelOPExp) {
            try {
                plc.writeBit(9, 0, 0, false);
            } catch (Exception e) {
                System.out.println("ERRO: não conseguiu limpar RecebidoOPExp DB9.DBX0.0");
            }
        }

        if (startOPExp && !recebidoOpExp) {
            MonitorService.statusExpedicao = 1;

            if (!MonitorService.readOnly) {
                try {
                    plc.writeBit(9, 0, 0, true);
                } catch (Exception e) {
                    System.out.println("ERRO: não conseguiu confirmar StartOPExp DB9.DBX0.0");
                }
            }
        }

        if (finishOPExp && !recebidoOpExp) {
            MonitorService.statusExpedicao = 2;
            MonitorService.blockFinished = true;

            if (!MonitorService.readOnly) {
                try {
                    plc.writeBit(9, 0, 0, true);
                } catch (Exception e) {
                    System.out.println("ERRO: não conseguiu confirmar FinishOPExp DB9.DBX0.0");
                }
            }
        }
    }

    /**
     * Quando o CLP pedir posição para guardar, o backend responde a primeira posição
     * livre da expedição.
     */
    private void tratarPedidoDePosicao(PlcConnector plc) {
        if (!pedirPosicaoExp) {
            MonitorService.aux_expedicao = false;

            if (!MonitorService.readOnly) {
                try {
                    plc.writeBit(9, 2, 1, false);
                } catch (Exception e) {
                    System.out.println("ERRO: não conseguiu limpar IniciarGuardarExp DB9.DBX2.1");
                }
            }

            return;
        }

        if (pedirPosicaoExp && !MonitorService.aux_expedicao && !MonitorService.readOnly) {
            MonitorService.aux_expedicao = true;

            int posicaoLivre = buscarPrimeiraPosicaoLivreExp();

            if (posicaoLivre <= 0) {
                System.out.println("ERRO: não existe posição livre na expedição.");
                return;
            }

            MonitorService.posicaoExpedicaoSolicitada = posicaoLivre;

            try {
                plc.writeInt(9, 4, posicaoLivre);
                plc.writeBit(9, 2, 1, true);

                System.out.println("[EXPEDIÇÃO] Posição livre enviada ao CLP: " + posicaoLivre);
            } catch (Exception e) {
                System.out.println("ERRO: não conseguiu enviar posição livre para expedição.");
            }
        }
    }

    /**
     * Quando o CLP informa que adicionou uma OP na expedição:
     * - confirma recebido;
     * - grava OP na memória DB9.DBW6 + posição;
     * - salva na tabela expedicao;
     * - marca o pedido como concluído automaticamente.
     */
    private void tratarAdicionarExpedicao(PlcConnector plc) {
        if (!adicionarExpedicao || MonitorService.aux_expedicao) {
            return;
        }

        MonitorService.aux_expedicao = true;

        int posicao = posicaoGuardadoExpedicao > 0
                ? posicaoGuardadoExpedicao
                : posicaoGuardarExp;

        int numeroOp = opGuardadoExpedicao > 0
                ? opGuardadoExpedicao
                : numeroOPExp;

        if (posicao <= 0 || posicao > 12 || numeroOp <= 0) {
            System.out.println("[EXPEDIÇÃO] Adicionar ignorado. Posição/OP inválida.");
            return;
        }

        if (!MonitorService.readOnly) {
            try {
                plc.writeBit(9, 2, 0, true);

                int offset = 6 + ((posicao - 1) * 2);
                plc.writeInt(9, offset, numeroOp);

                System.out.printf("[EXPEDIÇÃO] OP %d gravada no CLP na posição %d DB9.DBW%d%n",
                        numeroOp, posicao, offset);

            } catch (Exception e) {
                System.out.println("ERRO: não conseguiu gravar OP na memória da expedição.");
                e.printStackTrace();
            }
        }

        salvarExpedicaoLocal(posicao, numeroOp);
        finalizarPedidoAutomaticamente(numeroOp, posicao);
    }

    /**
     * Quando o CLP informa remoção, limpa:
     * - memória do CLP;
     * - tabela expedicao.
     */
    private void tratarRemoverExpedicao(PlcConnector plc) {
        if (!removerExpedicao || MonitorService.aux_expedicao) {
            return;
        }

        MonitorService.aux_expedicao = true;

        int posicao = posicaoRemovidoExpedicao;

        if (posicao <= 0 || posicao > 12) {
            System.out.println("[EXPEDIÇÃO] Remover ignorado. Posição inválida.");
            return;
        }

        limparMemoriaExpedicao(plc, posicao);
        limparExpedicaoLocal(posicao);
    }

    private void tratarFinalizacaoAutomatica() {
        if (finishOPExp) {
            MonitorService.statusProducao = 1;
            MonitorService.pedidoEmCurso = false;
            MonitorService.blockFinished = true;
        }
    }

    /**
     * Chamado pelo botão do front para limpar uma posição da expedição.
     */
    @Transactional
    public void limparPosicaoExpedicao(Integer posicao) {
        if (posicao == null || posicao < 1 || posicao > 12) {
            throw new BusinessException("Posição de expedição inválida. Informe uma posição entre 1 e 12.");
        }

        PlcConnector plc = plcConnectionService.getConnection(ipClpExpedicao);

        if (plc == null) {
            throw new BusinessException("Não foi possível conectar ao CLP de expedição: " + ipClpExpedicao);
        }

        limparMemoriaExpedicao(plc, posicao);
        limparExpedicaoLocal(posicao);

        System.out.println("[EXPEDIÇÃO] Posição " + posicao + " removida da memória e do banco.");
    }

    private void limparMemoriaExpedicao(PlcConnector plc, Integer posicao) {
        int offset = 6 + ((posicao - 1) * 2);

        try {
            plc.writeInt(9, offset, 0);

            if (posicaoGuardarExp == posicao) {
                plc.writeInt(9, 4, 0);
            }

            plc.writeBit(9, 2, 0, true);
            Thread.sleep(200);
            plc.writeBit(9, 2, 0, false);

            System.out.printf("[EXPEDIÇÃO] Memória limpa: posição %d DB9.DBW%d = 0%n", posicao, offset);

        } catch (Exception e) {
            throw new BusinessException("Erro ao limpar memória da expedição no CLP: " + e.getMessage());
        }
    }

    private void salvarExpedicaoLocal(Integer posicao, Integer numeroOp) {
        if (posicao == null || posicao < 1 || posicao > 12) {
            return;
        }

        if (numeroOp == null || numeroOp <= 0) {
            limparExpedicaoLocal(posicao);
            return;
        }

        Expedicao expedicao = expedicaoRepository
                .findByPosicaoExpedicao(posicao)
                .orElseGet(Expedicao::new);

        Pedido pedido = pedidoRepository
                .findByNumeroPedido(numeroOp)
                .orElse(null);

        expedicao.setPosicaoExpedicao(posicao);
        expedicao.setPedidoId(pedido != null ? pedido.getIdPedido() : Long.valueOf(numeroOp));
        expedicao.setDataSaida(LocalDateTime.now());

        expedicaoRepository.save(expedicao);
    }

    private void limparExpedicaoLocal(Integer posicao) {
        expedicaoRepository
                .findByPosicaoExpedicao(posicao)
                .ifPresent(expedicaoRepository::delete);
    }

    private void finalizarPedidoAutomaticamente(Integer numeroOp, Integer posicao) {
        pedidoRepository.findByNumeroPedido(numeroOp).ifPresent(pedido -> {
            if (pedido.getStatus() == null || pedido.getStatus() < 3) {
                pedido.setStatus(3);
            }

            pedido.setPosicaoExpedicao(posicao);

            pedidoRepository.save(pedido);

            System.out.printf("[PEDIDO] OP %d finalizada automaticamente na posição %d%n",
                    numeroOp, posicao);
        });

        MonitorService.statusProducao = 1;
        MonitorService.pedidoEmCurso = false;
        MonitorService.blockFinished = true;
    }

    /**
     * Chamado pelo front quando o usuário clica em concluir manualmente.
     */
    @Transactional
    public void gravarPedidoFinalizadoNaMemoria(Pedido pedido) {
        if (pedido == null) {
            throw new BusinessException("Pedido não informado para gravação na expedição.");
        }

        if (pedido.getPosicaoExpedicao() == null
                || pedido.getPosicaoExpedicao() < 1
                || pedido.getPosicaoExpedicao() > 12) {
            throw new BusinessException("Posição de expedição inválida. Informe uma posição entre 1 e 12.");
        }

        if (pedido.getNumeroPedido() == null) {
            throw new BusinessException("Número da ordem de produção não informado.");
        }

        PlcConnector plc = plcConnectionService.getConnection(ipClpExpedicao);

        if (plc == null) {
            throw new BusinessException("Não foi possível conectar ao CLP de expedição: " + ipClpExpedicao);
        }

        int posicao = pedido.getPosicaoExpedicao();
        int numeroOp = pedido.getNumeroPedido();
        int offset = 6 + ((posicao - 1) * 2);

        try {
            plc.writeInt(9, offset, numeroOp);
            plc.writeInt(9, 4, posicao);

            plc.writeBit(9, 2, 1, true);
            Thread.sleep(400);
            plc.writeBit(9, 2, 1, false);

            salvarExpedicaoLocal(posicao, numeroOp);
            finalizarPedidoAutomaticamente(numeroOp, posicao);

            System.out.printf("[EXPEDIÇÃO] OP %d gravada manualmente no CLP DB9.DBW%d%n",
                    numeroOp, offset);

        } catch (Exception e) {
            throw new BusinessException("Erro ao gravar pedido finalizado no CLP de expedição: " + e.getMessage());
        }
    }

    public int buscarPrimeiraPosicaoLivreExp() {
        List<Integer> ocupadas = expedicaoRepository.findAllPosicoesOcupadas();

        for (int i = 1; i <= 12; i++) {
            if (!ocupadas.contains(i)) {
                return i;
            }
        }

        return -1;
    }
}