import java.util.ArrayList;
import java.util.HashMap;

public class Node {
    private String name;
    private HashMap<String, Block> blockChain;
    private Block longestChainHead;

    public Node(String name) {
        this.name = name;
        this.longestChainHead = null;
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
}
