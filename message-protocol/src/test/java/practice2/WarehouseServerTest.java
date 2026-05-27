package practice2;

import org.junit.jupiter.api.*;
import practice2.warehouse.WarehouseServer;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.*;

class WarehouseServerTest {
    private static SecretKey secretKey;
    private WarehouseServer SUT;

    @BeforeAll
    static void generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        secretKey = keyGen.generateKey();
    }

    @AfterEach
    void tearDown() { if (SUT != null) SUT.stop(); }

    @Test
    void testServerStartsAndStops() {
        SUT = new WarehouseServer(secretKey, 1, 1, 1, 1, 1);
        assertDoesNotThrow(() -> {
            SUT.start();
            SUT.stop();
        });
    }

    @Test
    void testServerHandlesConcurrentMessages() throws InterruptedException {
        SUT = new WarehouseServer(secretKey, 5, 3, 6, 3, 5);
        assertDoesNotThrow(() -> {
            SUT.start();
            Thread.sleep(10000);
            SUT.stop();
        });
    }
}