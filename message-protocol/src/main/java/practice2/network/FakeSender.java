package practice2.network;

import java.util.concurrent.BlockingQueue;

public class FakeSender implements Sender, Runnable {
    private final BlockingQueue<byte[]> inputQueue;

    public FakeSender(BlockingQueue<byte[]> inputQueue) { this.inputQueue = inputQueue; }

    @Override
    public void run() {
        while (true) {
            try {
                byte[] data = inputQueue.take();
                send(data);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.println("Fake sender stopped");
    }

    @Override
    public void send(byte[] data) { System.out.println("Sent " + data.length + " bytes"); }
}