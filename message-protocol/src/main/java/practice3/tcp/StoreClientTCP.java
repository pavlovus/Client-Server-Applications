package practice3.tcp;

import practice1.Decrypter;
import practice1.Encrypter;
import practice1.Message;
import practice1.Packet;
import practice2.warehouse.CommandType;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicLong;

public class StoreClientTCP implements Closeable {
    private static final int INITIAL_RETRY_DELAY_MS = 500;
    private static final int MAX_RETRY_DELAY_MS = 16_000;
    private static final int CONNECT_TIMEOUT_MS = 3_000;
    private static final int READ_TIMEOUT_MS = 10_000;

    private final String host;
    private final int port;
    private final Encrypter encrypter;
    private final Decrypter decrypter;
    private final AtomicLong packetIdCounter = new AtomicLong(0);

    private final Object ioLock = new Object();

    private Socket socket;
    private DataInputStream  in;
    private DataOutputStream out;
    private volatile boolean closed = false;

    public StoreClientTCP(String host, int port, SecretKey secretKey) {
        this.host = host;
        this.port = port;
        this.encrypter = new Encrypter(secretKey);
        this.decrypter = new Decrypter(secretKey);
    }

    public String sendAndReceive(int cType, int userId, String content) throws InterruptedException, IOException {
        if (closed) throw new IllegalStateException("Client is closed");

        Message msg = new Message(cType, userId, content);
        long pktId  = packetIdCounter.incrementAndGet();
        Packet packet = new Packet((byte) 0x01, pktId, msg);

        ensureConnected();

        if (closed) throw new IllegalStateException("Client was closed during connect");

        synchronized (ioLock) {
            if (!isConnected())
                throw new IOException("Connection lost before send for pktId=" + pktId + ". Retry explicitly if the operation is idempotent.");
            try {
                byte[] encoded = encrypter.encrypt(packet);

                out.writeInt(encoded.length);
                out.write(encoded);
                out.flush();

                int    respLen   = in.readInt();
                byte[] respBytes = new byte[respLen];
                in.readFully(respBytes);

                Packet response = decrypter.decrypt(respBytes);
                return response.getMessage().getMessageContent();

            } catch (Exception e) {
                System.err.println("[TCP Client] I/O error for pktId=" + pktId + ": " + e.getMessage() + " — disconnecting");
                disconnect();
                throw new IOException("Send/receive failed for pktId=" + pktId + ". Server may have processed the request. " + "Retry explicitly if the operation is idempotent.", e);
            }
        }
    }

    public String sendAndReceive(CommandType command, int userId, String content) throws InterruptedException, IOException {
        return sendAndReceive(command.getCode(), userId, content);
    }

    @Override
    public void close() {
        closed = true;
        synchronized (ioLock) { disconnect(); }
        System.out.println("[TCP Client] Closed");
    }

    public boolean isConnected() { return socket != null && socket.isConnected() && !socket.isClosed(); }

    private void ensureConnected() throws InterruptedException {
        if (isConnected()) return;

        int delay = INITIAL_RETRY_DELAY_MS;
        while (!closed) {
            try {
                Socket newSocket = new Socket();
                newSocket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
                newSocket.setSoTimeout(READ_TIMEOUT_MS);

                DataInputStream  newIn  = new DataInputStream(new BufferedInputStream(newSocket.getInputStream()));
                DataOutputStream newOut = new DataOutputStream(new BufferedOutputStream(newSocket.getOutputStream()));

                synchronized (ioLock) {
                    disconnectInternal();
                    socket = newSocket;
                    in     = newIn;
                    out    = newOut;
                }
                System.out.println("[TCP Client] Connected to " + host + ":" + port);
                return;

            } catch (IOException e) {
                System.err.println("[TCP Client] Cannot connect (" + e.getMessage()  + "), retrying in " + delay + " ms…");
                Thread.sleep(delay);
                delay = Math.min(delay * 2, MAX_RETRY_DELAY_MS);
            }
        }
        throw new IllegalStateException("Client closed while trying to connect");
    }

    private void disconnect() { disconnectInternal(); }

    private void disconnectInternal() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) { System.err.println("[TCP Client] Error closing socket: " + e.getMessage()); }
        socket = null;
        in     = null;
        out    = null;
    }
}