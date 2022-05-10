import java.util.Random;

public class ElectionTimer extends Thread{
    volatile private int timeout;
    private final int MIN_TIMEOUT = 150 * 100, MAX_TIMEOUT = 300 * 100;
    volatile private boolean expired;

    public ElectionTimer() {
        reset();
    }

    public void run() {
        while (true) {
            System.out.println(Colors.ANSI_BLUE + "Timer (" + Thread.currentThread().getName() + "): start " + timeout + Colors.ANSI_RESET);
            try {
                System.out.println(Colors.ANSI_BLUE + "Timer (" + Thread.currentThread().getName() + "): sleeping...." + Colors.ANSI_RESET);
                Thread.sleep(timeout);
                System.out.println(Colors.ANSI_BLUE + "Timer (" + Thread.currentThread().getName() + "): expired" + Colors.ANSI_RESET);
                expired = true;
            } catch (InterruptedException e) {
                //e.printStackTrace();
            }

            //hold here until receiving a "reset" command that set expired back to FALSE
            while (expired) {
                Thread.onSpinWait();
            }
        }
    }

    synchronized public void reset() {
        if (timeout != 0) this.interrupt(); //do not interrupt if called from constructor
        Random rand = new Random();
        timeout = rand.nextInt((MAX_TIMEOUT - MIN_TIMEOUT) + 1) + MIN_TIMEOUT;
        System.out.println(Colors.ANSI_BLUE + ">>>Timer (" + Thread.currentThread().getName() + "): reset to " + timeout + Colors.ANSI_RESET);
        expired = false;
    }

    public boolean isExpired() { return expired; }
}
