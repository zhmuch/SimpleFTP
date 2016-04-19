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

    private static InetAddress clientAddr;
    private static String clientAddrString = "192.168.1.5";
    private static int clientPort = 7736;

    private static double errProb;
    private static String fileName;
    private static String filePath = "/Users/Muchen/Desktop/";

    /**
     * Experiment Parameters
     * Must be exactly same as in Client side.
     *
     * mss: Maximum Segment Size;
     * mssCount: Number of Segments;
     * lastSeg: Size of the last segment(less or equal than a MSS);
     * header:  Size of header;
     */
    private static int mss = 0;
    private static int mssNum = 0;
    private static int lastSeg = 0;

    private static int header = 8;


    private static void listen() throws IOException{

        serverAddr = InetAddress.getLocalHost();
        clientAddr = InetAddress.getByName(clientAddrString);

        DatagramSocket server = new DatagramSocket(serverPort);
        DatagramSocket replyACK = new DatagramSocket();

        //  Sequence Number start at 0;
        int expSequence = 0;

        //  Write receiving data to a file;
        FileOutputStream fileOut = new FileOutputStream(filePath + fileName, true);

        System.out.println("Server is listening! InetAddress: " + serverAddr + ",   Port Number: " + serverPort);

        //  Agreement about mss, mssNum, lastSeg;
        byte[] bootInfo = new byte[12];
        DatagramPacket bootReceiver = new DatagramPacket(bootInfo, 12);
        server.receive(bootReceiver);

//        System.out.println("bootInfo get!");

        bootInfo = bootReceiver.getData();

        byte[] mssBytes = new byte[4];
        System.arraycopy(bootInfo, 0, mssBytes, 0, 4);
        mss = java.nio.ByteBuffer.wrap(mssBytes).getInt();

        byte[] mssNumBytes = new byte[4];
        System.arraycopy(bootInfo, 4, mssNumBytes, 0, 4);
        mssNum = java.nio.ByteBuffer.wrap(mssNumBytes).getInt();

        byte[] lastSegBytes = new byte[4];
        System.arraycopy(bootInfo, 8, lastSegBytes, 0, 4);
        lastSeg = java.nio.ByteBuffer.wrap(lastSegBytes).getInt();

        //  ACK back to client.
        DatagramPacket bootRes = generateACK(mssBytes);
        replyACK.send(bootRes);

        System.out.println("Successfully connect to client at " + bootReceiver.getAddress() + " !");
//        System.out.println("mss: " + mss);
//        System.out.println("mssNum: " + mssNum);
//        System.out.println("lastSeq: " + lastSeg);


        //  Receiving Datagram;
        byte[] tmp = new byte[header + mss];
        DatagramPacket tmpReceiver = new DatagramPacket(tmp, header + mss);

        //  error count initialization;
        int errCount = 0;
        Random r = new Random();

        while(true){

            //  Receiving packet
            server.receive(tmpReceiver);
            tmp = tmpReceiver.getData();
//            System.out.println("tmp.size: " + tmp.length);

            clientAddr = tmpReceiver.getAddress();
//            System.out.println("clientAddr " + clientAddr);

            //  Sequence Number Field
            byte[] tmpSeq = new byte[4];
            System.arraycopy(tmp, 0, tmpSeq, 0, 4);

            int currSequence = java.nio.ByteBuffer.wrap(tmpSeq).getInt();
//            System.out.println("Receiving Sequence Number: " + currSequence);

            //  Generating Random number to decide whether the packet should be accepted.
            double randomValue = r.nextDouble();

            if(randomValue > errProb){

                // Tail field
//                if(tmp[6] != 85 || tmp[7] != 85)
//
//                    System.out.println("Packet Discard, Not a data packet! sequence number = " + currSequence);

//                else {

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
//                        System.out.println("Packet Discard, Not the expect sequence number! Sequence number = " + currSequence +
//                                ", Expect sequence number = " + expSequence);
                    }
//                }
            }
            else{
                System.out.println("Packet loss, sequence number = " + currSequence);
                errCount++;
            }

            if(expSequence > mssNum){
                //  File transfer complete;
                fileOut.close();
                server.close();
                replyACK.close();

                System.out.println("errCount: " + errCount);

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
        String[] test = {"7735", "test", "0.05"};
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
