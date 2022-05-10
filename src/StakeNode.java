import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class StakeNode {
    //node states
    private final String FOLLOW = "FOLLOWER", CANDID = "CANDIDATE", LEADER = "LEADER";
    //field names for request vote message
    public static final String CANDIDATE_ID = "candidateId", CANDIDATE_TERM = "candidateTerm",
            LAST_BLOCK_INDEX = "lastBlockIndex", LAST_BLOCK_TERM = "lastBlockTerm";
    //field names for heartbeat message
    public static final String LEADER_TERM = "leaderTerm", LEADER_ID = "leaderId";
    private final int HEARTBEAT_TIME = 100 * 10, BLOCK_PERIOD = 15000, MAJORITY;
    private String name;
    private HashMap<String, Block> blockChain;
    private HashMap<String, RemoteNode> remoteNodes;
    private Block longestChainHead;
    private Server server;
    private HashMap<UUID, Message> awaitingReplies;
    private ArrayList<Client> openClients;
    private ElectionTimer timer;
    private Integer voteCount, term, commitIndex;
    private String state, votedFor, currentLeader;
    private HashMap<Integer, Integer> blockTerms;
    private long blockPeriodStart;

    public StakeNode(String name, int port, HashMap<String, RemoteNode> remoteNodes) {
        this.name = name;
        this.blockChain = new HashMap<>();
        this.longestChainHead = null;
        this.remoteNodes = remoteNodes;
        this.awaitingReplies = new HashMap<>();
        this.openClients = new ArrayList<>();
        this.server = new Server(port);

        this.timer = new ElectionTimer();
        this.MAJORITY = (int) Math.ceil(remoteNodes.size() / 2.0) + (remoteNodes.size() % 2 == 0 ? 1 : 0);
        this.term = 0;
        this.voteCount = 0;
        this.state = FOLLOW;
        this.currentLeader = null;
        this.votedFor = null;
        this.blockTerms = new HashMap<>();
    }

    public void run() {
        this.server.start();
        this.timer.start();
        MessageHolder nextHolder;
        long lastHeartbeat = System.nanoTime();

        while (true) {
            if (this.state.equals(CANDID) && this.voteCount >= MAJORITY) {
                becomeLeader();
                //TODO: make a new block
            }

            if (state.equals(LEADER) && ((System.nanoTime() - lastHeartbeat) / 1000000) >= HEARTBEAT_TIME)
            {
                sendHeartbeat();
                lastHeartbeat = System.nanoTime();
            }

            nextHolder = this.server.getNextReadyHolder();
            while (nextHolder != null) {
                deliverMessage(nextHolder.getMessage());
                nextHolder = server.getNextReadyHolder();
            }

            if (((System.nanoTime() - blockPeriodStart) / 1000000) >= BLOCK_PERIOD && this.state.equals(LEADER)) {
                System.out.println(Colors.ANSI_YELLOW + "StakeNode (" + Thread.currentThread().getName() + "): current block period has expired... " + Colors.ANSI_RESET);
                this.timer.reset();
                //stop sending heartbeats and allow timers to expire if it's time to make another block
                this.state = FOLLOW;
            }
            if (this.timer.isExpired() && !this.state.equals(LEADER)) startElection();

            cleanClients();
        }
    }

    private void startElection() {
        // switch to candidate state
        this.state = CANDID;
        // increment its term
        this.term++;
        System.out.println(Colors.ANSI_YELLOW + "StakeNode (" + Thread.currentThread().getName() + "): became candidate in term " + term + Colors.ANSI_RESET);
        //start with vote for self
        this.voteCount = 1;
        // set voted for to the candidate id
        this.votedFor = name;
        // reset the term timer
        this.timer.reset();

        //send a requestVote to all other nodes
        for (String remoteNode : this.remoteNodes.keySet()) {
            if (remoteNode.equals(this.name)) continue;
            sendRequestVote(remoteNode);
        }
    }

    private void becomeLeader() {
        if (this.state.equals(CANDID)) {
            this.state = LEADER;
            this.currentLeader = this.name;

            System.out.println(Colors.ANSI_YELLOW + "StakeNode (" + Thread.currentThread().getName() + "): became the leader in term " + term + "!!" + Colors.ANSI_RESET);
            blockPeriodStart = System.nanoTime();
            sendHeartbeat();
        }
        else {
            System.out.println(Colors.ANSI_CYAN + "StakeNode (" + Thread.currentThread().getName() + "): was trying to become leader but found a new leader" + Colors.ANSI_RESET);
        }
    }

    /*private void broadcastBlock(Block block) {
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
    }*/

    private void processBlockMessage(Message message) {
        Gson gson = new Gson();
        Block newBlock = gson.fromJson(message.getPayload(), Block.class);
        //addBlock(newBlock);
    }

    private void sendHeartbeat() {
        JsonObject heartbeatInfo = new JsonObject();
        heartbeatInfo.addProperty(LEADER_TERM, this.term);
        heartbeatInfo.addProperty(LEADER_ID, this.name);

        for (String remoteNode : remoteNodes.keySet()) {
            if (remoteNode.equals(name)) continue;
            Message message = new Message(this.name, remoteNode, Message.HEARTBEAT_TYPE, heartbeatInfo.toString());
            sendMessage(remoteNode, message, false);
        }
    }

    private void processHeartbeatMessage(Message message) {
        JsonObject payloadJson = new JsonParser().parse(message.getPayload()).getAsJsonObject();

        if (payloadJson.get(LEADER_TERM).getAsInt() >= this.term) {
            this.timer.reset();
            this.currentLeader = message.getSender();
        }

        if (payloadJson.get(LEADER_TERM).getAsInt() > term) {
            if (!state.equals(FOLLOW)) {
                System.out.println(Colors.ANSI_YELLOW + "StakeNode (" + Thread.currentThread().getName() + "): switching to follower, new term " + payloadJson.get(LEADER_TERM).getAsInt() + " from node " + message.getSender() + " greater than my term " + term + Colors.ANSI_RESET);
            }
            state = FOLLOW;
            term = payloadJson.get(LEADER_TERM).getAsInt();
            votedFor = null;
        }
    }

    private void sendRequestVote(String dest) {
        JsonObject voteInfo = new JsonObject();

        // candidate requesting vote
        voteInfo.addProperty(CANDIDATE_ID, name);
        // candidate’s term
        voteInfo.addProperty(CANDIDATE_TERM, term);
        //last index of candidate's log
        voteInfo.addProperty(LAST_BLOCK_INDEX, longestChainHead == null ? 0 : longestChainHead.getNumber());
        //term of candidates last log entry
        if (longestChainHead != null && blockTerms.containsKey(longestChainHead.getNumber())) {
            voteInfo.addProperty(LAST_BLOCK_TERM, blockTerms.get(longestChainHead.getNumber()));
        }
        else {
            voteInfo.addProperty(LAST_BLOCK_TERM, (Integer) null);
        }

        Message message = new Message(name, dest, Message.REQ_VOTE_TYPE, voteInfo.toString());

        System.out.println(Colors.ANSI_CYAN + "StakeNode (" + Thread.currentThread().getName() + "): Sending request vote message [" + message.getGuid() + "] to node " + dest + Colors.ANSI_RESET);
        System.out.println(Colors.ANSI_CYAN + "     " + message.getPayload() + Colors.ANSI_RESET);
        sendMessage(dest, message, true);
    }

    private void processReqVoteMessage(Message message) {
        JsonObject responseJson = new JsonObject();
        JsonObject payloadJson = new JsonParser().parse(message.getPayload()).getAsJsonObject();

        if (payloadJson.get(CANDIDATE_TERM).getAsInt() > term) {
            this.timer.reset();

            if (!this.state.equals(FOLLOW)) {
                System.out.println(Colors.ANSI_YELLOW + "StakeNode (" + Thread.currentThread().getName() + "): switching to follower, new term " + payloadJson.get(CANDIDATE_TERM).getAsInt() + " from node " + message.getSender() + " greater than my term " + term + Colors.ANSI_RESET);
            }
            this.state = FOLLOW;
            this.term = payloadJson.get(CANDIDATE_TERM).getAsInt();
            this.votedFor = null;
        }

        //if the sending node's term is at least as high as my term
        //and either I haven't voted yet, or I already voted for this node,
        //maybe grant vote
        if (payloadJson.get(CANDIDATE_TERM).getAsInt() >= this.term
                && (this.votedFor == null || this.votedFor.equals(payloadJson.get(CANDIDATE_ID).getAsString()))) {

            boolean logIsUpToDate;
            //check if candidate's log is as up to date as mine
            if (this.longestChainHead == null) {
                logIsUpToDate = true; //I have no logs yet
            }
            else if (payloadJson.get(LAST_BLOCK_INDEX).getAsInt() == 0) {
                logIsUpToDate = false; //Candidate has no logs, but I do
            }
            else { //me and the candidate both have logs
                if (payloadJson.get(LAST_BLOCK_TERM).getAsInt() > blockTerms.get(longestChainHead.getNumber())) {
                    logIsUpToDate = true;
                }
                else if (payloadJson.get(LAST_BLOCK_TERM).getAsInt() < blockTerms.get(longestChainHead.getNumber())) {
                    logIsUpToDate = false;
                }
                else { //terms are equal - whose log is longer?
                    logIsUpToDate = payloadJson.get(LAST_BLOCK_INDEX).getAsInt() >= longestChainHead.getNumber();
                }
            }

            if (logIsUpToDate) {
                responseJson.addProperty("result", true);
                this.votedFor = payloadJson.get(CANDIDATE_ID).getAsString();
            }
            else {
                responseJson.addProperty("result", false);
            }
        }
        else {
            responseJson.addProperty("result", false);
        }

        responseJson.addProperty("originalMessageId", message.getGuid().toString());
        responseJson.addProperty("voteTerm", this.term);
        Message response = new Message(this.name, message.getSender(), Message.REPLY_TYPE, responseJson.toString());
        sendMessage(message.getSender(), response, false);
    }

    private void processReqVoteReply(Message message) {
        JsonObject replyJson = new JsonParser().parse(message.getPayload()).getAsJsonObject();
        if (replyJson.get("result").getAsBoolean() && replyJson.get("voteTerm").getAsInt() == this.term) {
            voteCount++;
        }
    }

    private void sendMessage(String dest, Message message, boolean waitForReply) {
        if (waitForReply) { this.awaitingReplies.put(message.getGuid(), message); }

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
            //if (origMessage.getType().equals(Message.TEST_TYPE))
            if (origMessage.getType().equals(Message.REQ_VOTE_TYPE)) {
                processReqVoteReply(message);
            }
        }
        else if (message.getType().equals(Message.BLOCK_TYPE)) {
            processBlockMessage(message);
        }
        else if (message.getType().equals(Message.REQ_VOTE_TYPE)) {
            processReqVoteMessage(message);
        }
        else if (message.getType().equals(Message.HEARTBEAT_TYPE)) {
            processHeartbeatMessage(message);
        }
    }

    /*
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
    */

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
