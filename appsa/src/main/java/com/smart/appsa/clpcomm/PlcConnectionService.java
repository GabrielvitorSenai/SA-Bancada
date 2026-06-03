package com.smart.appsa.clpcomm;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class PlcConnectionService {
    private static final Map<String, PlcConnector> conexoes = new ConcurrentHashMap<>();

    public synchronized PlcConnector getConnection(String ip) {
        // lógica de obter/abrir conexão
        PlcConnector connector = conexoes.get(ip);
            if (connector == null) {
                System.out.println("=============================");
                System.out.println("NOVA CONEXÃO COM O CLP: " + ip);
                System.out.println("=============================");
                connector = new PlcConnector(ip, 102);
                try {
                    connector.connect();
                    conexoes.put(ip, connector);
                } catch (Exception e) {
                    System.err.println("Erro ao conectar ao CLP " + ip);
                    e.printStackTrace();
                    return null;
                }
            }
            return connector;
    }

    public synchronized void disconnect(String ip) { 
        PlcConnector connector = conexoes.get(ip);
            if (connector != null) {
                try {
                    connector.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                conexoes.remove(ip);
            } 
    }

    public void closeAll() { 
        System.out.println("=============================");
            System.out.println("ENCERRAR CONEXÕES COM OS CLPs");
            System.out.println("=============================");
            for (Map.Entry<String, PlcConnector> entry : conexoes.entrySet()) {
                String ip = entry.getKey();
                PlcConnector connector = entry.getValue();
                try {
                    if (connector != null) {
                        connector.disconnect();
                        //System.out.println("Conexão com " + ip + " encerrada com sucesso.");
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao encerrar conexão com " + ip + ": " + e.getMessage());
                }
            }
            conexoes.clear();
    }

}
