package com.tecdes.appsabancada.controller;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.tecdes.appsabancada.service.PlcConnector;
import com.tecdes.appsabancada.service.PlcReaderDB;
import com.tecdes.appsabancada.service.PlcReaderMultDB;
import com.tecdes.appsabancada.service.SmartService;
import com.tecdes.appsabancada.service.SmartService.PlcConnectionManager;

@RestController
public class ClpController {

    private final Map<String, String> leiturasCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService leituraExecutor = Executors.newScheduledThreadPool(4);
    private final Map<String, ScheduledFuture<?>> leituraFutures = new ConcurrentHashMap<>();

    //private static byte[] dadosClp1 = new byte[0];
    private static byte[] dadosClp1;
    private static byte[] dadosClp2;
    private static byte[] dadosClp3;
    private static byte[] dadosClp4;

    @Autowired
    private SmartService smartService;

    @PostMapping("/start-leituras")
    public ResponseEntity<String> startLeituras(@RequestBody Map<String, String> ips) {
        ips.forEach((nome, ip) -> {
            if (!leituraFutures.containsKey(nome)) {
                PlcConnector plcConnector = PlcConnectionManager.getConexao(ip);
                if (plcConnector == null) {
                    System.err.println("Erro ao obter conexão com o CLP: " + ip);
                    return; // ignora esse CLP e continua com os demais
                }

                PlcReaderDB task = null;
                PlcReaderMultDB taskMult = null;
                long delayMs = 600; // valor padrão

                switch (nome.toLowerCase()) {
                    case "estoque" -> {
                        taskMult = new PlcReaderMultDB(
                                plcConnector,
                                nome,
                                new PlcReaderMultDB.PlcReadRequest(9, 0, 111),
                                new PlcReaderMultDB.PlcReadRequest(6, 0, 60),
                                new PlcReaderMultDB.PlcReadRequest(0, 0, 0),
                                new PlcReaderMultDB.PlcReadRequest(0, 0, 0), // Ignorado (db == 0)
                                new PlcReaderMultDB.PlcReadRequest(0, 0, 0),
                                dados -> {
                                    ClpController.dadosClp1 = dados;
                                    smartService.clpEstoque(ip, dados);
                                    atualizarCache("estoque", dados);
                                });
                        delayMs = 600; // Delay personalizado para ESTOQUE
                    }

                    case "processo" -> {
                        task = new PlcReaderDB(plcConnector, nome, 2, 0, 9, dados -> {
                            ClpController.dadosClp2 = dados;
                            smartService.clpProcesso(ip, dados);
                            atualizarCache("processo", dados);
                        });
                        delayMs = 400;
                    }

                    case "montagem" -> {
                        taskMult = new PlcReaderMultDB(
                                plcConnector,
                                nome,
                                new PlcReaderMultDB.PlcReadRequest(57, 0, 9),
                                new PlcReaderMultDB.PlcReadRequest(30, 16, 16),
                                new PlcReaderMultDB.PlcReadRequest(600, 14, 16),
                                new PlcReaderMultDB.PlcReadRequest(92, 2, 16), // Ignorado (db == 0)
                                new PlcReaderMultDB.PlcReadRequest(60, 20, 16),
                                dados -> {
                                    ClpController.dadosClp3 = dados;
                                    smartService.clpMontagem(ip, dados);
                                    atualizarCache("montagem", dados);
                                });
                        delayMs = 400;
                    }

                    case "expedicao" -> {
                        task = new PlcReaderDB(plcConnector, nome, 9, 0, 48, dados -> {
                            ClpController.dadosClp4 = dados;
                            smartService.clpExpedicao(ip, dados);
                            atualizarCache("expedicao", dados);
                        });
                        delayMs = 600;
                    }

                    default -> {
                        System.err.println("Nome de CLP inválido: " + nome);
                        return;
                    }
                }

                // if (task != null) {
                //     // A leitura só será iniciada novamente após o término do processamento + delayMs
                //     ScheduledFuture<?> future = leituraExecutor.scheduleWithFixedDelay(
                //             task, 0, delayMs, TimeUnit.MILLISECONDS
                //     );
                //     leituraFutures.put(nome, future);
                // }
                Runnable toSchedule = task != null ? task : taskMult;
                if (toSchedule != null) {
                    ScheduledFuture<?> future = leituraExecutor.scheduleWithFixedDelay(
                            toSchedule, 0, delayMs, TimeUnit.MILLISECONDS
                    );
                    leituraFutures.put(nome, future);
                }
            }
        });

        return ResponseEntity.ok("Leituras com PlcReaderTask iniciadas.");
    }

    private void atualizarCache(String nome, byte[] dados) {
        StringBuilder sb = new StringBuilder();
        for (byte b : dados) {
            sb.append(String.format("%02X ", b));
        }
        leiturasCache.put(nome, sb.toString().trim());
    }

