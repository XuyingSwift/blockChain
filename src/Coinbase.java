public class Coinbase {
    String person;
    int amount;

    public Coinbase(String person, int amount) {
        this.person = person;
        this.amount = amount;
    }

    public String getPerson() {
        return person;
    }

    public int getAmount() {
        return amount;
    }

    public String toString() {
        return this.person + "|" + this.amount;
    }
}
