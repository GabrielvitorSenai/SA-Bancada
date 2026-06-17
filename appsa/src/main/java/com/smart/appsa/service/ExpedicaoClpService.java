package com.smart.appsa.service;

import com.smart.appsa.clpcomm.PlcConnectionService;
import com.smart.appsa.clpcomm.PlcConnector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.smart.appsa.repository.ExpedicaoRepository;

@Service
public class ExpedicaoClpService {

    //********************** Expedição *************************
    //----------------------- NodeToPlc ------------------------
    /*---- StatusOP -------*/
    boolean recebidoOpExp = false;
    /*---- GerenciamentoExpedicao -------*/
    boolean recebidoExpedicao = false;
    boolean iniciarGuardarExp = false;
    int posicaoGuardarExp = 0;

    /*---- RemoverPedido -------*/
    int[] orderExpedicao = new int[12];

    //public static int posicaoExpedicaoSolicitada = 0;
    //----------------------- PlcToNode ------------------------
    /*---- StatusOP ------------------*/
    int numeroOPExp = 0;
    boolean cancelOPExp = false;
    boolean finishOPExp = false;
    boolean startOPExp = false;

    /*---- StatusEstacao -------*/
    boolean ocupadoExp = false;
    boolean aguardandoExp = false;
    boolean manualExp = false;
    boolean emergenciaExp = false;

    /*---- GerenciamentoEstoque ------*/
    boolean pedirPosicaoExp = false;
    int posicaoGuardadoExpedicao = 0;
    int posicaoRemovidoExpedicao = 0;
    boolean adicionarExpedicao = false;
    boolean removerExpedicao = false;
    int opGuardadoExpedicao = 0;

    @Autowired
    private PlcConnectionService plcConnectionService;

    @Autowired
    private ExpedicaoRepository expedicaoRepository;

    @Autowired
    private ApiIntegrationService apiIntegrationService;

