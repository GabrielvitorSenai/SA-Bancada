package com.tecdes.appsabancada.service;

import com.tecdes.appsabancada.clpcomm.PlcConnectionService;
import com.tecdes.appsabancada.clpcomm.PlcConnector;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tecdes.appsabancada.config.ApiUrlConfig;
import com.tecdes.appsabancada.entity.Estoque;
import com.tecdes.appsabancada.repository.EstoqueRepository;
import com.tecdes.appsabancada.repository.ExpedicaoRepository;

@Service
public class MonitorService {

    // Variáveis globais do programa
    public static boolean readOnly = false;
    public static boolean aux_expedicao = false; // Aux Expedição

    public static boolean pedidoEmCurso = false;
    public static byte statusProducao = 0;

    public static byte statusEstoque = 0;
    public static byte statusProcesso = 0;
    public static byte statusMontagem = 0;
    public static byte statusExpedicao = 0;

    public static int posicaoEstoqueSolicitada = 0;
    public static int posicaoExpedicaoSolicitada = 0;

    public static boolean blockFinished = false;

    @Autowired
    private PlcConnectionService plcConnectionService;

    @Autowired
    private EstoqueRepository estoqueRepository;

    @Autowired
    private ExpedicaoRepository expedicaoRepository;

    @Autowired
    private ApiUrlConfig apiUrlConfig;

    /*---- RealidadeAumentada */
    boolean xEmergenciaAtivadaExp = false;
    boolean xComutadorAutomaticoExp = false;
    boolean xNecessitaHomeEixoVerticalExp = false;
    boolean xNecessitaHomeEixoGiroExp = false;
    boolean xNecessitaHomeEixoHorizontalExp = false;
    boolean xServoDesligadoEixoHorizontalExp = false;
    boolean xServoDesligadoEixoGiroExp = false;
    boolean xServoDesligadoEixoVerticalExp = false;
    boolean xCondicaoIniciarExp = false;

    private Map<String, List<String>> eventosCLP = new ConcurrentHashMap<>();

    public void chamarApis() {
        String estoqueUrl = apiUrlConfig.getEstoqueApiUrl();
        String expedicaoUrl = apiUrlConfig.getExpedicaoApiUrl();

        System.out.println("Chamando estoque em: " + estoqueUrl);
        System.out.println("Chamando expedição em: " + expedicaoUrl);
    }

    public boolean sendBlockBytesToClp(String ipClp, int db, int offset, byte[] dados, int size) {
        PlcConnector plcConnector = plcConnectionService.getConnection(ipClp);
        if (plcConnector == null) {
            System.out.println("Sem conexão com CLP " + ipClp);
            return false;
        }
        if (!readOnly) {
            try {
                plcConnector.writeBlock(db, offset, size, dados); // escreve no bloco de dados
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        // se for readOnly ou nada a fazer, considere sucesso
        return true;
    }

    //*************************************************************
    // Função para iniciar a Execução do pedido
    //*************************************************************
    public void startExecuteOrder(String ipClp) {

        // Etapas a desenvolver:
        // 1 - ATUALIZAR O PRÓXIMO NÚMERO DE PEDIDO
        // MainFrame.posExpedArray[12] = MainFrame.posExpedArray[12] + 1;
        // int orderProduction = obterProximoPedido();
        //PlcConnector plcConnector = new PlcConnector(ipClp, 102); // ajuste o IP se necessário
        if (!readOnly) {

            PlcConnector plcConnector = plcConnectionService.getConnection(ipClp);
            if (plcConnector == null) {
                return;
            }

            posicaoExpedicaoSolicitada = searchFirstPositionFreeExp();

            try {

                // Inicializa as flags da estação ESTOQUE
                //plcConnector.connect();
                plcConnector.writeBit(9, 0, 0, Boolean.parseBoolean("FALSE"));
                plcConnector.writeBit(9, 64, 0, Boolean.parseBoolean("FALSE"));
                plcConnector.writeBit(9, 64, 1, Boolean.parseBoolean("FALSE"));
                plcConnector.writeBit(9, 62, 0, Boolean.parseBoolean("FALSE"));

                // plcConnector.writeBit(9, 62, 0, Boolean.parseBoolean("FALSE"));
                // Iniciar pedido
                System.out.println("INICIAR PEDIDO 2");
                plcConnector.writeBit(9, 62, 0, Boolean.parseBoolean("TRUE"));

            } catch (Exception ex) {

            }
        }
    }

    //***************************************************************
    // Funções para gerenciamento de posições no Estoque e Expedição
    //***************************************************************
    //********************************************************************************************************************************************** */
    public int SearchFirstPositionByColor(int cor, Set<Integer> posicoesUsadas) {
        List<Estoque> estoque = estoqueRepository.findByCorOrderByPosicaoEstoque_PosicaoAsc(cor);

        for (Estoque e : estoque) {
            if (e.getPosicaoEstoque() == null || e.getPosicaoEstoque().getPosicao() == null) {
                continue;
            }
            Integer pos = e.getPosicaoEstoque().getPosicao();
            if (!posicoesUsadas.contains(pos)) {
                return pos;
            }
        }

        return -1; // Nenhuma posição disponível
    }

    public int searchFirstPositionFreeExp() {
        List<Integer> ocupadas = expedicaoRepository.findAllPosicoesOcupadas();

        for (int i = 1; i <= 12; i++) {
            if (!ocupadas.contains(i)) {
                return i;
            }
        }
        return -1;
    }

    //*************************************************************
    // Função para reiniciar status de operação das estações
    //*************************************************************
    public void resetarStatus() {
        statusEstoque = 0;
        statusProcesso = 0;
        statusMontagem = 0;
        statusExpedicao = 0;
    }

    //*************************************************************
    // Funções para gerenciamento do modo Leitura
    //*************************************************************
    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        System.out.println("readOnly: " + readOnly);
    }

}
