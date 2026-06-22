package com.tecdes.appsabancada.service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PlcReaderMultDB implements Runnable {
    // Leitura agrupada de Data Blocks em um array consolidado
    private final PlcConnector plcConnector;
    private final String nomeClp;
    private final List<PlcReadRequest> readRequests;
    private final Consumer<byte[]> onDataRead;

    public PlcReaderMultDB(PlcConnector plcConnector, String nomeClp,
            PlcReadRequest r1, PlcReadRequest r2, PlcReadRequest r3,
            PlcReadRequest r4, PlcReadRequest r5,
            Consumer<byte[]> onDataRead) {

        this.plcConnector = plcConnector;
        this.nomeClp = nomeClp;
        this.onDataRead = onDataRead;
        this.readRequests = new ArrayList<>();

        // Mantém apenas solicitações válidas de leitura
        for (PlcReadRequest r : List.of(r1, r2, r3, r4, r5)) {
            if (r != null && r.db != 0) {
                this.readRequests.add(r);
            }
        }
    }

    @Override
    public void run() {
        try {
            List<byte[]> blocosLidos = new ArrayList<>();
            int totalLength = 0;

            for (PlcReadRequest r : readRequests) {
                byte[] bloco = plcConnector.readBlock(r.db, r.offset, r.size);
                blocosLidos.add(bloco);
                totalLength += bloco.length;
            }

            // Consolida os blocos lidos em um único retorno
            byte[] resultado = new byte[totalLength];
            int pos = 0;
            for (byte[] bloco : blocosLidos) {
                System.arraycopy(bloco, 0, resultado, pos, bloco.length);
                pos += bloco.length;
            }

            onDataRead.accept(resultado);

        } catch (Exception e) {
            System.err.println("Erro ao ler CLP " + nomeClp + ": " + e.getMessage());
        }
    }

    // Objeto auxiliar para descrever uma leitura de DB
    public static class PlcReadRequest {

        public final int db;
        public final int offset;
        public final int size;

        public PlcReadRequest(int db, int offset, int size) {
            this.db = db;
            this.offset = offset;
            this.size = size;
        }
    }
}
