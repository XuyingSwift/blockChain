import java.util.ArrayList;
import java.util.HashMap;

public class Node {
    String name;
    HashMap<String, Block> blockChain;
    Block longestChain;

    public Node(String name) {
        this.name = name;
        longestChain = null;
    }

    private void addBlock(Block block) {
        blockChain.put(block.getHash(), block);
        if (longestChain == null || block.getNumber() > longestChain.getNumber()) {
            longestChain = block;
        }
    }

    private void verifyBlock(Block block) {
        ArrayList<Block> startChain = new ArrayList<>();
        startChain.add(block);
        ArrayList<Block> totalChain = findChain(startChain);

        //TODO: reverse the list totalChain (or work through it backwards)
        //TODO: go through each transaction from each block and make sure no one runs out of money
    }

    private ArrayList<Block> findChain(ArrayList<Block> chain) {
        Block lastBlock = chain.get(chain.size() - 1);
        String previous = lastBlock.getPrevious();

        if (previous.equals("0000000000")) { //TODO: what is the hash value for the first block?
            return chain;
        }
        else {
            chain.add(blockChain.get(previous));
            return findChain(chain);
        }
    }
}
