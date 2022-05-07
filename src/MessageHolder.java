import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.HashMap;

public class MessageHolder extends Thread{
    public static final String WAIT = "WAIT", READY = "READY";
    private Socket socket;
    private volatile String messageState;
    private volatile Message message;

    public MessageHolder(Socket socket) {
        this.socket = socket;
        this.messageState = WAIT;
    }

    public void run() {
        System.out.println(Colors.ANSI_PURPLE + "* Another node connected..." + Colors.ANSI_RESET);

        try {
            BufferedReader socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String msg = socketIn.readLine();
            String messageJson = null;

            while (msg != null && msg.length() > 0) {
                if (messageJson == null) messageJson = msg;
                else messageJson += msg;

                msg = socketIn.readLine();
            }

            socketIn.close();
            socket.close();

            Gson gson = new Gson();
            this.message = gson.fromJson(messageJson, Message.class);
            this.messageState = READY;
            System.out.println(Colors.ANSI_PURPLE + "MessageHolder (" + Thread.currentThread().getName() + "): Ready with " + message.getType() + " message [" + message.getGuid() + "] from " + message.getSender() + " waiting to deliver..." + Colors.ANSI_RESET);
        } catch (IOException e) {
            System.out.println(Colors.ANSI_RED + "WARNING MessagePasser (" + Thread.currentThread().getName() + "): Communication failed" + Colors.ANSI_RESET);
            //e.printStackTrace();
        }
    }

    public String getMessageState() { return messageState; }

    public Message getMessage() { return message; }
}
