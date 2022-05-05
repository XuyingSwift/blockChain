import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class Node {
    private boolean testing = true;
    private String name;
    private HashMap<String, Block> blockChain;
    private HashMap<String, RemoteNode> remoteNodes;
    private Block longestChainHead;
    private Server server;
    private HashMap<UUID, Message> awaitingReplies;
    private ArrayList<Client> openClients;

    public Node(String name, int port, HashMap<String, RemoteNode> remoteNodes) {
        this.name = name;
        this.blockChain = new HashMap<>();
        this.longestChainHead = null;
        this.remoteNodes = remoteNodes;
        this.awaitingReplies = new HashMap<>();
        this.openClients = new ArrayList<>();
        this.server = new Server(port);
    }

    public void run() {
        this.server.start();
        MessageHolder nextHolder;
        long lastTest = System.nanoTime();

        while (true) {
            //do stuff

            if (testing && ((System.nanoTime() - lastTest) / 1000000) >= 5000) { //run test block every 5 seconds if in testing mode
                doTests();
                lastTest = System.nanoTime();
            }

            nextHolder = server.getNextReadyHolder();
            while (nextHolder != null) {
                deliverMessage(nextHolder.getMessage());
                nextHolder = server.getNextReadyHolder();
            }

            //do stuff

            cleanClients();

            //do stuff
        }
    }

    private void addBlock(Block block) {
        this.blockChain.put(block.getHash(), block);
        if (this.longestChainHead == null || block.getNumber() > this.longestChainHead.getNumber()) {
            this.longestChainHead = block;
        }
    }

    private void verifyBlock(Block block) {
        ArrayList<Block> startChain = new ArrayList<>();
        startChain.add(block);
        ArrayList<Block> totalChain = findChain(startChain);

        //TODO: reverse the list totalChain (or work through it backwards)
        //TODO: go through each transaction from each block and make sure no one runs out of money
    }

    //TODO: use a stack instead
    private ArrayList<Block> findChain(ArrayList<Block> chain) {
        Block lastBlock = chain.get(chain.size() - 1);
        String hashForPrevious = lastBlock.getPrevious();

        if (hashForPrevious.equals("0000000000")) { //TODO: what is the hash value for the first block?
            return chain;
        }
        else {
            chain.add(blockChain.get(hashForPrevious));
            return findChain(chain);
        }
    }

    private void sendMessage(String dest, Message message) {
        this.awaitingReplies.put(message.getGuid(), message);
        Client client = new Client(this.remoteNodes.get(dest).getAddress(), this.remoteNodes.get(dest).getPort(), message);
        client.start();
        this.openClients.add(client);
    }

    private void deliverMessage(Message message) {
        System.out.println(Colors.ANSI_CYAN + "Node (" + Thread.currentThread().getName() + "): Delivering " + message.getType() + " message [" + message.getGuid() + "] from node " + message.getSender() + Colors.ANSI_RESET);
        System.out.println(Colors.ANSI_CYAN + "     " + message.getPayload() + Colors.ANSI_RESET);

        if (message.getType().equals(Message.REPLY_TYPE)) {
            JsonObject msgJson = new JsonParser().parse(message.getPayload()).getAsJsonObject();

            UUID origId = UUID.fromString(msgJson.get("originalMessageId").getAsString());
            Message origMessage = awaitingReplies.get(origId);
            awaitingReplies.remove(origId);

            System.out.println(Colors.ANSI_CYAN + "Node (" + Thread.currentThread().getName() + "): Received reply for message [" + origMessage.getGuid() + "] to node " + origMessage.getDestination() + ", processing" + Colors.ANSI_RESET);
            //TODO: more processing based on type of original message and contents of reply
            if (origMessage.getType().equals(Message.TEST_TYPE)) {
                if (!message.getPayload().equals(Server.ACK)) {
                    JsonObject replyJson = new JsonParser().parse(message.getPayload()).getAsJsonObject();

                    if (replyJson.get("response").equals("BAD")) {
                        System.out.println(Colors.ANSI_YELLOW + "It didn't like my number :(" + Colors.ANSI_RESET);
                    }
                    else if (replyJson.get("response").equals("GOOD")) {
                        System.out.println(Colors.ANSI_YELLOW + "It liked my number :)" + Colors.ANSI_RESET);
                    }
                }
            }
        }
        else if (message.getType().equals(Message.TEST_TYPE)) {
            processTestMessage(message);
        }
        //TODO: more processing for other message types
    }

    private void cleanClients() {
        ArrayList<Client> removeList = new ArrayList<>();

        for (Client curClient : openClients) {
            if (curClient.getMessageState().equals(Client.DONE)) {
                System.out.println(Colors.ANSI_CYAN + "Node (" + Thread.currentThread().getName() + "): Client for message [" + curClient.getMessage().getGuid() + "] to node " + curClient.getMessage().getDestination() + " is done, cleaning up" + Colors.ANSI_RESET);
                removeList.add(curClient);
            }
            else if (curClient.getMessageState().equals(Client.EXCEPTION)) {
                System.out.println(Colors.ANSI_CYAN + "Node (" + Thread.currentThread().getName() + "): Client for message [" + curClient.getMessage().getGuid() + "] to node " + curClient.getMessage().getDestination() + " errored, cleaning up" + Colors.ANSI_RESET);
                awaitingReplies.remove(curClient.getMessage().getGuid());
                removeList.add(curClient);
            }
        }

        openClients.removeAll(removeList);
    }

    private void doTests() {
        Random rand = new Random();
        String destNode = (String) remoteNodes.keySet().toArray()[rand.nextInt(remoteNodes.size())];

        JsonObject msgJson = new JsonObject();
        msgJson.addProperty("theValue", rand.nextInt(500) + 1);

        Message testMessage = new Message(this.name, destNode, Message.TEST_TYPE, msgJson.toString());
        System.out.println(Colors.ANSI_CYAN + "Node (" + Thread.currentThread().getName() + "): Sending test message [" + testMessage.getGuid() + "] to node " + destNode + Colors.ANSI_RESET);
        System.out.println(Colors.ANSI_CYAN + "     " + testMessage.getPayload() + Colors.ANSI_RESET);
        sendMessage(destNode, testMessage);
    }

    private void processTestMessage(Message message) {
        JsonObject msgJson = new JsonParser().parse(message.getPayload()).getAsJsonObject();
        int theValue = msgJson.get("theValue").getAsInt();

        String replyPayload;

        if (theValue % 2 == 0) {
            System.out.println(Colors.ANSI_YELLOW + "Value " + theValue + " is even" + Colors.ANSI_RESET);
            replyPayload = Server.ACK;
        }
        else {
            System.out.println(Colors.ANSI_YELLOW + "Value " + theValue + " is odd" + Colors.ANSI_RESET);

            Random rand = new Random();

            if (rand.nextBoolean()) {
                System.out.println(Colors.ANSI_YELLOW + "I DON'T LIKE IT" + Colors.ANSI_RESET);
                JsonObject replyJson = new JsonObject();
                replyJson.addProperty("response", "BAD");
                replyPayload = replyJson.toString();
            }
            else {
                System.out.println(Colors.ANSI_YELLOW + "it's okay" + Colors.ANSI_RESET);
                JsonObject replyJson = new JsonObject();
                replyJson.addProperty("response", "GOOD");
                replyPayload = replyJson.toString();
            }
        }

        Message reply = new Message(this.name, message.getSender(), Message.REPLY_TYPE, replyPayload);
        sendMessage(reply.getDestination(), reply);
    }
}
