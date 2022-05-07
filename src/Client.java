import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

public class Client extends Thread{
    public static final String DONE = "DONE", WAITING = "WAITING", EXCEPTION = "EXCEPTION";
    private volatile String messageState;
    private String address;
    private int port;
    private Message message;

    public Client(String address, int port, Message message) {
        this.address = address;
        this.port = port;
        this.message = message;
        this.messageState = WAITING;
    }

    public void run() {
        try {
            Socket socket = new Socket(address, port);
            System.out.println(Colors.ANSI_GREEN + "Client (" + Thread.currentThread().getName() + "): Connection made to " + address + ":" + port + Colors.ANSI_RESET);

            PrintStream socketOut = new PrintStream(socket.getOutputStream());

            Gson gson = new Gson();
            String messageJson = gson.toJson(message);
            socketOut.println(messageJson);
            socketOut.println();

            socketOut.close();
            socket.close();

            this.messageState = DONE;
            System.out.println(Colors.ANSI_GREEN + "Client (" + Thread.currentThread().getName() + "): Sent " + message.getType() + " message [" + message.getGuid() + "] to " + message.getDestination() + Colors.ANSI_RESET);
        } catch (IOException e) {
            System.out.println(Colors.ANSI_RED + "WARNING Client (" + Thread.currentThread().getName() + "): Communication failed with node " + message.getDestination() + Colors.ANSI_RESET);
            this.messageState = EXCEPTION;
            //e.printStackTrace();
        }
    }

    public Message getMessage() { return message; }

    public String getMessageState() { return messageState; }
}