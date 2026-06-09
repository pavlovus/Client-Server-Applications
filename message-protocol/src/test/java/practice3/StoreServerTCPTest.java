package practice3;

import org.junit.jupiter.api.*;
import practice2.warehouse.CommandType;
import practice3.tcp.StoreClientTCP;
import practice3.tcp.StoreServerTCP;
import practice4.Database;
import practice4.Product;
import practice4.ProductRepository;
import practice4.ProductService;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class StoreServerTCPTest {

    private static SecretKey secretKey;

    private ProductService productService;
    private StoreServerTCP server;
    private int port;
    private Path tempDbFile;

    @BeforeAll
    static void generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        secretKey = keyGen.generateKey();
    }

    @BeforeEach
    void setUp() throws Exception {
        tempDbFile = Files.createTempFile("store_test_", ".db");

        productService = new ProductService(new ProductRepository(new Database("jdbc:sqlite:" + tempDbFile.toAbsolutePath())));

        port = findFreePort();
        startServer(productService);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.stop();
        Files.deleteIfExists(tempDbFile);
    }

    private void startServer(ProductService ps) throws InterruptedException {
        server = new StoreServerTCP(port, secretKey, ps);
        Thread serverThread = new Thread(() -> {
            try { server.start(); } catch (Exception ignored) {}
        }, "test-tcp-server");
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(150);
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket s = new ServerSocket(0)) {
            s.setReuseAddress(true);
            return s.getLocalPort();
        }
    }

    private int getQuantity(String name) { return productService.getByName(name).map(Product::getQuantity).orElse(-1); }

    @Test
    void shouldAddToStock() throws Exception {
        try (StoreClientTCP client = new StoreClientTCP("localhost", port, secretKey)) {
            String result = client.sendAndReceive(CommandType.ADD_TO_STOCK, 1, "гречка:50");
            assertEquals("OK:50", result);
            assertEquals(50, getQuantity("гречка"));
        }
    }

    @Test
    void shouldAddToStockAccumulates() throws Exception {
        try (StoreClientTCP client = new StoreClientTCP("localhost", port, secretKey)) {
            client.sendAndReceive(CommandType.ADD_TO_STOCK, 1, "рис:30");
            String result = client.sendAndReceive(CommandType.ADD_TO_STOCK, 1, "рис:20");
            assertEquals("OK:50", result);
            assertEquals(50, getQuantity("рис"));
        }
    }

    @Test
    void shouldGetQuantityExistingProduct() throws Exception {
        productService.addToStock("цукор", 100);
        try (StoreClientTCP client = new StoreClientTCP("localhost", port, secretKey)) {
            String result = client.sendAndReceive(CommandType.GET_QUANTITY, 1, "цукор");
            assertEquals("OK:100", result);
        }
    }

    @Test
    void shouldNotGetQuantityNonExistingProduct() throws Exception {
        try (StoreClientTCP client = new StoreClientTCP("localhost", port, secretKey)) {
            String result = client.sendAndReceive(CommandType.GET_QUANTITY, 1, "невідомий");
            assertTrue(result.startsWith("ERROR"));
        }
    }

    @Test
    void shouldDeleteFromStock() throws Exception {
        productService.addToStock("борошно", 100);
        try (StoreClientTCP client = new StoreClientTCP("localhost", port, secretKey)) {
            String result = client.sendAndReceive(CommandType.DELETE_FROM_STOCK, 1, "борошно:30");
            assertEquals("OK", result);
            assertEquals(70, getQuantity("борошно"));
        }
    }

    @Test
    void shouldNotDeleteFromStockIfInsufficientStock() throws Exception {
        productService.addToStock("пшоно", 10);
        try (StoreClientTCP client = new StoreClientTCP("localhost", port, secretKey)) {
            String result = client.sendAndReceive(CommandType.DELETE_FROM_STOCK, 1, "пшоно:50");
            assertTrue(result.startsWith("ERROR"));
            assertEquals(10, getQuantity("пшоно"));
        }
    }

    @Test
    void shouldAddGroup() throws Exception {
        try (StoreClientTCP client = new StoreClientTCP("localhost", port, secretKey)) {
            String result = client.sendAndReceive(CommandType.ADD_GROUP, 1, "крупи");
            assertEquals("OK", result);
        }
    }

    @Test
    void shouldNotAddDuplicateGroup() throws Exception {
        try (StoreClientTCP client = new StoreClientTCP("localhost", port, secretKey)) {
            client.sendAndReceive(CommandType.ADD_GROUP, 1, "крупи");
            String result = client.sendAndReceive(CommandType.ADD_GROUP, 1, "крупи");
            assertTrue(result.startsWith("ERROR"));
        }
    }

    @Test
    void shouldAddProductToGroup() throws Exception {
        productService.addCategory("бакалія");
        productService.addToStock("гречка", 1);
        try (StoreClientTCP client = new StoreClientTCP("localhost", port, secretKey)) {
            String result = client.sendAndReceive(CommandType.ADD_PRODUCT_TO_GROUP, 1, "бакалія:гречка");
            assertEquals("OK", result);
        }
    }

    @Test
    void shouldSetPrice() throws Exception {
        productService.addToStock("гречка", 100);
        try (StoreClientTCP client = new StoreClientTCP("localhost", port, secretKey)) {
            String result = client.sendAndReceive(CommandType.SET_PRICE, 1, "гречка:49.99");
            assertEquals("OK", result);
        }
    }

    @Test
    void shouldNotSetPriceNonExistingProduct() throws Exception {
        try (StoreClientTCP client = new StoreClientTCP("localhost", port, secretKey)) {
            String result = client.sendAndReceive(CommandType.SET_PRICE, 1, "невідомий:49.99");
            assertTrue(result.startsWith("ERROR"));
        }
    }

    @Test
    void shouldWorkWithConcurrentClientsAddToStock() throws Exception {
        int clientCount = 10;
        int amountPerClient = 10;
        ExecutorService executor = Executors.newFixedThreadPool(clientCount);
        CountDownLatch latch = new CountDownLatch(clientCount);
        List<String> results = new CopyOnWriteArrayList<>();

        for (int i = 0; i < clientCount; i++) {
            executor.submit(() -> {
                try (StoreClientTCP client = new StoreClientTCP("localhost", port, secretKey)) {
                    results.add(client.sendAndReceive(CommandType.ADD_TO_STOCK, 1, "гречка:" + amountPerClient));
                } catch (Exception e) { results.add("ERROR: " + e.getMessage());}
                finally { latch.countDown(); }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(clientCount, results.size());
        assertTrue(results.stream().allMatch(r -> r.startsWith("OK")), "Всі клієнти мають отримати OK, отримано: " + results);
        assertEquals(clientCount * amountPerClient, getQuantity("гречка"));
    }

    @Test
    void shouldWorkWithConcurrentClientsMixedOperations() throws Exception {
        productService.addToStock("рис", 1000);
        int clientCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(clientCount);
        CountDownLatch latch = new CountDownLatch(clientCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < clientCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try (StoreClientTCP client = new StoreClientTCP("localhost", port, secretKey)) {
                    String result = index % 2 == 0 ? client.sendAndReceive(CommandType.ADD_TO_STOCK, 1, "рис:10") : client.sendAndReceive(CommandType.DELETE_FROM_STOCK, 1, "рис:10");

                    if (result.startsWith("OK")) successCount.incrementAndGet();
                } catch (Exception e) { System.err.println("Помилка клієнта: " + e.getMessage());}
                finally { latch.countDown(); }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(clientCount, successCount.get(), "Всі операції мають бути успішними");
        assertEquals(1000, getQuantity("рис"));
        assertTrue(getQuantity("рис") >= 0, "Кількість не може бути від'ємною");
    }

    @Test
    void shouldHandleMultipleRequestsOnSingleConnection() throws Exception {
        try (StoreClientTCP client = new StoreClientTCP("localhost", port, secretKey)) {
            assertEquals("OK:10", client.sendAndReceive(CommandType.ADD_TO_STOCK, 1, "гречка:10"));
            assertEquals("OK:30", client.sendAndReceive(CommandType.ADD_TO_STOCK, 1, "гречка:20"));
            assertEquals("OK:30", client.sendAndReceive(CommandType.GET_QUANTITY, 1, "гречка"));
            assertEquals("OK", client.sendAndReceive(CommandType.DELETE_FROM_STOCK, 1, "гречка:30"));
            assertEquals(0, getQuantity("гречка"));
        }
    }

    @Test
    void clientShouldWaitUntilServerBecomesAvailable() throws Exception {
        server.stop();
        Thread.sleep(200);

        StoreClientTCP client = new StoreClientTCP("localhost", port, secretKey);

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1500);
                startServer(productService);
            } catch (Exception ignored) {}
        });

        String result = client.sendAndReceive(CommandType.ADD_TO_STOCK, 1, "гречка:50");
        assertEquals("OK:50", result);
        client.close();
    }

    @Test
    void clientShouldBeClosedWhileWaitingForServer() throws Exception {
        server.stop();
        Thread.sleep(200);

        StoreClientTCP client = new StoreClientTCP("localhost", port, secretKey);

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(800);
                client.close();
            } catch (Exception ignored) {}
        });

        assertThrows(Exception.class, () -> client.sendAndReceive(CommandType.ADD_TO_STOCK, 1, "гречка:50"));
    }
}