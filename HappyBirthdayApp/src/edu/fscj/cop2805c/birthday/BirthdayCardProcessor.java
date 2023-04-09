// BirthdayCardProcessor.java
// D. Singletary
// 3/5/23
// Process birthday cards

// D. Singletary
// 3/29/23
// added network logging

package edu.fscj.cop2805c.birthday;

import edu.fscj.cop2805c.log.Logger;
import edu.fscj.cop2805c.message.*;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BirthdayCardProcessor extends Thread
        implements MessageProcessor, Logger<BirthdayCard> {

    private BirthdayCardLogger logger;
    private Socket logSocket;
    private ObjectOutputStream streamToServer;

    private ConcurrentLinkedQueue<BirthdayCard> safeQueue;
    private boolean stopped = false;

    public BirthdayCardProcessor(ConcurrentLinkedQueue<BirthdayCard> safeQueue) {
        this.safeQueue = safeQueue;

        logger = new BirthdayCardLogger();

        // wait for a bit to allow eventlog server to start
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ie) {
            System.out.println("sleep interrupted! " + ie);
        }

        try {
            logSocket = new Socket("localhost", BirthdayCardLogger.LOGPORT);
            System.out.println("connected to log server");

            streamToServer = new ObjectOutputStream(logSocket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        // start polling (invokes run(), below)
        this.start();
    }

    // remove messages from the queue and process them
    public void processMessages() {
        safeQueue.stream().forEach(e -> {
                // Do something with each element
                e = safeQueue.remove();
                System.out.print(e);
                log(e);
        });
    }

    // allow external client to stop us
    public void endProcessing() {
        this.stopped = true;
        try {
            logSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        interrupt();
    }

    // poll queue for cards
    public void run() {
        final int SLEEP_TIME = 1000; // ms
        while (true) {
            try {
                processMessages();
                Thread.sleep(SLEEP_TIME);
                System.out.println("polling");
            } catch (InterruptedException ie) {
                // see if we should exit
                if (this.stopped == true) {
                    System.out.println("poll thread received exit signal");
                    break;
                }
            }
        }
    }
    @Override
    public void log(BirthdayCard c) {
        String msg = ":greeting:" + c.getUser().getName();
        try {
            streamToServer.writeObject(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
