import java.util.UUID;

public class Message {
    public static final String REPLY_TYPE = "REPLY", BLOCK_TYPE = "BLOCK", TEST_TYPE = "TEST";
    private String type, payload;
    private String sender, destination;
    private UUID guid;

    public Message(String sender, String destination, String type, String payload) {
        this.guid = UUID.randomUUID();
        this.sender = sender;
        this.destination = destination;
        this.type = type;
        this.payload = payload;
    }

    public UUID getGuid() { return guid; }
    public String getSender() { return sender; }
    public String getDestination() { return destination; }
    public String getType() { return type; }
    public String getPayload() { return payload; }
}
