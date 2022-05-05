import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;
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

        while (true) {
            //do stuff

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
        if (message.getType().equals(Message.REPLY_TYPE)) {
            JsonObject msgJson = new JsonParser().parse(message.getPayload()).getAsJsonObject();

            UUID origId = UUID.fromString(msgJson.get("originalMessageId").getAsString());
            Message origMessage = awaitingReplies.get(origId);
            awaitingReplies.remove(origId);

            //TODO: more processing based on type of original message and contents of reply
        }
        //TODO: more processing for other message types
    }

    private void cleanClients() {
        ArrayList<Client> removeList = new ArrayList<>();

        for (Client curClient : openClients) {
            if (curClient.getMessageState().equals(Client.DONE)) {
                removeList.add(curClient);
            }
            else if (curClient.getMessageState().equals(Client.EXCEPTION)) {
                awaitingReplies.remove(curClient.getMessage().getGuid());
                removeList.add(curClient);
            }
        }

        openClients.removeAll(removeList);
    }
}
