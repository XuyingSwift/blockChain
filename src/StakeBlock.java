import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
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
    public static final int BASE_REWARD = 100;
    private int reward;
    private ArrayList<String> verifiers;
    private String signature;
    private String finalSignature;
    private String previous;
    private String hash;
    private StakePerson stakePerson;

    public StakeBlock(int number, String stakePerson, int stakeAmount, String previous) {
        this.number = number;
        this.transactions = null;
        this.verifiers = new ArrayList<>();
        this.hash = null;
        this.previous = previous;
        this.stakePerson = new StakePerson(stakePerson, stakeAmount);
        reward = number * BASE_REWARD;
    }

    public void setStakePerson(StakePerson stakePerson) {
        this.stakePerson = stakePerson;
    }

    /*
    public void getBlockData() {
        StringBuilder data = new StringBuilder();
        data.append(this.number);
        data.append(Arrays.toString(this.transactions));
        data.append(this.previous);
        // making the block hash
        while (this.hash == null) {
            this.hash = hashBlock(data.toString());
        }
        data.append(this.hash);
    }*/

    public void makeBlockHash() {
        StringBuilder data = new StringBuilder();
        data.append(this.number);
        data.append(Arrays.toString(this.transactions));
        data.append(this.previous);
        // making the block hash
        this.hash = hashBlock(data.toString());
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

    public int getReward() {
        return this.reward;
    }

    public Transaction[] getTransactions() {
        return transactions;
    }

    public void setTransactions(Transaction[] transactions) {
        this.transactions = transactions;
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