    @GetMapping("/dados/{clp}")
    public ResponseEntity<String> getDados(@PathVariable String clp) {
        byte[] dados = switch (clp.toLowerCase()) {
            case "clp1" ->
                dadosClp1;
            case "clp2" ->
                dadosClp2;
            case "clp3" ->
                dadosClp3;
            case "clp4" ->
                dadosClp4;
            default ->
                null;
        };

        if (dados == null) {
            return ResponseEntity.ok("Ainda não há dados para " + clp);
        }

        StringBuilder builder = new StringBuilder();
        for (byte b : dados) {
            builder.append(String.format("%02X ", b));
        }

        return ResponseEntity.ok(builder.toString().trim());
    }

    @PostMapping("/stop-leituras")
    public ResponseEntity<String> stopLeituras() {
        leituraFutures.forEach((nome, future) -> {
            future.cancel(true);
            System.out.println("Thread de leitura '" + nome + "' cancelada.");
        });
        leituraFutures.clear();
        PlcConnectionManager.encerrarTodasAsConexoes();

        // Salvar os eventos acumulados
        //smartService.salvarEventosEmArquivo();
        return ResponseEntity.ok("Leituras interrompidas e eventos registrados.");
    }

    @GetMapping("/smartstream/{bancada}")
    public SseEmitter smartStream(@PathVariable String bancada) {
        SseEmitter emitter = new SseEmitter(0L);
        ExecutorService sseExecutor = Executors.newSingleThreadExecutor();

        sseExecutor.execute(() -> {
            try {
                while (true) {
                    byte[] dados = switch (bancada.toLowerCase()) {
                        case "estoque" -> {
                            // Adiciona dois bytes ao final
                            byte[] extendidoEst = new byte[dadosClp1.length + 6];
                            System.arraycopy(dadosClp1, 0, extendidoEst, 0, dadosClp1.length);
                            extendidoEst[extendidoEst.length - 6] = SmartService.statusEstoque;
                            extendidoEst[extendidoEst.length - 5] = SmartService.statusProcesso;
                            extendidoEst[extendidoEst.length - 4] = SmartService.statusMontagem;
                            extendidoEst[extendidoEst.length - 3] = SmartService.statusExpedicao;
                            extendidoEst[extendidoEst.length - 2] = SmartService.statusProducao;
                            extendidoEst[extendidoEst.length - 1] = (byte) (SmartService.pedidoEmCurso ? 1 : 0);
                            yield extendidoEst;
                        }
                        case "processo" ->
                            dadosClp2;
                        case "montagem" ->
                            dadosClp3;
                        case "expedicao" ->
                            dadosClp4;
                        default ->
                            null;
                    };

                    if (dados != null) {
                        StringBuilder hexBuilder = new StringBuilder();
                        for (byte b : dados) {
                            hexBuilder.append(String.format("%02X ", b));
                        }

                        String leituraHex = hexBuilder.toString().trim();

                        try {
                            emitter.send(SseEmitter.event().name("leitura").data(leituraHex));
                        } catch (IOException | IllegalStateException ex) {
                            emitter.complete();
                            break;
                        }
                    }

                    TimeUnit.MILLISECONDS.sleep(400);
                }
            } catch (InterruptedException e) {
                emitter.completeWithError(e);
                Thread.currentThread().interrupt();
            }
        });

        emitter.onCompletion(() -> System.out.println("SSE finalizado para " + bancada));
        emitter.onTimeout(emitter::complete);
        emitter.onError(emitter::completeWithError);

        return emitter;
    }

    @PostMapping("/smart/ping")
    public Map<String, Boolean> pingHosts(@RequestBody Map<String, String> ips) {
        Map<String, Boolean> resultados = new HashMap<>();

        ips.forEach((nome, ip) -> {
            boolean isClpOnline = false;

            try (Socket socket = new Socket()) {
                SocketAddress address = new InetSocketAddress(ip, 102);
                socket.connect(address, 2000); // timeout 2 segundos

                // Se conectou, consideramos que é um CLP Siemens
                isClpOnline = true;
            } catch (IOException e) {
                // Porta 102 não está aberta, ou não é um CLP válido
                isClpOnline = false;
            }

            System.out.println(nome + ": " + isClpOnline);

            resultados.put(nome, isClpOnline);
        });

        return resultados;
    }

    @PostMapping("/smart/reset-status")
    public ResponseEntity<String> resetarStatus() {
        smartService.resetarStatus();
        return ResponseEntity.ok("Status zerados com sucesso.");
    }
    
    @PostMapping("/smart/readonly")
    public ResponseEntity<String> setReadOnly(@RequestParam boolean value) {
        smartService.setReadOnly(value);
        return ResponseEntity.ok("Modo readOnly: " + value);
    }

    @GetMapping("/smart/readonly")
    public boolean getReadOnly() {
        return smartService.isReadOnly();
    }

}
