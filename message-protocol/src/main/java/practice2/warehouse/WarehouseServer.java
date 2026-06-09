package practice2.warehouse;

import practice1.Packet;
import practice2.network.FakeReceiver;
import practice2.network.FakeSender;
import practice2.pipeline.DecrypterWorker;
import practice2.pipeline.EncrypterWorker;
import practice2.pipeline.Processor;
import practice4.Database;
import practice4.ProductRepository;
import practice4.ProductService;

import javax.crypto.SecretKey;
import java.util.concurrent.*;

public class WarehouseServer {
    private final ExecutorService executor;
    private final ProductService productService;
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

    public WarehouseServer(SecretKey secretKey, ProductService productService, int receivers, int decrypters, int processors, int encrypters, int senders) {
        this.secretKey = secretKey;
        this.productService = productService;
        this.receivers = receivers;
        this.decrypters = decrypters;
        this.processors = processors;
        this.encrypters = encrypters;
        this.senders = senders;
        this.executor = Executors.newFixedThreadPool(receivers + decrypters + processors + encrypters + senders);
    }

    public WarehouseServer(SecretKey secretKey, String dbUrl, int receivers, int decrypters, int processors, int encrypters, int senders) {
        this(secretKey, new ProductService(new ProductRepository(new Database(dbUrl))), receivers, decrypters, processors, encrypters, senders);
    }

    public WarehouseServer(SecretKey secretKey, int receivers, int decrypters, int processors, int encrypters, int senders) {
        this(secretKey, "jdbc:sqlite:warehouse.db", receivers, decrypters, processors, encrypters, senders);
    }

    public void start() {
        for (int i = 0; i < receivers; i++) executor.submit(new FakeReceiver(inQueue, secretKey));

        for (int i = 0; i < decrypters; i++) executor.submit(new DecrypterWorker(inQueue, packetQueue, secretKey));

        for (int i = 0; i < processors; i++) executor.submit(new Processor(packetQueue, responseQueue, productService));

        for (int i = 0; i < encrypters; i++) executor.submit(new EncrypterWorker(responseQueue, outQueue, secretKey));

        for (int i = 0; i < senders; i++) executor.submit(new FakeSender(outQueue));

        System.out.println("WarehouseServer started");
    }

    public void stop() {
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) System.err.println("WarehouseServer did not stop correctly");
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        System.out.println("WarehouseServer stopped");
    }
}