import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server extends Thread {
    public final static String ACK = "ACK";
    private ServerSocket server;
    private boolean running;
    private volatile ArrayList<MessageHolder> messageHolders;

    public Server(int port) {
        try {
            this.server = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.messageHolders = new ArrayList<>();
        this.running = false;
    }

    public void stopServer() {
        try {
            this.server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.running = false;
        this.interrupt();
    }

    public void run() {
        System.out.println(Colors.ANSI_PURPLE + "* Started server on port " + server.getLocalPort() + " to listen for messages" + Colors.ANSI_RESET);
        running = true;

        while(running) {
            try {
                Socket socket = server.accept();
                //start a thread to handle receiving the message
                MessageHolder newHolder = new MessageHolder(socket);
                newHolder.start();

                synchronized(messageHolders) {
                    messageHolders.add(newHolder);
                }
            } catch (IOException ioException) {
                System.out.println(Colors.ANSI_PURPLE + "* Closing server socket..." + Colors.ANSI_RESET);
            }
        }
    }

    public MessageHolder getNextReadyHolder() {
        MessageHolder nextReady = null;
        int i = 0, max;

        synchronized (messageHolders) { max = messageHolders.size(); }

        while (i < max && nextReady == null) {
            if (messageHolders.get(i).getMessageState().equals(MessageHolder.READY)) {
                nextReady = messageHolders.get(i);
            }
            i++;
        }

        if (nextReady != null) {
            synchronized (messageHolders) { messageHolders.remove(i - 1); }
        }

        return nextReady;
    }

    public int getMessageHolderCount() { return messageHolders.size(); }
}
