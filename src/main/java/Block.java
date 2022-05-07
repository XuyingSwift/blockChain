import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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
        StringBuilder data = new StringBuilder();
        data.append(String.valueOf(this.number));
        data.append(this.coinbase.toString());
        data.append(Arrays.toString(this.transactions));
        data.append(this.previous);

        while (!this.hash.substring(0, validPrefix.length()).equals(validPrefix)) {
            this.nonce++;
            this.hash = hashBlock(data.toString());
        }
    }

    private String hashBlock(String data) {
        String sha256hex = Hashing.sha256()
                .hashString(data, StandardCharsets.UTF_8)
                .toString();
        return sha256hex;
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
