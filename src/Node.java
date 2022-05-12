import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.crypto.NoSuchPaddingException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.*;

public class Node {
    private boolean testing = false;
    private String name;
    private HashMap<String, Block> blockChain;
    private HashMap<String, StakeBlock> stakeBlockChain;
    private HashMap<String, RemoteNode> remoteNodes;
    private Block longestChainHead;
    private Server server;
    private HashMap<UUID, Message> awaitingReplies;
    private ArrayList<Client> openClients;
    private BlockMiner blockMiner;
    private KeyGenerator keyGenerator;
    private EncryptDecrypt encryptDecrypt;
    public Node(String name, int port, HashMap<String, RemoteNode> remoteNodes) {
        this.name = name;
        this.blockChain = new HashMap<>();
        this.stakeBlockChain = new HashMap<>();
        this.longestChainHead = null;
        this.remoteNodes = remoteNodes;
        this.awaitingReplies = new HashMap<>();
        this.openClients = new ArrayList<>();
        this.server = new Server(port);
        try {
            this.keyGenerator = new KeyGenerator(1024);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }
        try {
            this.encryptDecrypt = new EncryptDecrypt();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        this.server.start();
        MessageHolder nextHolder;
        long lastTest = System.nanoTime();
        this.blockMiner = new BlockMiner();

        while (true) {
            //if not already mining a block, make a new one and start mining
            if (blockMiner.getBlock() == null) {
                Block newBlock;

                if (longestChainHead == null) {
                    newBlock = new Block(1, this.name, Block.FIRST_HASH);
                } else {
                    newBlock = new Block(this.longestChainHead.getNumber() + 1, this.name, this.longestChainHead.getHash());
                    HashMap<String, Integer> chainState = computeChainState(longestChainHead);
                    System.out.println("    Starting state of next block " + newBlock.getNumber() + ": " + chainState.toString());
                    GenerateTransaction transactionGenerator = new GenerateTransaction(chainState);
                    Transaction[] newTrans = transactionGenerator.generateTransaction();
                    newBlock.setTransactions(newTrans);
                    System.out.println("    Transactions for next block " + newBlock.getNumber() + ": " + Arrays.toString(newTrans));
                }

                System.out.println(Colors.ANSI_CYAN + "Node (" + Thread.currentThread().getName() + "): Generated block " + newBlock.getNumber() + " with previous block ..." + newBlock.getPrevious().substring(57) + Colors.ANSI_RESET);
                blockMiner.setBlock(newBlock);
                blockMiner.start();
            }

            if (testing && ((System.nanoTime() - lastTest) / 1000000) >= 5000) { //run test code every 5 seconds if in testing mode
                doTests();
                lastTest = System.nanoTime();
                System.out.println(">>>>" + server.getMessageHolderCount() + " message holders, " + openClients.size() + " open clients" + "<<<<");
            }

            nextHolder = server.getNextReadyHolder();
            while (nextHolder != null) {
                deliverMessage(nextHolder.getMessage());
                nextHolder = server.getNextReadyHolder();
            }

            if (blockMiner.getBlockState().equals(BlockMiner.READY)) {
                Block myNewBlock = blockMiner.getBlock();
                addBlock(myNewBlock);
                try {
                    writeToDisk();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                broadcastBlock(myNewBlock);
                blockMiner = new BlockMiner();
            }

            cleanClients();
        }
    }

    private void addBlock(Block block) {
        if (verifyBlock(block)) {
            System.out.println(Colors.ANSI_YELLOW + "Node (" + Thread.currentThread().getName() + "): Adding new block " + block.getNumber() + " [..." + block.getHash().substring(57) + "] with previous block ..." + block.getPrevious().substring(57) + Colors.ANSI_RESET);
            this.blockChain.put(block.getHash(), block);

            if (this.longestChainHead == null || block.getNumber() > this.longestChainHead.getNumber()) {
                System.out.println(Colors.ANSI_YELLOW + "Node (" + Thread.currentThread().getName() + "): Updated head of my longest chain to block " + block.getNumber() + " [..." + block.getHash().substring(57) + "]" + Colors.ANSI_RESET);
                this.longestChainHead = block;
                blockMiner.interrupt();
                blockMiner = new BlockMiner();
            }
        }
        else {
            System.out.println(Colors.ANSI_RED + "Node (" + Thread.currentThread().getName() + "): New block " + block.getNumber() + " [..." + block.getHash().substring(57) + "] with previous block ..." + block.getPrevious().substring(57) + " was not valid; rejecting!" + Colors.ANSI_RESET);
        }
    }

    private HashMap<String, Integer> computeChainState(Block lastBlock) {
        Stack<Block> totalChain = findChain(lastBlock);
        HashMap<String, Integer> chainState = new HashMap<>();

        for (String curPerson : remoteNodes.keySet()) chainState.put(curPerson, 0);

        while (!totalChain.empty()) {
            Block curBlock = totalChain.pop();

            String miner = curBlock.getCoinbase().getPerson();
            if (!chainState.containsKey(miner)) {
                chainState.put(miner, 0);
            }
            chainState.put(miner, chainState.get(miner) + curBlock.getCoinbase().getAmount());

            for (Transaction curTxn : curBlock.getTransactions()) {
                if (curTxn != null) {
                    String from = curTxn.getFrom(), to = curTxn.getTo();

                    if (!chainState.containsKey(from)) {
                        chainState.put(from, 0);
                    }
                    if (!chainState.containsKey(to)) {
                        chainState.put(to, 0);
                    }

                    chainState.put(from, chainState.get(from) - curTxn.getAmount());
                    chainState.put(to, chainState.get(to) + curTxn.getAmount());
                }
            }
        }

        return chainState;
    }

    public boolean verifyStakeBlock(StakeBlock stakeBlock) {
        Stack<StakeBlock> totalChain = findStakeBlockChain(stakeBlock) ;
        boolean isValid = true;
        HashMap<String, Integer> chainState = new HashMap<>();

        while (!totalChain.isEmpty() && isValid) {
            StakeBlock curBlock = totalChain.pop();

            String miner = curBlock.getStakePerson().getStake_person();
            if (!chainState.containsKey(miner)) {
                chainState.put(miner, 0);
            }
            chainState.put(miner, chainState.get(miner) + curBlock.getStakePerson().getStake_amount());

            for (Transaction curTxn : curBlock.getTransactions()) {
                if (curTxn != null) {
                    String from = curTxn.getFrom(), to = curTxn.getTo();

                    if (!chainState.containsKey(from)) {
                        chainState.put(from, 0);
                    }
                    if (!chainState.containsKey(to)) {
                        chainState.put(to, 0);
                    }

                    chainState.put(from, chainState.get(from) - curTxn.getAmount());
                    //This means that someone was "DOUBLE SPENDING" and ran out of money, so it's not a valid block
                    if (chainState.get(from) < 0) isValid = false;
                    chainState.put(to, chainState.get(to) + curTxn.getAmount());
                }
            }
        }

        return isValid;
    }

    private boolean verifyBlock(Block block) {
        Stack<Block> totalChain = findChain(block);
        boolean isValid = true;
        HashMap<String, Integer> chainState = new HashMap<>();

        while (!totalChain.isEmpty() && isValid) {
            Block curBlock = totalChain.pop();

            String miner = curBlock.getCoinbase().getPerson();
            if (!chainState.containsKey(miner)) {
                chainState.put(miner, 0);
            }
            chainState.put(miner, chainState.get(miner) + curBlock.getCoinbase().getAmount());

            for (Transaction curTxn : curBlock.getTransactions()) {
                if (curTxn != null) {
                    String from = curTxn.getFrom(), to = curTxn.getTo();

                    if (!chainState.containsKey(from)) {
                        chainState.put(from, 0);
                    }
                    if (!chainState.containsKey(to)) {
                        chainState.put(to, 0);
                    }

                    chainState.put(from, chainState.get(from) - curTxn.getAmount());
                    //This means that someone was "DOUBLE SPENDING" and ran out of money, so it's not a valid block
                    if (chainState.get(from) < 0) isValid = false;
                    chainState.put(to, chainState.get(to) + curTxn.getAmount());
                }
            }
        }

        return isValid;
    }

    private Stack<Block> findChain(Block startBlock) {
        Stack<Block> chain = new Stack<>();
        chain.push(startBlock);
        return findChain(chain);
    }

    private Stack<StakeBlock> findStakeBlockChain(StakeBlock startBlock) {
        Stack<StakeBlock> chain = new Stack<StakeBlock>();
        chain.push(startBlock);
        return findStakeBlockChain(chain);
    }

    private Stack<StakeBlock> findStakeBlockChain(Stack<StakeBlock> chain) {
        StakeBlock lastBlock = chain.peek();
        String hashForPrevious = lastBlock.getPrevious();

        if (hashForPrevious.equals(StakeBlock.FIRST_HASH)) { //TODO: what is the hash value for the first block?
            return chain;
        }
        else {
            chain.push(stakeBlockChain.get(hashForPrevious));
            return findStakeBlockChain(chain);
        }
    }

    private Stack<Block> findChain(Stack<Block> chain) {
        Block lastBlock = chain.peek();
        String hashForPrevious = lastBlock.getPrevious();

        if (hashForPrevious.equals(Block.FIRST_HASH)) {
            return chain;
        }
        else {
            chain.push(blockChain.get(hashForPrevious));
            return findChain(chain);
        }
    }

    private void sendMessage(String dest, Message message, boolean waitForReply) {
        if (waitForReply) { this.awaitingReplies.put(message.getGuid(), message); }

        Client client = new Client(this.remoteNodes.get(dest).getAddress(), this.remoteNodes.get(dest).getPort(), message);
        client.start();
        this.openClients.add(client);
    }

    private void broadcastBlock(Block block) {
        Gson gson = new Gson();
        String blockJson = gson.toJson(block);

        for (String remote : remoteNodes.keySet()) {
            if (!remote.equals(this.name)) {
                Message blockMessage = new Message(this.name, remote, Message.BLOCK_TYPE, blockJson);

                System.out.println(Colors.ANSI_CYAN + "Node (" + Thread.currentThread().getName() + "): Sending block message [" + blockMessage.getGuid() + "] to node " + remote + Colors.ANSI_RESET);
                System.out.println(Colors.ANSI_CYAN + "     " + blockMessage.getPayload() + Colors.ANSI_RESET);

                sendMessage(remote, blockMessage, false);
            }
        }
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
                if (msgJson.get("response").getAsString().equals("BAD")) {
                    System.out.println(Colors.ANSI_YELLOW + "It didn't like my number :(" + Colors.ANSI_RESET);
                }
                else if (msgJson.get("response").getAsString().equals("GOOD")) {
                    System.out.println(Colors.ANSI_YELLOW + "It liked my number :)" + Colors.ANSI_RESET);
                }
            }
        }
        else if (message.getType().equals(Message.TEST_TYPE)) {
            processTestMessage(message);
        }
        else if (message.getType().equals(Message.BLOCK_TYPE)) {
            processBlockMessage(message);
        }
    }

