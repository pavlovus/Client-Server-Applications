package practice3.udp;

import practice1.Decrypter;
import practice1.Encrypter;
import practice1.Message;
import practice1.Packet;
import practice2.warehouse.CommandHandler;
import practice4.ProductService;

import javax.crypto.SecretKey;
import java.net.*;
import java.util.concurrent.*;

public class StoreServerUDP {
    private static final int MAX_PACKET_SIZE = 65507;

    private final int port;
    private final ExecutorService workerPool;

    private final Decrypter decrypter;
    private final Encrypter encrypter;
    private final CommandHandler commandHandler;

    private volatile DatagramSocket socket;
    private volatile boolean running = false;

    public StoreServerUDP(int port, SecretKey secretKey, ProductService productService) {
        this.port = port;
        this.decrypter = new Decrypter(secretKey);
        this.encrypter = new Encrypter(secretKey);
        this.commandHandler = new CommandHandler(productService);
        this.workerPool = Executors.newCachedThreadPool(r -> {Thread t = new Thread(r, "udp-worker");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() throws SocketException {
        socket  = new DatagramSocket(port);
        running = true;
        System.out.println("[UDP Server] Listening on port " + port);

        byte[] buffer = new byte[MAX_PACKET_SIZE];

        while (running) {
            DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(datagram);

                byte[] data = new byte[datagram.getLength()];
                System.arraycopy(datagram.getData(), datagram.getOffset(), data, 0, datagram.getLength());

                InetAddress clientAddress = datagram.getAddress();
                int clientPort = datagram.getPort();

                workerPool.submit(() -> handle(data, clientAddress, clientPort));
            } catch (SocketException e) {
                if (running) System.err.println("[UDP Server] Socket error: " + e.getMessage());
            } catch (Exception e) { System.err.println("[UDP Server] Receive error: " + e.getMessage()); }
        }
    }

    public void stop() {
        running = false;
        if (socket != null) socket.close();
        workerPool.shutdownNow();
        System.out.println("[UDP Server] Stopped");
    }

    private void handle(byte[] data, InetAddress clientAddress, int clientPort) {
        try {
            Packet request = decrypter.decrypt(data);
            String result = commandHandler.handle(request.getMessage());
            Message responseMsg = new Message(request.getMessage().getCType(), request.getMessage().getBUserId(), result);
            Packet responsePacket = new Packet(request.getBSrc(), request.getBPktId(), responseMsg);
            byte[] responseBytes = encrypter.encrypt(responsePacket);

            DatagramPacket response = new DatagramPacket( responseBytes, responseBytes.length, clientAddress, clientPort );

            synchronized (socket) { socket.send(response); }
            System.out.printf("[UDP Server] Handled pktId=%d from %s:%d → %s%n", request.getBPktId(), clientAddress.getHostAddress(), clientPort, result);
        } catch (Exception e) {
            System.err.println("[UDP Server] Error handling datagram from " + clientAddress + ":" + clientPort + " — " + e.getMessage());
        }
    }
}