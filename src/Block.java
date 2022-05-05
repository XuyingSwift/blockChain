public class Block {
    private int number;
    private long nonce;
    private Coinbase coinbase;
    private Transaction[] transactions;
    private String previous, hash;
    private final int coinbaseAmount = 100;
    private final int maxTransactions = 5;

    public Block(int number, String coinbasePerson, String previous) {
        this.number = number;
        this.coinbase = new Coinbase(coinbasePerson, coinbaseAmount);
        transactions = new Transaction[maxTransactions];
        this.previous = previous;
    }

    public void mineBlock(String validPrefix) {
        /* TODO:
         * -pick a new nonce
         * -hash everything together
         *      -number
         *      -nonce
         *      -coinbase
         *      -transactions
         *      -previous
         * -check if the hash starts with validPrefix
         * -this method will finish once it finds a nonce and valid hash
         */
    }

    public int getNumber() {
        return number;
    }

    public Transaction[] getTransactions() {
        return transactions;
    }

    public String getPrevious() {
        return previous;
    }

    public String getHash() {
        return hash;
    }
}
