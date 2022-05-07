public class Transaction {
    String from, to;
    int amount;

    public Transaction(String from, String to, int amount) {
        this.from = from;
        this.to = to;
        this.amount = amount;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public int getAmount() {
        return amount;
    }

    public String toString() {
        return this.from + "|" + this.to + "|" + this.amount;
    }
}
