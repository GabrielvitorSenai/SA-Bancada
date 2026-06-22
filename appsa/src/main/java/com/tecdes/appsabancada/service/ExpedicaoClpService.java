package com.tecdes.appsabancada.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tecdes.appsabancada.entity.Expedicao;
import com.tecdes.appsabancada.entity.Pedido;
import com.tecdes.appsabancada.exception.BusinessException;
import com.tecdes.appsabancada.clpcomm.PlcConnectionService;
import com.tecdes.appsabancada.clpcomm.PlcConnector;
import com.tecdes.appsabancada.repository.ExpedicaoRepository;
import com.tecdes.appsabancada.repository.PedidoRepository;

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

    @Transactional
    public void processData(String ip, byte[] dadosClp4) {
        if (dadosClp4 == null || dadosClp4.length < 46) {
            return;
        }

        PlcConnector plc = plcConnectionService.getConnection(ip);

        if (plc == null) {
            return;
        }

        lerVariaveis(dadosClp4);

        tratarStatusOperacao(plc);
        tratarPedidoDePosicao(plc);
        tratarAdicionarExpedicao(plc);
        tratarRemoverExpedicao(plc);
        tratarFinalizacaoAutomatica();
    }

    private void lerVariaveis(byte[] d) {
        recebidoOpExp = (d[0] & 0x01) != 0;

        recebidoExpedicao = (d[2] & 0x01) != 0;
        iniciarGuardarExp = (d[2] & 0x02) != 0;
        posicaoGuardarExp = ((d[4] & 0xFF) << 8) | (d[5] & 0xFF);

        int x = 0;
        for (int c = 0; c < 24; c += 2) {
            orderExpedicao[x] = ((d[c + 6] & 0xFF) << 8) | (d[c + 7] & 0xFF);
            x++;
        }

        numeroOPExp = ((d[30] & 0xFF) << 8) | (d[31] & 0xFF);
        cancelOPExp = (d[32] & 0x01) != 0;
        finishOPExp = (d[32] & 0x02) != 0;
        startOPExp = (d[32] & 0x04) != 0;

        ocupadoExp = (d[34] & 0x01) != 0;
        aguardandoExp = (d[34] & 0x02) != 0;
        manualExp = (d[34] & 0x04) != 0;
        emergenciaExp = (d[34] & 0x08) != 0;

        pedirPosicaoExp = (d[36] & 0x01) != 0;
        posicaoGuardadoExpedicao = ((d[38] & 0xFF) << 8) | (d[39] & 0xFF);
        posicaoRemovidoExpedicao = ((d[40] & 0xFF) << 8) | (d[41] & 0xFF);
        adicionarExpedicao = (d[42] & 0x01) != 0;
        removerExpedicao = (d[42] & 0x02) != 0;
        opGuardadoExpedicao = ((d[44] & 0xFF) << 8) | (d[45] & 0xFF);
    }

    private void tratarStatusOperacao(PlcConnector plc) {
        if (!MonitorService.readOnly && !startOPExp && !finishOPExp && !cancelOPExp) {
            try {
                plc.writeBit(9, 0, 0, false);
            } catch (Exception e) {
                System.out.println("ERRO: limpar RecebidoOPExp DB9.DBX0.0");
            }
        }

        if (startOPExp && !recebidoOpExp) {
            MonitorService.statusExpedicao = 1;

            if (!MonitorService.readOnly) {
                try {
                    plc.writeBit(9, 0, 0, true);
                } catch (Exception e) {
                    System.out.println("ERRO: confirmar StartOPExp DB9.DBX0.0");
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
                    System.out.println("ERRO: confirmar FinishOPExp DB9.DBX0.0");
                }
            }
        }
    }

    private void tratarPedidoDePosicao(PlcConnector plc) {
        if (!pedirPosicaoExp) {
            MonitorService.aux_expedicao = false;

            if (!MonitorService.readOnly) {
                try {
                    plc.writeBit(9, 2, 1, false);
                } catch (Exception e) {
                    System.out.println("ERRO: limpar IniciarGuardarExp DB9.DBX2.1");
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
            } catch (Exception e) {
                System.out.println("ERRO: enviar posição livre para expedição.");
            }
        }
    }

    private void tratarAdicionarExpedicao(PlcConnector plc) {
        if (!adicionarExpedicao || MonitorService.aux_expedicao) {
            return;
        }

        MonitorService.aux_expedicao = true;

        int posicao = posicaoGuardadoExpedicao > 0 ? posicaoGuardadoExpedicao : posicaoGuardarExp;
        int numeroOp = opGuardadoExpedicao > 0 ? opGuardadoExpedicao : numeroOPExp;

        if (posicao <= 0 || posicao > 12 || numeroOp <= 0) {
            System.out.println("[EXPEDIÇÃO] Adicionar ignorado. Posição/OP inválida.");
            return;
        }

        if (!MonitorService.readOnly) {
            try {
                plc.writeBit(9, 2, 0, true);

                int offset = 6 + ((posicao - 1) * 2);
                plc.writeInt(9, offset, numeroOp);
            } catch (Exception e) {
                System.out.println("ERRO: gravar OP na memória da expedição.");
                e.printStackTrace();
            }
        }

        salvarExpedicaoLocal(posicao, numeroOp);
        finalizarPedidoAutomaticamente(numeroOp, posicao);
    }

    private void tratarRemoverExpedicao(PlcConnector plc) {
        if (!removerExpedicao || MonitorService.aux_expedicao) {
            return;
        }

        MonitorService.aux_expedicao = true;

        int posicao = posicaoRemovidoExpedicao;

        if (posicao <= 0 || posicao > 12) {
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

    @Transactional
    public void limparPosicaoExpedicao(Integer posicao) {
        if (posicao == null || posicao < 1 || posicao > 12) {
            throw new BusinessException("Posição de expedição inválida. Informe uma posição entre 1 e 12.");
        }

        PlcConnector plc = plcConnectionService.getConnection(ipClpExpedicao);

        if (plc != null) {
            limparMemoriaExpedicao(plc, posicao);
        }
        limparExpedicaoLocal(posicao);
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
        expedicao.setPedido(pedido);
        expedicao.setPedidoId(pedido != null ? pedido.getIdPedido() : Long.valueOf(numeroOp));
        expedicao.setNumeroPedido(numeroOp);
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
        });

        MonitorService.statusProducao = 1;
        MonitorService.pedidoEmCurso = false;
        MonitorService.blockFinished = true;
    }

    @Transactional
    public void gravarPedidoFinalizadoNaMemoria(Pedido pedido) {
        if (pedido == null) {
            throw new BusinessException("Pedido não informado para gravação na expedição.");
        }

        if (pedido.getPosicaoExpedicao() == null
                || pedido.getPosicaoExpedicao() < 1
                || pedido.getPosicaoExpedicao() > 12) {
            int livre = buscarPrimeiraPosicaoLivreExp();

            if (livre <= 0) {
                throw new BusinessException("Não existe posição livre na expedição.");
            }

            pedido.setPosicaoExpedicao(livre);
        }

        if (pedido.getNumeroPedido() == null) {
            throw new BusinessException("Número da ordem de produção não informado.");
        }

        PlcConnector plc = plcConnectionService.getConnection(ipClpExpedicao);

        int posicao = pedido.getPosicaoExpedicao();
        int numeroOp = pedido.getNumeroPedido();
        int offset = 6 + ((posicao - 1) * 2);

        try {
            if (plc == null) {
                salvarExpedicaoLocal(posicao, numeroOp);
                finalizarPedidoAutomaticamente(numeroOp, posicao);
                return;
            }
            plc.writeInt(9, offset, numeroOp);
            plc.writeInt(9, 4, posicao);

            plc.writeBit(9, 2, 1, true);
            Thread.sleep(400);
            plc.writeBit(9, 2, 1, false);

            salvarExpedicaoLocal(posicao, numeroOp);
            finalizarPedidoAutomaticamente(numeroOp, posicao);
        } catch (Exception e) {
            throw new BusinessException("Erro ao gravar pedido finalizado no CLP de expedição: " + e.getMessage());
        }
    }

    @Transactional
    public Expedicao salvarExpedicao(Expedicao expedicao) {
        if (expedicao == null || expedicao.getPosicaoExpedicao() == null || expedicao.getPosicaoExpedicao() < 1 || expedicao.getPosicaoExpedicao() > 12) {
            throw new BusinessException("Posição de expedição inválida. Informe uma posição entre 1 e 12.");
        }
        Integer numero = expedicao.getNumeroPedido() != null ? expedicao.getNumeroPedido() : expedicao.getOrderNumber();
        if (numero == null && expedicao.getPedidoId() != null) numero = expedicao.getPedidoId().intValue();
        salvarExpedicaoLocal(expedicao.getPosicaoExpedicao(), numero == null ? 0 : numero);
        return expedicaoRepository.findByPosicaoExpedicao(expedicao.getPosicaoExpedicao()).orElseThrow();
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
