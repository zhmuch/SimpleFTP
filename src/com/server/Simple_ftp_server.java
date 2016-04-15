/**
 * Created by Muchen on 4/1/16.
 */

package com.server;

import java.math.BigInteger;
import java.util.*;
import java.io.*;
import java.net.*;

import com.Simple_ftp_helper;


public class Simple_ftp_server {

    private static InetAddress serverAddr;
    private static int serverPort = 7735;      //Default Port Number;

    private static double errProb;
    private static String fileName;

    /**
     * Windows "D://xxx//xxx"
     */
    private static String filePath = "/Users/Muchen/Desktop/";

    private static InetAddress clientAddr;
    private static int clientPort = 7736;

    /**
     * Experiment Parameters
     * Must be exactly same as in Client side.
     *
     * mss: Maximum Segment Size;
     * mssCount: Number of Segments;
     * lastSeg: Size of the last segment(less or equal than a MSS);
     * header:  Size of header;
     */
    private static int mss = 8;
    private static int mssNum = 7;
    private static int lastSeg = 7;
    private static int header = 8;


    private static void listen() throws IOException{

        serverAddr = InetAddress.getLocalHost();
        clientAddr = InetAddress.getLocalHost();

        DatagramSocket server = new DatagramSocket(serverPort);
        DatagramSocket replyACK = new DatagramSocket();

//        System.out.println("MSS: " + mss);
//        System.out.println("mssNum: " + mssNum);
//        System.out.println("lastSeg: " + lastSeg);

        byte[] tmp = new byte[header + mss];
        DatagramPacket tmpReceiver = new DatagramPacket(tmp, header + mss);

        //  Sequence Number start at 0;
        int expSequence = 0;

        //  Write receiving data to a file;
        FileOutputStream fileOut = new FileOutputStream(filePath + fileName, true);

        System.out.println("Server is listening! InetAddress: " + serverAddr + ",   Port Number: " + serverPort);

        while(true){

            //  Receiving packet
            server.receive(tmpReceiver);
            tmp = tmpReceiver.getData();

            clientAddr = tmpReceiver.getAddress();
            System.out.println("clientAddr " + clientAddr);
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

                        byte[] data;
                        int dataSize;

                        if (currSequence < mssNum) {
                            data = new byte[mss];
                            dataSize = mss;
                        }
                        else{
                            data = new byte[lastSeg];
                            dataSize = lastSeg;
                        }
                        System.arraycopy(tmp, 8, data, 0, dataSize);

                        byte[] currCheck = Simple_ftp_helper.compChecksum(data);
                        boolean isCorrect = (currCheck[0] == tmp[4] && currCheck[1] == tmp[5]) ? true : false;

//                        System.out.println("data size: " + data.length);

                        if(isCorrect){
                            fileOut.write(data, 0, dataSize);
                            expSequence++;

                            DatagramPacket res = generateACK(tmp);
                            replyACK.send(res);
                        } else {
                            System.out.println("Packet Discard, Checksum not match! Sequence number = " + currSequence);
                        }

                    } else if(expSequence > currSequence) {
                        DatagramPacket res = generateACK(tmp);
                        replyACK.send(res);
                    } else {
                        System.out.println("Packet Discard, Not the expect sequence number! Sequence number = " + currSequence +
                                ", Expect sequence number = " + expSequence);
                    }
                }
            }
            else{
                System.out.println("Packet loss, sequence number = " + currSequence);
            }

            if(expSequence > mssNum){
                //  File transfer complete;
                fileOut.close();
                server.close();
                replyACK.close();

                break;
            }

        }

        System.out.println("File Transfer Complete !");

    }

    /**
     *
     * @param tmp
     * @return
     */
    private static DatagramPacket generateACK(byte[] tmp){

        byte[] resBytes = new byte[8];

        //  Sequence Number field;
        System.arraycopy(tmp, 0, resBytes, 0, 4);

        //  All zeros field;
        resBytes[4] = resBytes[5] = 0;

        //  1010101010101010 field;
        byte[] tail = new byte[2];
        tail[0] = tail[1] = new BigInteger("1010101010101010", 2).toByteArray()[1];
        System.arraycopy(tail, 0, resBytes, 6, 2);

        DatagramPacket res = new DatagramPacket(resBytes, resBytes.length, clientAddr, clientPort);

        return res;

    }

    public static void main(String[] args){

//        For test
        String[] test = {"7735", "test", "0.9"};
        args = test;

        if(args.length != 3){
            System.out.println("Input Parameters Error!");
            System.out.println("Simple_ftp_server port# file-name p");
            return;
        }

        serverPort = Integer.parseInt(args[0]);
        fileName = args[1];
        errProb = Double.parseDouble(args[2]);

//        System.out.println("portNum: " + serverPort);
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
