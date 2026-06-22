package com.tecdes.appsabancada.controller;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.tecdes.appsabancada.clpcomm.PlcConnectionService;
import com.tecdes.appsabancada.clpcomm.PlcConnector;
import com.tecdes.appsabancada.clpcomm.PlcReaderDB;
import com.tecdes.appsabancada.clpcomm.PlcReaderMultDB;
import com.tecdes.appsabancada.service.ClpSyncService;
import com.tecdes.appsabancada.service.EstoqueClpService;
import com.tecdes.appsabancada.service.ExpedicaoClpService;
import com.tecdes.appsabancada.service.MonitorService;
import com.tecdes.appsabancada.service.MontagemClpService;
import com.tecdes.appsabancada.service.ProcessoClpService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/clp")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ClpController {

    private final PlcConnectionService plcConnectionService;
    private final MonitorService monitorService;
    private final ClpSyncService clpSyncService;
    private final EstoqueClpService estoqueService;
    private final ProcessoClpService processoService;
    private final MontagemClpService montagemService;
    private final ExpedicaoClpService expedicaoService;

    private final Map<String, String> readingsCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService readingExecutor = Executors.newScheduledThreadPool(4);
    private final Map<String, ScheduledFuture<?>> readingFutures = new ConcurrentHashMap<>();

    private static byte[] dataClp1;
    private static byte[] dataClp2;
    private static byte[] dataClp3;
    private static byte[] dataClp4;

    @PostMapping("/sync-estoque")
    public ResponseEntity<Map<String, Object>> syncEstoque(@RequestBody(required = false) Map<String, String> body) {
        String ip = body != null ? body.get("estoque") : null;
        return ResponseEntity.ok(clpSyncService.sincronizarEstoque(ip));
    }

    @PostMapping("/sync-expedicao")
    public ResponseEntity<Map<String, Object>> syncExpedicao(@RequestBody(required = false) Map<String, String> body) {
        String ip = body != null ? body.get("expedicao") : null;
        return ResponseEntity.ok(clpSyncService.sincronizarExpedicao(ip));
    }

    @PostMapping("/sync-all")
    public ResponseEntity<Map<String, Object>> syncAll(@RequestBody(required = false) Map<String, String> ips) {
        return ResponseEntity.ok(clpSyncService.sincronizarTudo(ips));
    }

    @PostMapping("/start-readings")
    public ResponseEntity<Map<String, Object>> startReadings(@RequestBody Map<String, String> ips) {
        Map<String, Object> resposta = new HashMap<>();
        Map<String, Object> detalhes = new HashMap<>();

        if (ips == null || ips.isEmpty()) {
            resposta.put("sucesso", false);
            resposta.put("mensagem", "Nenhum IP de CLP foi informado.");
            resposta.put("timestamp", LocalDateTime.now().toString());
            return ResponseEntity.badRequest().body(resposta);
        }

        ips.forEach((nome, ip) -> {
            Map<String, Object> item = new HashMap<>();

            try {
                if (nome == null || ip == null || ip.isBlank()) {
                    item.put("conectado", false);
                    item.put("mensagem", "Nome ou IP inválido.");
                    detalhes.put(String.valueOf(nome), item);
                    return;
                }

                String nomeNormalizado = nome.trim().toLowerCase();
                String ipNormalizado = ip.trim();

                if (readingFutures.containsKey(nomeNormalizado)) {
                    item.put("conectado", true);
                    item.put("mensagem", "Leitura já estava ativa.");
                    item.put("ip", ipNormalizado);
                    detalhes.put(nomeNormalizado, item);
                    return;
                }

                PlcConnector plcConnector = plcConnectionService.getConnection(ipNormalizado);

                if (plcConnector == null) {
                    item.put("conectado", false);
                    item.put("mensagem", "Não foi possível conectar ao CLP.");
                    item.put("ip", ipNormalizado);
                    detalhes.put(nomeNormalizado, item);
                    return;
                }

                Runnable task = criarTaskLeitura(nomeNormalizado, ipNormalizado, plcConnector);

                if (task == null) {
                    item.put("conectado", false);
                    item.put("mensagem", "Nome de CLP inválido: " + nomeNormalizado);
                    item.put("ip", ipNormalizado);
                    detalhes.put(nomeNormalizado, item);
                    return;
                }

                long delayMs = delayPorBancada(nomeNormalizado);

                ScheduledFuture<?> future = readingExecutor.scheduleWithFixedDelay(
                        task,
                        0,
                        delayMs,
                        TimeUnit.MILLISECONDS
                );

                readingFutures.put(nomeNormalizado, future);

                item.put("conectado", true);
                item.put("mensagem", "Leitura iniciada.");
                item.put("ip", ipNormalizado);
                item.put("intervaloMs", delayMs);
                detalhes.put(nomeNormalizado, item);

            } catch (Exception e) {
                item.put("conectado", false);
                item.put("mensagem", e.getMessage());
                detalhes.put(String.valueOf(nome), item);
                e.printStackTrace();
            }
        });

        boolean peloMenosUmConectado = detalhes.values()
                .stream()
                .anyMatch(v -> v instanceof Map<?, ?> map && Boolean.TRUE.equals(map.get("conectado")));

        resposta.put("sucesso", peloMenosUmConectado);
        resposta.put("mensagem", peloMenosUmConectado
                ? "Leitura iniciada para pelo menos um CLP."
                : "Nenhum CLP foi conectado.");
        resposta.put("detalhes", detalhes);
        resposta.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(resposta);
    }

    private Runnable criarTaskLeitura(String nome, String ip, PlcConnector plcConnector) {
        return switch (nome) {
            case "estoque" -> new PlcReaderMultDB(
                    plcConnector,
                    nome,
                    new PlcReaderMultDB.PlcReadRequest(9, 0, 111),
                    new PlcReaderMultDB.PlcReadRequest(6, 0, 60),
                    new PlcReaderMultDB.PlcReadRequest(0, 0, 0),
                    new PlcReaderMultDB.PlcReadRequest(0, 0, 0),
                    new PlcReaderMultDB.PlcReadRequest(0, 0, 0),
                    dados -> {
                        dataClp1 = dados;
                        estoqueService.processData(ip, dados);
                        updateCache("estoque", dados);
                    }
            );

            case "processo" -> new PlcReaderDB(
                    plcConnector,
                    nome,
                    2,
                    0,
                    9,
                    dados -> {
                        dataClp2 = dados;
                        processoService.processData(ip, dados);
                        updateCache("processo", dados);
                    }
            );

            case "montagem" -> new PlcReaderMultDB(
                    plcConnector,
                    nome,
                    new PlcReaderMultDB.PlcReadRequest(57, 0, 9),
                    new PlcReaderMultDB.PlcReadRequest(30, 16, 16),
                    new PlcReaderMultDB.PlcReadRequest(600, 14, 16),
                    new PlcReaderMultDB.PlcReadRequest(92, 2, 16),
                    new PlcReaderMultDB.PlcReadRequest(60, 20, 16),
                    dados -> {
                        dataClp3 = dados;
                        montagemService.processData(ip, dados);
                        updateCache("montagem", dados);
                    }
            );

            case "expedicao" -> new PlcReaderDB(
                    plcConnector,
                    nome,
                    9,
                    0,
                    48,
                    dados -> {
                        dataClp4 = dados;
                        expedicaoService.processData(ip, dados);
                        updateCache("expedicao", dados);
                    }
            );

            default -> null;
        };
    }

    private long delayPorBancada(String nome) {
        return switch (nome) {
            case "processo", "montagem" -> 400;
            case "estoque", "expedicao" -> 600;
            default -> 600;
        };
    }

    private void updateCache(String nome, byte[] dados) {
        readingsCache.put(nome, bytesParaHex(dados));
    }

    @GetMapping("/data/{clp}")
    public ResponseEntity<String> getData(@PathVariable String clp) {
        byte[] dados = obterDadosPorNome(clp);

        if (dados == null) {
            return ResponseEntity.ok("Ainda não há dados para " + clp);
        }

        return ResponseEntity.ok(bytesParaHex(dados));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> resposta = new HashMap<>();

        resposta.put("pedidoEmCurso", MonitorService.pedidoEmCurso);
        resposta.put("pedidoFinalizado", MonitorService.blockFinished);

        resposta.put("statusEstoque", MonitorService.statusEstoque);
        resposta.put("statusProcesso", MonitorService.statusProcesso);
        resposta.put("statusMontagem", MonitorService.statusMontagem);
        resposta.put("statusExpedicao", MonitorService.statusExpedicao);
        resposta.put("statusProducao", MonitorService.statusProducao);

        resposta.put("estoqueConectado", dataClp1 != null || readingFutures.containsKey("estoque"));
resposta.put("processoConectado", dataClp2 != null || readingFutures.containsKey("processo"));
resposta.put("montagemConectado", dataClp3 != null || readingFutures.containsKey("montagem"));
resposta.put("expedicaoConectado", dataClp4 != null || readingFutures.containsKey("expedicao"));
        resposta.put("leiturasAtivas", readingFutures.keySet());
        resposta.put("cache", readingsCache);
        resposta.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(resposta);
    }

    @PostMapping("/stop-readings")
    public ResponseEntity<Map<String, Object>> stopReadings() {
        readingFutures.forEach((nome, future) -> {
            future.cancel(true);
            System.out.println("Thread de leitura '" + nome + "' cancelada.");
        });

        readingFutures.clear();

        dataClp1 = null;
        dataClp2 = null;
        dataClp3 = null;
        dataClp4 = null;

        readingsCache.clear();

        plcConnectionService.closeAll();

        Map<String, Object> resposta = new HashMap<>();
        resposta.put("sucesso", true);
        resposta.put("mensagem", "Leituras interrompidas e conexões fechadas.");
        resposta.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(resposta);
    }

    @GetMapping("/smartstream/{bancada}")
    public SseEmitter smartStream(@PathVariable String bancada) {
        SseEmitter emitter = new SseEmitter(0L);
        ExecutorService sseExecutor = Executors.newSingleThreadExecutor();

        sseExecutor.execute(() -> {
            try {
                while (true) {
                    byte[] dados = montarFrameStream(bancada);

                    if (dados != null) {
                        emitter.send(SseEmitter.event()
                                .name("leitura")
                                .data(bytesParaHex(dados)));
                    } else {
                        emitter.send(SseEmitter.event()
                                .name("aguardando")
                                .data("Aguardando dados de " + bancada));
                    }

                    TimeUnit.MILLISECONDS.sleep(400);
                }
            } catch (IOException | IllegalStateException ex) {
                emitter.complete();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                emitter.completeWithError(e);
            } catch (Exception e) {
                emitter.completeWithError(e);
            } finally {
                sseExecutor.shutdown();
            }
        });

        emitter.onCompletion(sseExecutor::shutdownNow);
        emitter.onTimeout(() -> {
            emitter.complete();
            sseExecutor.shutdownNow();
        });
        emitter.onError(ex -> {
            emitter.completeWithError(ex);
            sseExecutor.shutdownNow();
        });

        return emitter;
    }

    private byte[] montarFrameStream(String bancada) {
        if (bancada == null) {
            return null;
        }

        return switch (bancada.toLowerCase()) {
            case "estoque" -> montarFrameEstoqueComStatus();
            case "processo" -> dataClp2;
            case "montagem" -> dataClp3;
            case "expedicao" -> dataClp4;
            default -> null;
        };
    }

    private byte[] montarFrameEstoqueComStatus() {
        if (dataClp1 == null) {
            return null;
        }

        byte[] extendidoEst = new byte[dataClp1.length + 6];

        System.arraycopy(dataClp1, 0, extendidoEst, 0, dataClp1.length);

        extendidoEst[extendidoEst.length - 6] = MonitorService.statusEstoque;
        extendidoEst[extendidoEst.length - 5] = MonitorService.statusProcesso;
        extendidoEst[extendidoEst.length - 4] = MonitorService.statusMontagem;
        extendidoEst[extendidoEst.length - 3] = MonitorService.statusExpedicao;
        extendidoEst[extendidoEst.length - 2] = MonitorService.statusProducao;
        extendidoEst[extendidoEst.length - 1] = (byte) (MonitorService.pedidoEmCurso ? 1 : 0);

        return extendidoEst;
    }

    private byte[] obterDadosPorNome(String clp) {
        if (clp == null) {
            return null;
        }

        return switch (clp.toLowerCase()) {
            case "estoque", "clp1" -> dataClp1;
            case "processo", "clp2" -> dataClp2;
            case "montagem", "clp3" -> dataClp3;
            case "expedicao", "clp4" -> dataClp4;
            default -> null;
        };
    }

    private String bytesParaHex(byte[] dados) {
        if (dados == null) {
            return "";
        }

        StringBuilder hexBuilder = new StringBuilder();

        for (byte b : dados) {
            hexBuilder.append(String.format("%02X ", b));
        }

        return hexBuilder.toString().trim();
    }

    @PostMapping("/smart/ping")
    public ResponseEntity<Map<String, Boolean>> pingHosts(@RequestBody Map<String, String> ips) {
        Map<String, Boolean> resultados = new HashMap<>();

        if (ips == null || ips.isEmpty()) {
            return ResponseEntity.ok(resultados);
        }

        ips.forEach((nome, ip) -> {
            boolean online = false;

            if (ip != null && !ip.isBlank()) {
                try (Socket socket = new Socket()) {
                    SocketAddress address = new InetSocketAddress(ip.trim(), 102);
                    socket.connect(address, 2000);
                    online = true;
                } catch (IOException e) {
                    online = false;
                }
            }

            resultados.put(nome, online);
        });

        return ResponseEntity.ok(resultados);
    }

    @PostMapping("/smart/reset-status")
    public ResponseEntity<String> resetarStatus() {
        monitorService.resetarStatus();
        return ResponseEntity.ok("Status zerados com sucesso.");
    }

    @PostMapping("/smart/readonly")
    public ResponseEntity<String> setReadOnly(@RequestParam boolean value) {
        monitorService.setReadOnly(value);
        return ResponseEntity.ok("Modo readOnly: " + value);
    }

    @GetMapping("/smart/readonly")
    public ResponseEntity<Boolean> getReadOnly() {
        return ResponseEntity.ok(monitorService.isReadOnly());
    }
}
