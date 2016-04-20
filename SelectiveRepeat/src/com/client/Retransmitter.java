package com.client;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Muchen on 4/20/16.
 */
public class Retransmitter extends TimerTask {

    private int sequ;

    public Retransmitter(int i) {
        this.sequ = i;
    }

    @Override
    public void run(){

        System.out.println("Timeout, sequence number: " + sequ);

        Simple_ftp_client.cancel(sequ);

        try {

//            System.out.println("Without Exception");
            Simple_ftp_client.rdt_send(Simple_ftp_client.fileBytes.get(sequ), sequ);

        } catch (IOException e){
            e.printStackTrace();
        }

        Simple_ftp_client.setTimer(sequ);

    }
}
