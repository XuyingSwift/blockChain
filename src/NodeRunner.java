import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

public class NodeRunner {
    public static void main(String[] args) {
        //config string format: "myName myName 127.0.0.1 5000 remote_1 127.0.0.1 5001 remote_2 127.0.0.1 5002", ...
        String myName = args[0];
        HashMap<String, RemoteNode> remoteNodes = buildRemoteList(args);
        System.out.println(myName);
        int port = remoteNodes.get(myName).getPort();

        Node node = new Node(myName, port, remoteNodes);

        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Press <enter> to continue...");
        try {
            input.readLine();
            System.out.print("Starting in ");
            for (int i = 3; i >= 1; i--) {
                System.out.print(i + "..");
                Thread.sleep(1000);
            }
            System.out.print(System.lineSeparator());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        node.run();

        System.out.println("Press <enter> to quit...");
        try {
            input.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println(Colors.ANSI_PURPLE + "* Shut down server");
        System.out.println("*" + Colors.ANSI_RESET);
    }

    private static HashMap<String, RemoteNode> buildRemoteList(String[] config) {
        HashMap<String, RemoteNode> remotes = new HashMap<>();

        int idx = 1;
        while (idx < config.length) {
            String curId = config[idx];
            idx++;
            String curAddress = config[idx];
            idx++;
            int curPort = Integer.parseInt(config[idx]);
            idx++;

            remotes.put(curId, new RemoteNode(curId, curAddress, curPort));
        }

        return remotes;
    }
}
