package practice2.network;

import practice1.Encrypter;
import practice1.Message;
import practice1.Packet;
import practice2.warehouse.CommandType;

import javax.crypto.SecretKey;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

public class FakeReceiver implements Receiver, Runnable {
    private final BlockingQueue<byte[]> outputQueue;
    private final Encrypter encrypter;
    private final Random random = new Random();

    public FakeReceiver(BlockingQueue<byte[]> outputQueue, SecretKey secretKey) {
        this.outputQueue = outputQueue;
        this.encrypter = new Encrypter(secretKey);
    }

    @Override
    public void run() {
        while (true) {
            try {
                byte[] packet = receive();
                outputQueue.put(packet);
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) { System.err.println("Mistake while generating packet: " + e.getMessage()); }
        }

        System.out.println("Fake receiver stopped");
    }
    @Override
    public byte[] receive() throws Exception {
        CommandType cType = CommandType.values()[random.nextInt(CommandType.values().length)];;
        int userId = random.nextInt(100);
        byte src = (byte) random.nextInt(256);
        long pktId = random.nextLong();

        String content = generateContent(cType);
        Message message = new Message(cType.getCode(), userId, content);
        Packet packet = new Packet(src, pktId, message);

        return encrypter.encrypt(packet);
    }

    private String generateContent(CommandType command) {
        String[] products = {"гречка", "рис", "цукор", "борошно"};
        String[] groups = {"крупи", "бакалія"};
        String product = products[random.nextInt(products.length)];
        String group = groups[random.nextInt(groups.length)];

        return switch (command) {
            case GET_QUANTITY -> product;
            case DELETE_FROM_STOCK, ADD_TO_STOCK -> product + ":" + (random.nextInt(50) + 1);
            case ADD_GROUP -> group;
            case ADD_PRODUCT_TO_GROUP -> group + ":" + product;
            case SET_PRICE -> product + ":" + String.format("%.2f", random.nextDouble() * 100);
        };
    }
}