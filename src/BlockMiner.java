public class BlockMiner extends Thread{
    public static final String WAITING = "WAITING", READY = "READY";
    private volatile Block block;
    private volatile String blockState;
    private final String hashPrefix = "000000";

    public BlockMiner() {
        this.blockState = WAITING;
        this.block = null;
    }

    public void run() {
        this.blockState = WAITING;
        if (this.block != null) {
            System.out.println(Colors.ANSI_CYAN + "BlockMiner (" + Thread.currentThread().getName() + "): Mining block " + block.getNumber() + Colors.ANSI_RESET);
            this.block.mineBlock(hashPrefix);

            if (!this.isInterrupted()) {
                this.blockState = READY;
                System.out.println(Colors.ANSI_YELLOW + "BlockMiner (" + Thread.currentThread().getName() + "): Finished mining block " + block.getNumber() + "[..." + block.getHash().substring(57) + "]" + Colors.ANSI_RESET);
            }
            else {
                clearBlock();
            }
        }
        else {
            System.out.println(Colors.ANSI_RED + "ERROR BlockMiner (" + Thread.currentThread().getName() + "): cannot mine NULL block");
        }
    }

    public void setBlock(Block block) { this.block = block; }
    public void interrupt() {
        if (this.block != null) this.block.stopMining();
        super.interrupt();
    }
    public void clearBlock() {
        this.block = null;
        this.blockState = WAITING;
    }
    public Block getBlock() { return this.block; }
    public String getBlockState() { return this.blockState; }
}
