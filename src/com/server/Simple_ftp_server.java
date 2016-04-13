/**
 * Created by Muchen on 4/1/16.
 */

package com.server;

import java.util.*;
import java.io.*;
import java.net.*;

import com.Simple_ftp_helper;
import sun.awt.AWTAccessor;

public class Simple_ftp_server {

    private static int portNum = 14000;
    private static double errProb;
    private static String fileName;

    private static String filePath = "/Users/Muchen/Desktop/";

    /**
     * Experiment Parameters
     * Must be exactly same as in Client side.
     *
     * mss: Maximum Segment Size;
     * mssCount: Number of Segments;
     * lastSeg: Size of the last segment(less or equal than a MSS)
     */
    private static int mss = 8;
    private static int mssNum = 16;
    private static int lastSeg = 16;
    private static int header = 8;


    private static void listen() throws IOException{

        DatagramSocket server = new DatagramSocket(portNum);

        DatagramSocket replyACK = new DatagramSocket();

//        byte[] testCheck = { (byte) 0xed, 0x2A, 0x44, 0x10, 0x03, 0x30 };
//        System.out.println("Checksum: " + Simple_ftp_helper.compChecksum(testCheck));

        System.out.println("MSS: " + mss);
        System.out.println("mssNum: " + mssNum);
        System.out.println("lastSeg: " + lastSeg);

        byte[] tmp = new byte[header + mss];
        DatagramPacket tmpReceiver = new DatagramPacket(tmp, header + mss);

        //  Sequence Number start at 0;
        int expSequence = 0;

        //  Write receiving data to a file;
        FileOutputStream fileOut = new FileOutputStream(filePath + fileName, true);

        while(true){

            System.out.println("Listening");

            //  Receiving packet
            server.receive(tmpReceiver);
            tmp = tmpReceiver.getData();

            System.out.println("Packet Received!");

            //  Sequence Number Field
            byte[] tmpSeq = new byte[4];
            System.arraycopy(tmp, 0, tmpSeq, 0, 4);

            int currSequence = java.nio.ByteBuffer.wrap(tmpSeq).getInt();
            System.out.println("Receiving Sequence Number: " + currSequence);

            //  Generating Random number to decide whether the packet should be accepted.
            Random r = new Random();
            double randomValue = r.nextDouble();


            if(randomValue > errProb){

                // Tail field
                if(tmp[6] != 85 || tmp[7] != 85)

                    System.out.println("Packet Discard, Not a data packet! sequence number = " + currSequence);

                else {
                    //  If the packet if expected.
                    if (expSequence == currSequence) {
                        byte[] data = new byte[mss];
                        System.arraycopy(tmp, 8, data, 0, mss);

                        byte[] currCheck = Simple_ftp_helper.compChecksum(data);
                        boolean isCorrect = (currCheck[0] == tmp[4] && currCheck[1] == tmp[5]) ? true : false;

                        if(isCorrect)
                            System.out.println("Checksum Correct!");
                        else
                            System.out.println("Checksum Failed!");

//            fileOut.write(tmp);
//            fileOut.close();
//            System.out.println("File Closed !");

                    } else {
                        System.out.println("Packet Not Expected, sequence number = " + currSequence +
                                ", Expect sequence number = " + expSequence);
                    }
                }
            }
            else{
                System.out.println("Packet loss, sequence number = " + currSequence);
            }

            if(true)
                break;
        }

        System.out.println("File Transfer Complete !");

    }

    public static void main(String[] args){

//        For test
        String[] test = {"14000", "test", "-0.1"};
        args = test;

        if(args.length != 3){
            System.out.println("Input Parameters Error!");
            System.out.println("Simple_ftp_server port# file-name p");
            return;
        }

        portNum = Integer.parseInt(args[0]);
        fileName = args[1];
        errProb = Double.parseDouble(args[2]);

//        System.out.println("portNum: " + portNum);
//        System.out.println("fileName: " + fileName);
//        System.out.println("errProb: " + errProb);

        try{
            listen();
        }
        catch (IOException e){
            e.printStackTrace();
        }

    }

}
