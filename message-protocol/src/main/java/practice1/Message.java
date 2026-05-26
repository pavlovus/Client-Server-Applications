package practice1;

public class Message {
    private int cType;
    private int bUserId;
    private String messageContent;

    public Message(int cType, int bUserId, String messageContent) {
        this.cType = cType;
        this.bUserId = bUserId;
        this.messageContent = messageContent;
    }

    public int getCType() { return cType; }
    public void setCType(int cType) { this.cType = cType; }

    public int getBUserId() { return bUserId; }
    public void setBUserId(int bUserId) { this.bUserId = bUserId; }

    public String getMessageContent() { return messageContent; }
    public void setMessageContent(String message) { this.messageContent = message; }
}