    public void processData(String ip, byte[] dadosClp4) {
        // lógica que hoje está no método clpExpedicao(...)

        //-------------- Apresentação no console -----------------
        StringBuilder leituraClp4 = new StringBuilder();
        for (byte b : dadosClp4) {
            leituraClp4.append(String.format("%02X ", b));
        }
        String clp4 = leituraClp4.toString().trim();
        //System.out.println("[CLP4] " + clp4);

        PlcConnector plcConnectorExp = plcConnectionService.getConnection(ip);
        if (plcConnectorExp == null) {
            return;
        }
        //-------------- Leitura das variáveis -------------------
        recebidoOpExp = (dadosClp4[0] & 0x01) != 0;

        recebidoExpedicao = (dadosClp4[2] & 0x01) != 0;
        iniciarGuardarExp = (dadosClp4[2] & 0x02) != 0;
        posicaoGuardarExp = ((dadosClp4[4] & 0xFF) << 8) | (dadosClp4[5] & 0xFF);

        int x = 0;
        for (int c = 0; c < 24; c += 2) {
            orderExpedicao[x] = (int) ((dadosClp4[c + 6] & 0xFF) << 8) | (dadosClp4[c + 7] & 0xFF);
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

        // System.out.println("StatusEstoque: " + statusEstoque + "\n"
        //         + "StatusProcesso: " + statusProcesso + "\n"
        //         + "StatusMontagem: " + statusMontagem + "\n"
        //         + "StatusExpedicao: " + statusExpedicao + "\n");
        // Se as três flags (StartOPExp, FinishOPExp e CancelOPExp) estão em FALSE, então a flag
        // RecebidoOPExp fica em FALSE
        if (startOPExp == false & finishOPExp == false & cancelOPExp == false) {
            if (MonitorService.readOnly == false) {
                try {

                    //System.out.println("(startOPExp == false & finishOPExp == false & cancelOPExp == false): Atualização da Flag RecebidoOPExp [DB9:2.0] para FALSE");
                    plcConnectorExp.writeBit(9, 0, 0, Boolean.parseBoolean("FALSE")); // coloca RecebidoOPExp em FALSE

                } catch (Exception e) {
                    System.out.println(
                            "ERRO [startOp][finishOp]: Atualização da Flag RecebidoOPExp [DB9:0.0] para FALSE");
                }

            }
        }

        // Se o pedido foi finalizado pela estação de MONTAGEM e a estação EXPEDIÇÃO
        // informou que iniciou a operação
        // então a flag recebidoOpExp fica em TRUE
        if (startOPExp == true & recebidoOpExp == false) {
            if (MonitorService.statusProducao == 0 & MonitorService.pedidoEmCurso == true) {
                MonitorService.statusExpedicao = 1;
            } else {
                //statusExpedicao = 0;
            }
            // blockFinished = true;
            // updateDisplayStation();
            if (MonitorService.readOnly == false) {
                try {
                    plcConnectorExp.writeBit(9, 0, 0, Boolean.parseBoolean("TRUE")); // coloca RecebidoOPExp em TRUE

                } catch (Exception e) {
                    System.out.println(
                            "ERRO [startOp]: Atualização da Flag RecebidoOPExp [DB9:0.0] para TRUE");
                }

            }
        }

        // Se a estação EXPEDIÇÃO sinalizou o término da operação e ficou OCUPADO, então
        // a flag RecebidoOP fica em TRUE
        if (finishOPExp == true & recebidoOpExp == false) {
            if (MonitorService.readOnly == false) {
                // JOptionPane.showMessageDialog(null, "1 - Vou iniciar a guarda do BLOCO!!!");

                try {
                    // Panel3.plcWrite = new PlcConnector(ipExpedicao, 9, 0, 1, 0, 1);
                    plcConnectorExp.writeBit(9, 0, 0, Boolean.parseBoolean("TRUE")); // coloca RecebidoOPExp em TRUE
                    MonitorService.blockFinished = true;

                } catch (Exception e) {
                    System.out.println(
                            "ERRO [finishOp]: Atualização da Flag RecebidoOPExp [DB9:0.0] para TRUE");
                }
                if (MonitorService.statusProducao == 0 & MonitorService.pedidoEmCurso == true) {
                    MonitorService.statusExpedicao = 2;
                } else {
                    //statusExpedicao = 0;

                }
            }
        }

        if (pedirPosicaoExp == false) {
            if (!MonitorService.readOnly) {
                MonitorService.aux_expedicao = false;
                // Coloca a flag IniciarGuardar em FALSE
                try {
                    plcConnectorExp.writeBit(9, 2, 1, Boolean.parseBoolean("FALSE"));  // coloca  IniciarGuardar em FALSE

                } catch (Exception e) {
                    System.out.println(
                            "ERRO [Pedir Posição]: Atualização da Flag IniciarGuardar [DB9:2.1] para FALSE");
                }
            }
        }

        // verifica se Expedição pede posição para guardar
        if ((pedirPosicaoExp == true) & MonitorService.aux_expedicao == false) {

            //System.out.println(
            //       "\n\nEstou aqui -  if ((pedirPosicaoExp == true) & aux_expedicao == false)\n\n");
            // Rotina para verificar qual posição está disponível para guardar
            MonitorService.aux_expedicao = true;

            // ROTINA PARA LOCALIZAR POSIÇÃO DISPONÍVEL NO MAGAZINE DA EXPEDIÇÃO PARA
            // ADICIONAR BLOCO CONCLUÍDO
            // Rotina para verificar qual posição está disponível para guardar
            if (!MonitorService.readOnly) {

                // Solicita posição disponível para guardar (0-LIVRE 1-OCUPADA)
                // Certifique-se de que posExpedicaoLivre é seguro para acesso
                //int posExpedicaoLivre = posicaoExpedicaoSolicitada/*getPositionExpedicao()*/;
                //System.out.println("Posição disponível no Magazine Expedição: " + posExpedicaoLivre);
                // Atualiza a variável PosicaoGuardarExpedicao no CLP EXPEDIÇÂO
                try {
                    plcConnectorExp.writeInt(9, 4, MonitorService.posicaoExpedicaoSolicitada);   // Atualiza a variável PosicaoGuardarExpedicao no CLP EXPEDIÇÂO

                } catch (Exception e) {
                    System.out.println("ERRO: Atualização da PosicaoGuardarExpedicao [DB9:4]");
                }

                try {
                    plcConnectorExp.writeBit(9, 2, 1, Boolean.parseBoolean("TRUE")); // coloca IniciarGuardar em TRUE

                } catch (Exception e) {
                    System.out.println(
                            "ERRO [Pedir Posição]: Atualização da Flag IniciarGuardar [DB9:2.1] para TRUE");
                }

            }

        }

        if (!MonitorService.readOnly & (!adicionarExpedicao || !removerExpedicao)) {
            try {
                //System.out.println("(!readOnly & (!adicionarExpedicao || !removerExpedicao)): Atualização da Flag RecebidoExpedicao [DB9:2.0] para FALSE");
                plcConnectorExp.writeBit(9, 2, 0, false); // coloca RecebidoExpedicao em FALSE
            } catch (Exception e) {
                if (!adicionarExpedicao & !removerExpedicao) {
                    System.out.println("ERRO [Adicionar e Remover Expedição]: Atualização da Flag RecebidoExpedicao [DB9:2.0] para FALSE");
                } else if (!adicionarExpedicao) {
                    System.out.println("ERRO [Adicionar Expedição]: Atualização da Flag RecebidoExpedicao [DB9:2.0] para FALSE");
                } else {
                    System.out.println("ERRO [Remover Expedição]: Atualização da Flag RecebidoExpedicao [DB9:2.0] para FALSE");
                }
            }
        }

        // Se a flag adicionarExpedicao está TRUE E aux_expedicao está FALSE então a flag RecebidoExpedicao fica em TRUE
        if ((adicionarExpedicao == true) & MonitorService.aux_expedicao == false) {
            MonitorService.aux_expedicao = true;

            // Ler as variáveis PosicaoGuardadoExpedicao e opGuardadoExpedicao
            if (MonitorService.readOnly == false) {

                try {
                    // Panel3.plcWrite = new PlcConnector(ipExpedicao, 9, 2, 1, 0, 1);
                    plcConnectorExp.writeBit(9, 2, 0, Boolean.parseBoolean("TRUE")); // coloca RecebidoExpedicao em TRUE

                } catch (Exception e) {
                    System.out.println(
                            "ERRO [Adicionar Expedição]: Atualização da Flag RecebidoExpedicao [DB9:2.0] para TRUE");
                }

                int offset = 6 + (posicaoGuardarExp - 1) * 2;
                System.out.println("Guardando Operacao em posicaoGuardarExp: " + posicaoGuardarExp);
                if (posicaoGuardarExp > 0) {

                    try {
                        // Atualiza cor no CLP
                        plcConnectorExp.writeInt(9, offset, opGuardadoExpedicao);

                        // Cria mapa de dados com apenas uma posição
                        Map<String, Integer> dadosMap = new HashMap<>();
                        dadosMap.put("OP:" + posicaoGuardarExp, opGuardadoExpedicao);

                        // === Chama serviço de integração ===
                        boolean sucesso = apiIntegrationService.salvarExpedicao(dadosMap);

                        if (sucesso) {
                            System.out.println("Expedição salva com sucesso na API.");
                        } else {
                            System.out.println("Falha ao salvar expedição na API.");
                            // aqui você poderia lançar uma exceção ou marcar para tentar novamente
                        }

                    } catch (Exception e) {
                        System.out.println("ERRO: Na tentativa de adicionar na Expedição");
                        e.printStackTrace();
                    }
                }
            }

        }
        // Se a flag removerExpedicao está TRUE E aux_expedicao está FALSE então a flag RecebidoExpedicao fica em TRUE
        if ((removerExpedicao == true) & MonitorService.aux_expedicao == false) { // verifica se Expedição pede posição
            // para remover
            MonitorService.aux_expedicao = true;
            //System.out.println("Estou Aqui em => (removerExpedicao == true) & aux_expedicao == false)");

            // Ler a variável PosicaoRemovidoExpedicao
            // posicaoRemovidoExpedicao = ((dadosClp4[40] & 0xFF) << 8) | (dadosClp4[41] & 0xFF);
            //if (readOnly == false) {
            // System.out.println("Flag: RecebidoExpediçcao_TRUE");
            if (MonitorService.readOnly == false) {
                try {
                    // Panel3.plcWrite = new PlcConnector(ipExpedicao, 9, 2, 1, 0, 1);
                    plcConnectorExp.writeBit(9, 2, 0, Boolean.parseBoolean("TRUE")); // coloca RecebidoOPExpedicao em TRUE

                } catch (Exception e) {
                    System.out.println(
                            "ERRO [Adicionar Expedição]: Atualização da Flag RecebidoExpedicao [DB9:2.0] para TRUE");
                }

                int offset = 6 + (posicaoRemovidoExpedicao - 1) * 2;

                System.out.println("Removendo Operacao de posicaoREmovidoExpedicao: " + posicaoRemovidoExpedicao);

                if (posicaoRemovidoExpedicao > 0 && !MonitorService.readOnly) {

                    try {
                        // Atualiza cor no CLP
                        plcConnectorExp.writeInt(9, offset, 0);

                        // Cria mapa de dados com apenas uma posição
                        Map<String, Integer> dadosMap = new HashMap<>();
                        dadosMap.put("OP:" + posicaoRemovidoExpedicao, 0);

                        // === Chama serviço de integração ===
                        boolean sucesso = apiIntegrationService.salvarExpedicao(dadosMap);

                        if (sucesso) {
                            System.out.println("Expedição salva com sucesso na API.");
                        } else {
                            System.out.println("Falha ao salvar expedição na API.");
                            // aqui você poderia lançar uma exceção ou marcar para tentar novamente
                        }

                    } catch (Exception e) {
                        System.out.println("ERRO: Na tentativa de remover da Expedição");
                        e.printStackTrace();
                    }
                }
            }
            // adicionaRemoveMagazineExpedicao(posicaoRemovidoExpedicao, 0);
            // updatePlcExpedicao();
            //}
        }

        if ((posicaoGuardadoExpedicao == posicaoGuardarExp) & (ocupadoExp == false) & (finishOPExp == true)) {

            if (MonitorService.readOnly == false) {

                System.out.println("AQUI: statusProducao: " + MonitorService.statusProducao);
                System.out.println("AQUI: pedidoEmCurso:: " + MonitorService.pedidoEmCurso);
                if (MonitorService.statusProducao == 0 & MonitorService.pedidoEmCurso == true) {

                    System.out.println("--------------------------------------------------");
                    System.out.println(" ");
                    //pedidoEmCurso = false;
                    MonitorService.statusProducao = 1;
                }

                System.out.println("Operação OP:" + opGuardadoExpedicao + " Finalizada: ");
            }

            // }
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
