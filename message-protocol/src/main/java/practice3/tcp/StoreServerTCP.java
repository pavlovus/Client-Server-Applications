package practice3.tcp;

import practice1.Decrypter;
import practice1.Encrypter;
import practice1.Packet;
import practice2.warehouse.CommandHandler;
import practice1.Message;
import practice4.ProductService;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class StoreServerTCP {
    private static final int BUFFER_SIZE = 65536;

    private final int port;
    private final SecretKey secretKey;
    private final ProductService productService;
    private final ExecutorService clientPool;

    private volatile ServerSocket serverSocket;
    private volatile boolean running = false;

    public StoreServerTCP(int port, SecretKey secretKey, ProductService productService) {
        this.port = port;
        this.secretKey = secretKey;
        this.productService = productService;
        this.clientPool = Executors.newCachedThreadPool(r -> { Thread t = new Thread(r, "tcp-client-handler"); t.setDaemon(true); return t; });
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        System.out.println("[TCP Server] Listening on port " + port);

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                clientSocket.setSoTimeout(30_000);
                System.out.println("[TCP Server] Client connected: " + clientSocket.getRemoteSocketAddress());
                clientPool.submit(new ClientHandler(clientSocket, secretKey, productService));
            } catch (SocketException e) { if (running) System.err.println("[TCP Server] Accept error: " + e.getMessage()); }
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) { System.err.println("[TCP Server] Error closing server socket: " + e.getMessage()); }
        clientPool.shutdownNow();
        System.out.println("[TCP Server] Stopped");
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private final Decrypter decrypter;
        private final Encrypter encrypter;
        private final CommandHandler commandHandler;

        ClientHandler(Socket socket, SecretKey secretKey, ProductService productService) {
            this.socket = socket;
            this.decrypter = new Decrypter(secretKey);
            this.encrypter = new Encrypter(secretKey);
            this.commandHandler = new CommandHandler(productService);
        }

        @Override
        public void run() {
            String clientAddress = socket.getRemoteSocketAddress().toString();
            try ( DataInputStream in  = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                    DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))
            ) {
                while (!socket.isClosed()) {
                    int length;
                    try {
                        length = in.readInt();
                    } catch (EOFException e) { break; }

                    if (length <= 0 || length > BUFFER_SIZE) {
                        System.err.println("[TCP Server] Invalid frame length " + length + " from " + clientAddress);
                        break;
                    }

                    byte[] data = new byte[length];
                    in.readFully(data);

                    Packet request = decrypter.decrypt(data);
                    String result = commandHandler.handle(request.getMessage());
                    Message response = new Message( request.getMessage().getCType(), request.getMessage().getBUserId(), result);
                    Packet responsePacket = new Packet(request.getBSrc(), request.getBPktId(), response);
                    byte[] responseBytes = encrypter.encrypt(responsePacket);

                    out.writeInt(responseBytes.length);
                    out.write(responseBytes);
                    out.flush();
                }
            } catch (SocketTimeoutException e) { System.err.println("[TCP Server] Client timed out: " + clientAddress);}
            catch (SocketException e) { System.err.println("[TCP Server] Client disconnected abruptly: " + clientAddress); }
            catch (Exception e) { System.err.println("[TCP Server] Error handling client " + clientAddress + ": " + e.getMessage()); }
            finally {
                try {
                    socket.close();
                } catch (IOException e) { System.err.println("[TCP Server] Error closing socket for " + clientAddress + ": " + e.getMessage()); }

                System.out.println("[TCP Server] Connection closed: " + clientAddress);
            }
        }
    }
}