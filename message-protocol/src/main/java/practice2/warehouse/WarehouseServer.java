package practice2.warehouse;

import javax.crypto.SecretKey;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import practice1.Packet;
import practice2.network.FakeReceiver;
import practice2.network.FakeSender;
import practice2.pipeline.DecrypterWorker;
import practice2.pipeline.EncrypterWorker;
import practice2.pipeline.Processor;

public class WarehouseServer {
    private final ExecutorService executor;
    private final Warehouse warehouse;
    private final SecretKey secretKey;

    private final BlockingQueue<byte[]> inQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<Packet> packetQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<Packet> responseQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<byte[]> outQueue = new LinkedBlockingQueue<>();

    private final int receivers;
    private final int decrypters;
    private final int processors;
    private final int encrypters;
    private final int senders;

    public WarehouseServer(SecretKey secretKey, int receivers, int decrypters, int processors, int encrypters, int senders) {
        this.secretKey = secretKey;
        this.warehouse = new Warehouse();
        this.receivers = receivers;
        this.decrypters = decrypters;
        this.processors = processors;
        this.encrypters = encrypters;
        this.senders = senders;
        this.executor = Executors.newFixedThreadPool(receivers + decrypters + processors + encrypters + senders);
    }

    public void start() {
        for (int i = 0; i < receivers; i++) executor.submit(new FakeReceiver(inQueue, secretKey));

        for (int i = 0; i < decrypters; i++) executor.submit(new DecrypterWorker(inQueue, packetQueue, secretKey));

        for (int i = 0; i < processors; i++) executor.submit(new Processor(packetQueue, responseQueue, warehouse));

        for (int i = 0; i < encrypters; i++) executor.submit(new EncrypterWorker(responseQueue, outQueue, secretKey));

        for (int i = 0; i < senders; i++) executor.submit(new FakeSender(outQueue));

        System.out.println("WarehouseServer started");
    }

    public void stop() {
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS))
                System.err.println("WarehouseServer did not stop correctly");
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        System.out.println("WarehouseServer stopped");
    }
}