import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public class StakeBlock {
    public static final String FIRST_HASH = "0".repeat(64);
    //    public static final String FIRST_HASH = "0".repeat(64);
    //    private int number;
    //    private long nonce;
    //    private Coinbase coinbase;
    //    private Transaction[] transactions;
    //    private String previous, hash;
    //    private final int coinbaseAmount = 100;
    //    private final int maxTransactions = 5;
    //    private boolean keepMining;

    private int number;
    private Transaction[] transactions;
    private final int reward = 100;
    private ArrayList<String> verifiers;
    private String signature;
    private String finalSignature;
    private String previous;
    private String hash;
    private StakePerson stakePerson;

    public StakeBlock(int number, Transaction[] transactions, String signature, String finalSignature, String stakePerson, int stakeAmount) {
        this.number = number;
        this.transactions = transactions;
        this.verifiers = new ArrayList<>();
        this.signature = signature;
        this.finalSignature = finalSignature;
        this.hash = null;
        this.previous = null;
        this.stakePerson = new StakePerson(stakePerson, stakeAmount);
    }

    public void signBlock(String validPrefix) {
        StringBuilder data = new StringBuilder();
        data.append(this.number);
        data.append(Arrays.toString(this.transactions));
        data.append(this.previous);
        // making the block hash
        while (this.hash == null) {
            StringBuilder finalData = new StringBuilder();
            finalData.append(data);
            this.hash = hashBlock(finalData.toString());
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

    public void setNumber(int number) {
        this.number = number;
    }

    public Transaction[] getTransactions() {
        return transactions;
    }

    public void setTransactions(Transaction[] transactions) {
        this.transactions = transactions;
    }

    public int getReward() {
        return reward;
    }

    public ArrayList<String> getVerifiers() {
        return verifiers;
    }

    public void setVerifiers(ArrayList<String> verifiers) {
        this.verifiers = verifiers;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getFinalSignature() {
        return finalSignature;
    }

    public void setFinalSignature(String finalSignature) {
        this.finalSignature = finalSignature;
    }

    public String getPrevious() {
        return previous;
    }

    public void setPrevious(String previous) {
        this.previous = previous;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public StakePerson getStakePerson() {
        return stakePerson;
    }
}
