package com.tecdes.appsabancada.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class S7ProtocolClient_ {

    private final String plcIpAddress;
    private final int port;
    private Socket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private Object value;

    public S7ProtocolClient_(String plcIpAddress, int port) {
        this.plcIpAddress = plcIpAddress;
        this.port = port;
    }

    public boolean connect() throws Exception {
        try {
            InetAddress address = InetAddress.getByName(plcIpAddress);
            socket = new Socket(address, port);
            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();
            return true;
        } catch (IOException e) {
            throw new Exception("Falha ao conectar ao CLP: " + e.getMessage(), e);
        }
    }

    // Métodos para criar as requisições de Conexão, Comunicação, Leitura e Escrita de variáveis no CLP
    private byte[] createConnectionRequest() {
        return new byte[]{0x03, 0x00, 0x00, 0x16, 0x11, (byte) 0xE0, 0x00, 0x00, 0x00, 0x01,
            0x00, (byte) 0xC1, 0x02, 0x01, 0x00, (byte) 0xC2, 0x02, 0x01, 0x01, (byte) 0xC0, 0x01, 0x09};
    }

    private byte[] createSetupCommunication() {
        return new byte[]{0x03, 0x00, 0x00, 0x19, 0x02, (byte) 0xF0, (byte) 0x80,
            0x32, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x08, 0x00, 0x00,
            (byte) 0xF0, 0x00, 0x00, 0x01, 0x00, 0x01, 0x03, (byte) 0xC0};
    }

    public byte[] createReadRequest(int db, int offset, int bit, int size, String type) {

        // Type: str ; real; int ; byte ; bool
        byte tpSize;
        int startAddress;

        // ((offset << 3) & 0xFFF8 | (bit & 0x07))
        if ((size == 1) & (type.toLowerCase().equals("boolean"))) {

            // System.out.println("Aqui em TAG Bit 1");
            tpSize = 0x01;
            startAddress = (offset << 3) & 0xFFF8 | (bit & 0x07);
        } else {
            /// System.out.println("Aqui em TAG Bit 2");
            tpSize = 0x02;
            startAddress = offset << 3;
        }

        return new byte[]{
            // TPKT + COTP Header
            0x03, 0x00, 0x00, (byte) (31), 0x02, (byte) 0xF0, (byte) 0x80,
            // S7 Header
            0x32, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0E, 0x00, 0x00,
            // Parameter: Function Code, Item Count
            0x04, 0x01,
            // Item Header: Variable Specification, Length of Following, Syntax ID
            0x12, 0x0A, 0x10,
            // Transport Size, Parameter length, DB Number (6), Area Type (DB)
            tpSize, (byte) ((size >> 8) & 0xFF), (byte) (size & 0xFF),
            (byte) ((db >> 8) & 0xFF), (byte) (db & 0xFF), (byte) 0x84,
            // Address: Bit Address, Byte Offset (16), Reserved
            0x00, (byte) ((startAddress >> 8) & 0xFF), (byte) (startAddress & 0xFF)

        };
    }

    public byte[] createWriteRequest(int db, int offset, int bit, int size, String type, Object value) {

        // System.out.println("Aqui em createWriteRequest");
        int lenghtTag;
        if (type.toLowerCase().equals("string")) {
            lenghtTag = size + 2;
        } else {
            lenghtTag = size;
        }

        // System.out.println("\n\nValor recebido (value) + size: " + value + " - " +
        // value.length() + " - " + size);
        ByteBuffer buffer = ByteBuffer.allocate(35 + lenghtTag);
        buffer.order(ByteOrder.BIG_ENDIAN);
        // Type: str ; real; int ; byte ; bool

        // System.out.println("Buffer length: " + buffer.array().length);
        byte tpSize;
        int startAddress;

        // ((offset << 3) & 0xFFF8 | (bit & 0x07))
        if ((size == 1) & (type.toLowerCase().equals("boolean"))) {
            tpSize = 0x01;
            startAddress = (offset << 3) & 0xFFF8 | (bit & 0x07);
        } else {
            tpSize = 0x02;
            startAddress = offset << 3;
        }

        int lengthPacket = 35 + lenghtTag;
        int dataLength = 4 + size;
        // int lengthShft = size << 3;
        // byte[] data;

        // TPKT Header
        buffer.put((byte) 0x03); // Version
        buffer.put((byte) 0x00); // Reserved
        buffer.putShort((short) lengthPacket); // Length

        // COTP Header
        buffer.put((byte) 0x02); // Length
        buffer.put((byte) 0xF0); // PDU Type: DT Data
        buffer.put((byte) 0x80); // Destination reference

        // S7 Header
        buffer.put((byte) 0x32); // Protocol ID
        buffer.put((byte) 0x01); // ROSCTR: Job (1)
        buffer.putShort((short) 0x0000); // Redundancy Indentification (Reserved): 0x0000
        buffer.putShort((short) 0x0000); // Protocol Data Unit Reference: 0
        buffer.putShort((short) 0x000E); // Parameter length: 14
        buffer.putShort((short) dataLength); // Data length

        // Parameter: Function Code, Item Count
        buffer.put((byte) 0x05); // Function: Write Var (0x05)
        buffer.put((byte) 0x01); // Item count: 1

        // Item Header: Variable Specification, Length of Following, Syntax ID
        buffer.put((byte) 0x12); // Variable specification: 0x12
        buffer.put((byte) 0x0A); // Length of Following address specification: 10
        buffer.put((byte) 0x10); // Syntax ID: S7ANY (0x10)

        // Transport Size, Parameter length, DB Number (6), Area Type (DB)
        buffer.put((byte) tpSize); // Transport Size: BIT (1) BYTE (2)
        buffer.putShort((short) size); // Length
        buffer.putShort((short) db); // DB number
        buffer.put((byte) 0x84); // Area: Data blocks (DB) (0x84)

        // Address: Bit Address, Byte Offset (16), Reserved
        buffer.put((byte) ((byte) (startAddress >> 16) & 0xFF));
        buffer.put((byte) ((byte) (startAddress >> 8) & 0xFF));
        buffer.put((byte) ((byte) startAddress & 0xFF));

        // Data
        buffer.put((byte) 0x00); // Return code: Reserved (0x00)
        buffer.put((byte) (tpSize + 2)); // Transport Size: BIT (1)+2 , BYTE (2)+2

        if (type.toLowerCase().equals("boolean")) {

            //System.out.println("Aqui Boolean: " + value);
            buffer.putShort((short) lenghtTag); // Length
            buffer.put((byte) ((boolean) value ? 0x01 : 0x00));

        }

        if (type.toLowerCase().equals("integer")) {
            buffer.putShort((short) (lenghtTag << 3)); // Length
            buffer.putShort((short) ((int) value));

        }

        if (type.toLowerCase().equals("byte")) {
            buffer.putShort((short) (lenghtTag << 3)); // Length
            buffer.put((byte) ((byte) value));

        }

        if (type.toLowerCase().equals("string")) {

            // System.out.println("Aqui String: " + value);
            buffer.putShort((short) (lenghtTag << 3)); // Length
            buffer.put((byte) size);
            buffer.put((byte) ((String) value).trim().length());
            buffer.put(((String) value).getBytes(StandardCharsets.UTF_8));
            // System.out.println("Retorno de createWriteRequest: " +
            // bytesToHex(buffer.array(), buffer.array().length));

        }

        if (type.toLowerCase().equals("block")) {

            // System.out.println("Aqui String: " + value);
            buffer.putShort((short) (lenghtTag << 3)); // Length

            buffer.put((byte[]) value);
            // System.out.println("Retorno de createWriteRequest: " +
            // bytesToHex(buffer.array(), buffer.array().length));

        }

        if (type.toLowerCase().equals("float")) {

            float valFloat = (float) value;
            ByteBuffer bufferFloat = ByteBuffer.allocate(Float.BYTES); // Aloca espaço suficiente para um float
            bufferFloat.putFloat(valFloat); // Coloca o valor float no buffer
            buffer.putShort((short) (lenghtTag << 3)); // Length
            buffer.put(bufferFloat.array());

            // System.out.println("Retorno de createWriteRequest - Float: " +
            // bytesToHex(buffer.array(), buffer.array().length));
        }

        return buffer.array();
    }

    private byte[] readFullResponse() throws IOException {
        byte[] header = inputStream.readNBytes(4);
        if (header.length < 4) {
            throw new IOException("Falha ao ler cabeçalho TPKT");
        }

        int totalLength = ((header[2] & 0xFF) << 8) | (header[3] & 0xFF);
        if (totalLength < 4) {
            throw new IOException("Tamanho de pacote inválido: " + totalLength);
        }

        byte[] body = inputStream.readNBytes(totalLength - 4);
        if (body.length < totalLength - 4) {
            throw new IOException("Corpo da resposta incompleto");
        }

        byte[] full = new byte[totalLength];
        System.arraycopy(header, 0, full, 0, 4);
        System.arraycopy(body, 0, full, 4, body.length);
        return full;
    }

    // Métodos para enviar as requisições de Conexão, Comunicação, Leitura e Escrita de variáveis para o CLP
    public void sendConnectionRequest() throws Exception {
        if (outputStream == null) {
            throw new Exception("Conexão não estabelecida.");
        }
        try {
            outputStream.write(createConnectionRequest());
            outputStream.flush();
            //byte[] response = readFullResponse();
        } catch (Exception e) {
            throw new Exception("Erro ao enviar a solicitação de conexão: " + e.getMessage(), e);
        }
    }

    public void sendSetupCommunication() throws Exception {
        if (outputStream == null) {
            throw new Exception("Conexão não estabelecida.");
        }
        try {
            outputStream.write(createSetupCommunication());
            outputStream.flush();
            //byte[] response = readFullResponse();
        } catch (Exception e) {
            throw new Exception("Erro ao enviar o pacote de configuração: " + e.getMessage(), e);
        }
    }

    public Object sendReadRequest(int db, int offset, int bit, int size, String type) throws Exception {
        if (outputStream == null) {
            throw new Exception("Conexão não estabelecida.");
        }
        try {
            byte[] packet = createReadRequest(db, offset, bit, size, type);
            outputStream.write(packet);
            outputStream.flush();
            byte[] response = readFullResponse();
            switch (type.toLowerCase()) {
                case "string" ->
                    value = extractStringFromResponse(response, size);
                case "block" ->
                    value = extractBlockFromResponse(response, size);
                case "integer" ->
                    value = extractIntegerFromResponse(response);
                case "float" ->
                    value = extractFloatFromResponse(response);
                case "byte" ->
                    value = extractByteFromResponse(response);
                case "boolean" ->
                    value = extractBooleanFromResponse(response);
                default ->
                    throw new IllegalArgumentException("Tipo de variável não suportado.");
            }
            return value;
        } catch (Exception e) {
            throw new Exception("Erro ao enviar o pacote de leitura: " + e.getMessage(), e);
        }
    }

    public boolean sendWriteRequest(int db, int offset, int bit, int size, String type, Object value) throws Exception {
        if (outputStream == null) {
            throw new Exception("Conexão não estabelecida.");
        }
        try {
            byte[] packet = createWriteRequest(db, offset, bit, size, type, value);
            outputStream.write(packet);
            outputStream.flush();
            byte[] response = readFullResponse();
            return response.length >= 22 && response[21] == (byte) 0xFF;
        } catch (IOException e) {
            throw new Exception("Erro ao enviar o pacote de escrita: " + e.getMessage(), e);
        }
    }

    // Métodos para extrair as variáveis obtidas nas respostas do CLP
    private int extractIntegerFromResponse(byte[] response) {
        if (response.length < 27) {
            throw new IllegalArgumentException("Resposta muito curta para leitura de inteiro.");
        }
        return ByteBuffer.wrap(response, 25, 2).order(ByteOrder.BIG_ENDIAN).getShort();
    }

    private float extractFloatFromResponse(byte[] response) {
        if (response.length < 29) {
            throw new IllegalArgumentException("Resposta muito curta para leitura de float.");
        }
        return ByteBuffer.wrap(response, 25, 4).order(ByteOrder.BIG_ENDIAN).getFloat();
    }

    private byte extractByteFromResponse(byte[] response) {
        if (response.length < 26) {
            throw new IllegalArgumentException("Resposta muito curta para leitura de byte.");
        }
        return ByteBuffer.wrap(response, 25, 1).order(ByteOrder.BIG_ENDIAN).get();
    }

    private byte[] extractBlockFromResponse(byte[] response, int size) {
        if (response.length < 25 + size) {
            throw new IllegalArgumentException("Resposta muito curta para leitura de bloco.");
        }
        return Arrays.copyOfRange(response, 25, 25 + size);
    }

    private boolean extractBooleanFromResponse(byte[] response) {
        if (response.length < 26) {
            throw new IllegalArgumentException("Resposta muito curta para leitura de boolean.");
        }
        return (response[25] & 0x01) == 1;
    }

    private String extractStringFromResponse(byte[] response, int size) {
        if (response.length < 25 + size) {
            throw new IllegalArgumentException("Resposta muito curta para leitura de string.");
        }
        return new String(response, 25, size).trim();
    }

    public void disconnect() {
        try {
            //System.out.println("Iniciando encerramento da conexão com o CLP");
            if (outputStream != null) {
                outputStream.close();
                //System.out.println("outputStream fechado");
            }
            if (inputStream != null) {
                inputStream.close();
                //System.out.println("inputStream fechado");
            }
            if (socket != null) {
                //socket.setSoLinger(true, 0); // 🔸 Adicionado
                socket.close();
                //System.out.println("Socket fechado");
            }
            //System.out.println("Conexão com o CLP encerrada.");
        } catch (IOException e) {
            //System.err.println("Erro ao encerrar a conexão: " + e.getMessage());
        }
    }

    public static String bytesToHex(byte[] bytes, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02X ", bytes[i]));
        }
        return sb.toString().trim();
    }

}
