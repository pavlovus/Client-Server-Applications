package practice2.warehouse;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Warehouse {
    private final ConcurrentHashMap<String, AtomicInteger> stock;
    private final ConcurrentHashMap<String, Double> prices;
    private final ConcurrentHashMap<String, Set<String>> groups;

    public Warehouse() {
        stock = new ConcurrentHashMap<>();
        prices = new ConcurrentHashMap<>();
        groups = new ConcurrentHashMap<>();
    }

    public int getProductQuantity(String productName) {
        AtomicInteger current = stock.get(productName);
        return current == null ? -1 : current.get();
    }

    public boolean deleteFromStock(String productName, int amount) {
        AtomicInteger current = stock.get(productName);
        if (current == null) return false;

        while (true) {
            int currentVal = current.get();
            if (currentVal < amount) return false;

            if (current.compareAndSet(currentVal, currentVal - amount)) { return true; }
        }
    }

    public int addToStock(String productName, int amount) {
        return stock.computeIfAbsent(productName, k -> new AtomicInteger(0)).addAndGet(amount);
    }

    public boolean addEmptyGroup(String groupName) {
        return groups.putIfAbsent(groupName, ConcurrentHashMap.newKeySet()) == null;
    }

    public boolean addProductToGroup(String groupName, String productName) {
        Set<String> current = groups.get(groupName);
        return current != null && current.add(productName);
    }

    public boolean setProductPrice(String productName, double price) {
        if (!stock.containsKey(productName)) return false;
        prices.put(productName, price);
        return true;
    }
}