package practice3.udp;

import practice1.Decrypter;
import practice1.Encrypter;
import practice1.Message;
import practice1.Packet;
import practice2.warehouse.CommandType;

import javax.crypto.SecretKey;
import java.io.Closeable;
import java.io.IOException;
import java.net.*;
import java.util.concurrent.atomic.AtomicLong;

public class StoreClientUDP implements Closeable {
    private static final int TIMEOUT_MS  = 2_000;
    private static final int MAX_RETRIES = 5;
    private static final int MAX_PACKET  = 65507;

    private final int timeoutMs;
    private final int maxRetries;

    private final InetAddress serverAddress;
    private final int serverPort;
    private final Encrypter encrypter;
    private final Decrypter decrypter;
    private final DatagramSocket socket;
    private final AtomicLong packetIdCounter = new AtomicLong(0);

    public StoreClientUDP(String host, int port, SecretKey secretKey) throws Exception {
        this.serverAddress = InetAddress.getByName(host);
        this.serverPort = port;
        this.encrypter = new Encrypter(secretKey);
        this.decrypter = new Decrypter(secretKey);
        this.socket = new DatagramSocket();
        this.timeoutMs = TIMEOUT_MS;
        this.maxRetries = MAX_RETRIES;
        this.socket.setSoTimeout(this.timeoutMs);
    }

    public StoreClientUDP(String host, int port, SecretKey secretKey, int maxRetries, int timeoutMs) throws Exception {
        this.serverAddress = InetAddress.getByName(host);
        this.serverPort = port;
        this.encrypter = new Encrypter(secretKey);
        this.decrypter = new Decrypter(secretKey);
        this.socket = new DatagramSocket();
        this.maxRetries = maxRetries;
        this.timeoutMs = timeoutMs;
        this.socket.setSoTimeout(this.timeoutMs);
    }

    public String sendAndReceive(int cType, int userId, String content) throws Exception {
        long pktId = packetIdCounter.incrementAndGet();

        Message msg    = new Message(cType, userId, content);
        Packet  packet = new Packet((byte) 0x01, pktId, msg);
        byte[]  data   = encrypter.encrypt(packet);

        DatagramPacket outDatagram = new DatagramPacket(data, data.length, serverAddress, serverPort);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            System.out.printf("[UDP Client] Sending pktId=%d attempt %d/%d%n", pktId, attempt, MAX_RETRIES);
            socket.send(outDatagram);

            int staleCount = 0;

            long deadline = System.currentTimeMillis() + TIMEOUT_MS;
            while (System.currentTimeMillis() < deadline) {
                byte[] buf = new byte[MAX_PACKET];
                DatagramPacket inDataGram = new DatagramPacket(buf, buf.length);
                try {
                    socket.receive(inDataGram);
                } catch (SocketTimeoutException e) { break; }

                byte[] raw = new byte[inDataGram.getLength()];
                System.arraycopy(inDataGram.getData(), 0, raw, 0, inDataGram.getLength());

                try {
                    Packet response = decrypter.decrypt(raw);

                    if (response.getBPktId() == pktId) {
                        if (staleCount > 0)
                            System.out.printf("[UDP Client] pktId=%d: skipped %d stale packet(s) before receiving response%n", pktId, staleCount);
                        String result = response.getMessage().getMessageContent();
                        System.out.printf("[UDP Client] pktId=%d → %s%n", pktId, result);
                        return result;
                    } else {
                        staleCount++;
                        System.out.printf("[UDP Client] pktId=%d: received stale response pktId=%d (ignored, stale #%d)%n", pktId, response.getBPktId(), staleCount);
                    }

                } catch (Exception e) {
                    System.err.println("[UDP Client] Bad response packet (cannot decrypt/parse): " + e.getMessage());
                }
            }

            if (staleCount > 0)
                System.out.printf("[UDP Client] Timeout for pktId=%d on attempt %d/%d " + "(consumed %d stale packets during wait)%n", pktId, attempt, MAX_RETRIES, staleCount);
            else
                System.out.printf("[UDP Client] Timeout waiting for pktId=%d, attempt %d/%d%n", pktId, attempt, MAX_RETRIES);
        }
        throw new IOException("No response after " + MAX_RETRIES + " attempts for pktId=" + pktId);
    }

    public String sendAndReceive(CommandType command, int userId, String content) throws Exception {
        return sendAndReceive(command.getCode(), userId, content);
    }

    @Override
    public void close() {
        socket.close();
        System.out.println("[UDP Client] Closed");
    }
}