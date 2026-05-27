package practice2.pipeline;

import practice1.Message;
import practice1.Packet;
import practice2.warehouse.CommandHandler;
import practice2.warehouse.Warehouse;

import java.util.concurrent.BlockingQueue;

public class Processor implements Runnable {
    private final BlockingQueue<Packet> inputQueue;
    private final BlockingQueue<Packet> outputQueue;
    private final CommandHandler commandHandler;

    public Processor(BlockingQueue<Packet> inputQueue, BlockingQueue<Packet> outputQueue, Warehouse warehouse) {
        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
        this.commandHandler = new CommandHandler(warehouse);
    }

    @Override
    public void run() {
        while (true) {
            try {
                Packet packet = inputQueue.take();
                String result = commandHandler.handle(packet.getMessage());

                Message response = new Message(packet.getMessage().getCType(), packet.getMessage().getBUserId(), result);
                outputQueue.put(new Packet(packet.getBSrc(), packet.getBPktId(), response));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.println("Processor stopped");
    }
}