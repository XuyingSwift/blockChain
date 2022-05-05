public class RemoteNode {
    private String address, id;
    private int port;

    public RemoteNode(String id, String address, int port) {
        this.id = id;
        this.address = address;
        this.port = port;
    }

    public String getId() { return id; }
    public String getAddress() { return address; }
    public int getPort() { return port; }
}
