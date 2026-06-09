package practice3;

import org.junit.jupiter.api.*;
import practice2.warehouse.CommandType;
import practice3.udp.StoreClientUDP;
import practice3.udp.StoreServerUDP;
import practice4.Database;
import practice4.Product;
import practice4.ProductRepository;
import practice4.ProductService;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.DatagramSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class StoreServerUDPTest {

    private static SecretKey secretKey;

    private ProductService productService;
    private StoreServerUDP server;
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
        tempDbFile = Files.createTempFile("store_udp_test_", ".db");
        productService = new ProductService(new ProductRepository(new Database("jdbc:sqlite:" + tempDbFile.toAbsolutePath())));
        port = findFreeUDPPort();
        server = new StoreServerUDP(port, secretKey, productService);
        Thread serverThread = new Thread(() -> { try { server.start(); } catch (Exception ignored) {} }, "test-udp-server");
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(150);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.stop();
        Files.deleteIfExists(tempDbFile);
    }

    private static int findFreeUDPPort() throws Exception { try (DatagramSocket s = new DatagramSocket(0)) { return s.getLocalPort(); } }

    private StoreClientUDP fastRetryClient() throws Exception { return new StoreClientUDP("localhost", port, secretKey, 300, 2); }

    private int getQuantity(String name) { return productService.getByName(name).map(Product::getQuantity).orElse(-1); }

    @Test
    void shouldAddToStock() throws Exception {
        try (StoreClientUDP client = new StoreClientUDP("localhost", port, secretKey)) {
            String result = client.sendAndReceive(CommandType.ADD_TO_STOCK, 1, "гречка:50");
            assertEquals("OK:50", result);
            assertEquals(50, getQuantity("гречка"));
        }
    }

    @Test
    void shouldAddToStockAccumulates() throws Exception {
        try (StoreClientUDP client = new StoreClientUDP("localhost", port, secretKey)) {
            client.sendAndReceive(CommandType.ADD_TO_STOCK, 1, "рис:30");
            String result = client.sendAndReceive(CommandType.ADD_TO_STOCK, 1, "рис:20");
            assertEquals("OK:50", result);
            assertEquals(50, getQuantity("рис"));
        }
    }

    @Test
    void shouldGetQuantityExistingProduct() throws Exception {
        productService.addToStock("цукор", 100);
        try (StoreClientUDP client = new StoreClientUDP("localhost", port, secretKey)) {
            String result = client.sendAndReceive(CommandType.GET_QUANTITY, 1, "цукор");
            assertEquals("OK:100", result);
        }
    }

    @Test
    void shouldNotGetQuantityNonExistingProduct() throws Exception {
        try (StoreClientUDP client = new StoreClientUDP("localhost", port, secretKey)) {
            String result = client.sendAndReceive(CommandType.GET_QUANTITY, 1, "невідомий");
            assertTrue(result.startsWith("ERROR"));
        }
    }

    @Test
    void shouldDeleteFromStock() throws Exception {
        productService.addToStock("борошно", 100);
        try (StoreClientUDP client = new StoreClientUDP("localhost", port, secretKey)) {
            String result = client.sendAndReceive(CommandType.DELETE_FROM_STOCK, 1, "борошно:30");
            assertEquals("OK", result);
            assertEquals(70, getQuantity("борошно"));
        }
    }

    @Test
    void shouldNotDeleteFromStockInsufficientStock() throws Exception {
        productService.addToStock("пшоно", 10);
        try (StoreClientUDP client = new StoreClientUDP("localhost", port, secretKey)) {
            String result = client.sendAndReceive(CommandType.DELETE_FROM_STOCK, 1, "пшоно:50");
            assertTrue(result.startsWith("ERROR"));
            assertEquals(10, getQuantity("пшоно"));
        }
    }

    @Test
    void shouldAddGroup() throws Exception {
        try (StoreClientUDP client = new StoreClientUDP("localhost", port, secretKey)) {
            String result = client.sendAndReceive(CommandType.ADD_GROUP, 1, "крупи");
            assertEquals("OK", result);
        }
    }

    @Test
    void shouldNotAddDuplicateGroup() throws Exception {
        try (StoreClientUDP client = new StoreClientUDP("localhost", port, secretKey)) {
            client.sendAndReceive(CommandType.ADD_GROUP, 1, "крупи");
            String result = client.sendAndReceive(CommandType.ADD_GROUP, 1, "крупи");
            assertTrue(result.startsWith("ERROR"));
        }
    }

    @Test
    void shouldAddProductToGroup() throws Exception {
        productService.addCategory("бакалія");
        productService.addToStock("гречка", 1);
        try (StoreClientUDP client = new StoreClientUDP("localhost", port, secretKey)) {
            String result = client.sendAndReceive(CommandType.ADD_PRODUCT_TO_GROUP, 1, "бакалія:гречка");
            assertEquals("OK", result);
        }
    }

    @Test
    void shouldSetPrice() throws Exception {
        productService.addToStock("гречка", 100);
        try (StoreClientUDP client = new StoreClientUDP("localhost", port, secretKey)) {
            String result = client.sendAndReceive(CommandType.SET_PRICE, 1, "гречка:49.99");
            assertEquals("OK", result);
        }
    }

    @Test
    void shouldNotSetPriceNonExistingProduct() throws Exception {
        try (StoreClientUDP client = new StoreClientUDP("localhost", port, secretKey)) {
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
                try (StoreClientUDP client = new StoreClientUDP("localhost", port, secretKey)) {
                    results.add(client.sendAndReceive(CommandType.ADD_TO_STOCK, 1, "гречка:" + amountPerClient));
                } catch (Exception e) { results.add("ERROR: " + e.getMessage()); }
                finally { latch.countDown(); }
            });
        }

        latch.await(15, TimeUnit.SECONDS);
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
                try (StoreClientUDP client = new StoreClientUDP("localhost", port, secretKey)) {
                    String result = index % 2 == 0 ? client.sendAndReceive(CommandType.ADD_TO_STOCK, 1, "рис:10") : client.sendAndReceive(CommandType.DELETE_FROM_STOCK, 1, "рис:10");

                    if (result.startsWith("OK")) successCount.incrementAndGet();
                } catch (Exception e) { System.err.println("Помилка клієнта: " + e.getMessage()); }
                finally { latch.countDown(); }
            });
        }

        latch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(clientCount, successCount.get(), "Всі операції мають бути успішними");
        assertEquals(1000, getQuantity("рис"));
    }

    @Test
    void clientShouldThrowsAfterMaxRetriesWhenServerUnavailable() throws Exception {
        server.stop();
        Thread.sleep(200);

        try (StoreClientUDP client = fastRetryClient()) {
            assertThrows(IOException.class, () -> client.sendAndReceive(CommandType.GET_QUANTITY, 1, "гречка"), "Клієнт має кинути IOException після вичерпання спроб");
        }
    }

    @Test
    void clientShouldSucceedAfterTransientUnavailability() throws Exception {
        try (StoreClientUDP client = new StoreClientUDP("localhost", port, secretKey)) {
            assertEquals("OK:50", client.sendAndReceive(CommandType.ADD_TO_STOCK, 1, "гречка:50"));
        }

        server.stop();
        Thread.sleep(200);

        server = new StoreServerUDP(port, secretKey, productService);
        Thread serverThread = new Thread(() -> { try { server.start(); } catch (Exception ignored) {}}, "test-udp-server-restart");
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(150);

        try (StoreClientUDP client = new StoreClientUDP("localhost", port, secretKey)) {
            String result = client.sendAndReceive(CommandType.ADD_TO_STOCK, 1, "гречка:30");
            assertEquals("OK:80", result);
        }
    }
}