    private void processBlockMessage(Message message) {
        Gson gson = new Gson();
        Block newBlock = gson.fromJson(message.getPayload(), Block.class);
        addBlock(newBlock);
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
        sendMessage(destNode, testMessage, true);
    }

    private void processTestMessage(Message message) {
        JsonObject msgJson = new JsonParser().parse(message.getPayload()).getAsJsonObject();
        int theValue = msgJson.get("theValue").getAsInt();

        JsonObject replyJson = new JsonObject();
        replyJson.addProperty("originalMessageId", message.getGuid().toString());

        if (theValue % 2 == 0) {
            System.out.println(Colors.ANSI_YELLOW + "Value " + theValue + " is even" + Colors.ANSI_RESET);
            replyJson.addProperty("response", Server.ACK);
        }
        else {
            System.out.println(Colors.ANSI_YELLOW + "Value " + theValue + " is odd" + Colors.ANSI_RESET);

            Random rand = new Random();

            if (rand.nextBoolean()) {
                System.out.println(Colors.ANSI_YELLOW + "I DON'T LIKE IT" + Colors.ANSI_RESET);
                replyJson.addProperty("response", "BAD");
            }
            else {
                System.out.println(Colors.ANSI_YELLOW + "it's okay" + Colors.ANSI_RESET);
                replyJson.addProperty("response", "GOOD");
            }
        }

        Message reply = new Message(this.name, message.getSender(), Message.REPLY_TYPE, replyJson.toString());
        sendMessage(reply.getDestination(), reply, false);
    }

    private void writeToDisk() throws IOException {
        JsonObject diskInfo = new JsonObject();
        diskInfo.addProperty("node_name", this.name);
        Gson gson = new Gson();
        diskInfo.addProperty("block_chain", gson.toJson(blockChain));
        String fileName = "Node_" + this.name + "_blackChain.json";
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
        writer.write(diskInfo.toString());
        writer.close();
    }
}
