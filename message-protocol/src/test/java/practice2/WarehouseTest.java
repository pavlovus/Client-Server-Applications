package practice2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import practice2.warehouse.Warehouse;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WarehouseTest {
    private Warehouse SUT;

    @BeforeEach
    void setUp() { SUT = new Warehouse(); }

    @Test
    void testConcurrentAddToStock() throws InterruptedException {
        int threads = 10;
        int amountPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                SUT.addToStock("гречка", amountPerThread);
                latch.countDown();
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        assertEquals(100, SUT.getProductQuantity("гречка"));
    }

    @Test
    void testConcurrentDeleteFromStock() throws InterruptedException {
        SUT.addToStock("рис", 100);

        int threads = 10;
        int amountPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                boolean success = SUT.deleteFromStock("рис", amountPerThread);
                if (success) successCount.incrementAndGet();
                latch.countDown();
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        assertEquals(10, successCount.get());
        assertEquals(50, SUT.getProductQuantity("рис"));
        assertTrue(SUT.getProductQuantity("рис") >= 0, "Кількість не може бути від'ємною");
    }

    @Test
    void testConcurrentDeleteNeverGoesNegative() throws InterruptedException {
        SUT.addToStock("цукор", 30);

        int threads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                SUT.deleteFromStock("цукор", 5);
                latch.countDown();
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        assertTrue(SUT.getProductQuantity("цукор") >= 0, "Кількість не може бути від'ємною");
    }

    @Test
    void testConcurrentAddAndDelete() throws InterruptedException {
        SUT.addToStock("борошно", 50);

        int threads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            final int index = i;
            executor.submit(() -> {
                if (index % 2 == 0) SUT.addToStock("борошно", 10);
                else SUT.deleteFromStock("борошно", 10);
                latch.countDown();
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        assertEquals(50, SUT.getProductQuantity("борошно"));
    }

    @Test
    void testConcurrentAddGroup() throws InterruptedException {
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                SUT.addEmptyGroup("крупи");
                latch.countDown();
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        assertTrue(SUT.addProductToGroup("крупи", "гречка"), "Група має існувати після конкурентного створення");
    }

    @Test
    void testConcurrentAddProductToGroup() throws InterruptedException {
        SUT.addEmptyGroup("бакалія");

        String[] products = {"гречка", "рис", "цукор", "борошно", "пшоно"};
        int threads = products.length;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (String product : products) {
            executor.submit(() -> {
                SUT.addProductToGroup("бакалія", product);
                latch.countDown();
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        for (String product : products) {
            assertFalse(SUT.addProductToGroup("бакалія", product), product + " вже має бути в групі");
        }
    }

    @Test
    void testConcurrentSetPrice() throws InterruptedException {
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        SUT.addToStock("гречка", 100);

        for (int i = 0; i < threads; i++) {
            final double price = i * 10.0;
            executor.submit(() -> {
                SUT.setProductPrice("гречка", price);
                latch.countDown();
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        assertTrue(SUT.getProductQuantity("гречка") >= 0, "Склад має залишатись в коректному стані після конкурентного встановлення ціни");
    }
}
