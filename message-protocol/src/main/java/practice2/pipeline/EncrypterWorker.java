package practice2.pipeline;

import practice1.Encrypter;
import practice1.Packet;

import javax.crypto.SecretKey;
import java.util.concurrent.BlockingQueue;

public class EncrypterWorker implements Runnable {
    private final BlockingQueue<Packet> inputQueue;
    private final BlockingQueue<byte[]> outputQueue;
    private final Encrypter encrypter;

    public EncrypterWorker(BlockingQueue<Packet> inputQueue, BlockingQueue<byte[]> outputQueue, SecretKey secretKey) {
        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
        this.encrypter = new Encrypter(secretKey);
    }

    @Override
    public void run() {
        while (true) {
            try {
                Packet data = inputQueue.take();
                byte[] packet = encrypter.encrypt(data);
                outputQueue.put(packet);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) { System.out.println("Mistake while encrypting packet: " + e.getMessage()); }
        }

        System.out.println("Encrypter worker stopped");
    }
}