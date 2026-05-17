package org.example;

public class Packet {
    private byte bSrc;
    private long bPktId;

    private Message message;

    public Packet(byte bSrc, long bPktId, Message message) {
        this.bSrc = bSrc;
        this.bPktId = bPktId;
        this.message = message;
    }

    public byte getBSrc() { return bSrc; }
    public void setBSrc(byte bSrc) { this.bSrc = bSrc; }

    public long getBPktId() { return bPktId; }
    public void setBPktId(long bPktId) { this.bPktId = bPktId; }

    public Message getMessage() { return message; }
    public void setMessage(Message message) { this.message = message; }
}