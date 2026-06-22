package com.tecdes.appsabancada.service;

import java.util.function.Consumer;

public class PlcReaderDB implements Runnable {

    // Leitura de um Data Block em array único

    private final PlcConnector plcConnector;
    private final String nomeClp;
    private final int db;
    private final int offset;
    private final int size;
    private final Consumer<byte[]> onDataRead;

    public PlcReaderDB(PlcConnector plcConnector, String nomeClp, int db, int offset, int size, Consumer<byte[]> onDataRead) {
        this.plcConnector = plcConnector;
        this.nomeClp = nomeClp;
        this.db = db;
        this.offset = offset;
        this.size = size;
        this.onDataRead = onDataRead;
    }

    @Override
    public void run() {
        try {
            byte[] data = plcConnector.readBlock(db, offset, size);
            onDataRead.accept(data);
        } catch (Exception e) {
            System.err.println("Erro ao ler CLP " + nomeClp + ": " + e.getMessage());
        }
    }
}