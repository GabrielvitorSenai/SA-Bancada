package com.smart.appsa.service;

import com.smart.appsa.clpcomm.PlcConnectionService;
import com.smart.appsa.clpcomm.PlcConnector;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class ProcessoClpService {

    //********************** Processo **************************
    //----------------------- NodeToPlc ------------------------
    /*---- StatusOP -------*/
    boolean recebidoOpPro = false;
    //----------------------- PlcToNode ------------------------
    /*---- StatusOP -------*/
    int numeroOPPro = 0;
    boolean cancelOPPro = false;
    boolean finishOPPro = false;
    boolean startOPPro = false;

    /*---- StatusEstacao -------*/
    boolean ocupadoPro = false;
    boolean aguardandoPro = false;
    boolean manualPro = false;
    boolean emergenciaPro = false;

    /*---- RealidadeAumentada --------*/
    boolean xEmergenciaAtivadaPro = false;
    boolean xComutadorAutomaticoPro = false;
    boolean xNecessitaHomeEixoXPro = false;
    boolean xNecessitaHomeEixoYPro = false;
    boolean xServoDesligadoEixoXPro = false;
    boolean xServoDesligadoEixoYPro = false;
    boolean xCondicaoIniciarPro = false;


    @Autowired private PlcConnectionService plcConnectionService;

    public void processData(String ip, byte[] dadosClp2) {
        // lógica que hoje está no método clpProcesso(...)

        //-------------- Apresentação no console -----------------
        StringBuilder leituraClp2 = new StringBuilder();
        for (byte b : dadosClp2) {
            leituraClp2.append(String.format("%02X ", b));
        }
        String clp2 = leituraClp2.toString().trim();
        //System.out.println("[CLP2] " + clp2);

        PlcConnector plcConnectorPro = plcConnectionService.getConnection(ip);
        if (plcConnectorPro == null) {
            return;
        }

        //-------------- Leitura das variáveis -------------------
        recebidoOpPro = (dadosClp2[0] & 0x01) != 0;

        numeroOPPro = ((dadosClp2[2] & 0xFF) << 8) | (dadosClp2[3] & 0xFF);
        cancelOPPro = (dadosClp2[4] & 0x01) != 0;
        finishOPPro = (dadosClp2[4] & 0x02) != 0;
        startOPPro = (dadosClp2[4] & 0x04) != 0;

        ocupadoPro = (dadosClp2[6] & 0x01) != 0;
        aguardandoPro = (dadosClp2[6] & 0x02) != 0;
        manualPro = (dadosClp2[6] & 0x04) != 0;
        emergenciaPro = (dadosClp2[6] & 0x08) != 0;

        // System.out.println("StatusEstoque: " + statusEstoque + "\n"
        //         + "StatusProcesso: " + statusProcesso + "\n"
        //         + "StatusMontagem: " + statusMontagem + "\n"
        //         + "StatusExpedicao: " + statusExpedicao + "\n");
        // Se as três flags (StartOP, FinishOP e CancelOP) estão em FALSE, então a flag
        // RecebidoOP fica em FALSE
        if (startOPPro == false && finishOPPro == false && cancelOPPro == false) {
            if (MonitorService.readOnly == false) {

                try {
                    plcConnectorPro.writeBit(2, 0, 0, Boolean.parseBoolean("FALSE")); // coloca RecebidoOPPro em FALSE
                } catch (Exception ex) {
                }
            }
        }
        // Se a estação PROCESSO sinalizou o inicio da operação e recebidoOpPro está em FALSE, então a
        // flag RecebidoOPPRO fica em TRUE
        if (startOPPro == true && recebidoOpPro == false) {
            if (MonitorService.statusProducao == 0 & MonitorService.pedidoEmCurso == true) {
                MonitorService.statusProcesso = 1;
            } else {
                //statusProcesso = 0;
            }

            if (MonitorService.readOnly == false) {
                try {
                    plcConnectorPro.writeBit(2, 0, 0, Boolean.parseBoolean("TRUE"));   // coloca RecebidoOPPro em TRUE
                } catch (Exception e) {

                    e.printStackTrace();
                }
            }

        }
        // Se a estação PROCESSO sinalizou o témino da operação e ficou OCUPADO, então a
        // flag RecebidoOP fica em TRUE
        if (finishOPPro == true && recebidoOpPro == false) {
            if (MonitorService.readOnly == false) {

                try {
                    plcConnectorPro.writeBit(2, 0, 0, Boolean.parseBoolean("TRUE"));  // coloca RecebidoOPPro em TRUE
                } catch (Exception e) {

                    e.printStackTrace();
                }
                if (MonitorService.statusProducao == 0 & MonitorService.pedidoEmCurso == true) {
                    MonitorService.statusProcesso = 2;
                } else {
                    //statusProcesso = 0;
                }

            }

        }



        
    }


}
