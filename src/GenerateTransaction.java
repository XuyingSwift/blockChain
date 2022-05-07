import java.lang.reflect.Array;
import java.util.*;

public class GenerateTransaction {
    private HashMap<String, Integer> transactions;

    public GenerateTransaction(HashMap<String, Integer> transactions) {
        this.transactions = transactions;
    }

    public HashMap<String, Integer> getTransactions() {
        return transactions;
    }

    public Transaction[] generateTransaction() {
        int min = 2;
        int max = 5;
        Random random = new Random();
        int size = random.nextInt(max - min) + min;
        Transaction[] transactionList = new Transaction[size];
        String[] nodes = this.transactions.keySet().toArray(new String[0]);
        String nodeName = randomNode(nodes);
        int nodeValue = this.transactions.get(nodeName);

        if (nodeValue > 0) {
            this.transactions.remove(nodeName);

            Iterator txIterator = this.transactions.entrySet().iterator();
            int index = 0;
            while (txIterator.hasNext()) {
                Map.Entry current = (Map.Entry) txIterator.next();
                Transaction trx = new Transaction(nodeName, (String) current.getKey(), (int) (nodeValue*(10.0f/100.0f)));
                transactionList[index] = trx;
                index++;
            }
        }else {
            System.out.println(nodeName + " does not have enough money!");
        }
        return transactionList;
    }

    private String randomNode(String[] arr) {
        Random rand = new Random();
        int index = rand.nextInt(arr.length);
        String item = arr[index];
        return item;
    }
}
