import java.util.UUID;

public class Message {
    public static final String REPLY_TYPE = "REPLY";
    private String type, payload;
    private int sender, destination;
    private UUID guid;

    public Message(int sender, int destination, String type, String payload) {
        this.guid = UUID.randomUUID();
        this.sender = sender;
        this.destination = destination;
        this.type = type;
        this.payload = payload;
    }

    public UUID getGuid() { return guid; }
    public int getSender() { return sender; }
    public int getDestination() { return destination; }
    public String getType() { return type; }
    public String getPayload() { return payload; }
}
