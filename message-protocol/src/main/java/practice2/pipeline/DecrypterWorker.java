package practice2.pipeline;

import practice1.Decrypter;
import practice1.Packet;

import javax.crypto.SecretKey;
import java.util.concurrent.BlockingQueue;

public class DecrypterWorker implements Runnable {
    private final BlockingQueue<byte[]> inputQueue;
    private final BlockingQueue<Packet> outputQueue;
    private final Decrypter decrypter;

    public DecrypterWorker(BlockingQueue<byte[]> inputQueue, BlockingQueue<Packet> outputQueue, SecretKey secretKey) {
        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
        this.decrypter = new Decrypter(secretKey);
    }

    @Override
    public void run() {
        while (true) {
            try {
                byte[] data = inputQueue.take();
                Packet packet = decrypter.decrypt(data);
                outputQueue.put(packet);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) { System.out.println("Mistake while decrypting packet: " + e.getMessage()); }
        }

        System.out.println("Decrypter worker stopped");
    }
}