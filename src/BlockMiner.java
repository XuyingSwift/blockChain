public class BlockMiner extends Thread{
    public static final String WAITING = "WAITING", READY = "READY";
    private Block block;
    private volatile String blockState;
    private final String hashPrefix = "000";

    public BlockMiner(Block block) {
        this.block = block;
        this.blockState = WAITING;
    }

    public void run() {
        this.block.mineBlock(hashPrefix);
        this.blockState = READY;
    }

    public Block getBlock() { return this.block; }
    public String getBlockState() { return this.blockState; }
}
