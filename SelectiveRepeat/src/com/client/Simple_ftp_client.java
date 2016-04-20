package com.client;


import com.Simple_ftp_helper;

import java.math.BigInteger;
import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.*;

/**
 * Created by Muchen on 4/1/16.
 */


public class Simple_ftp_client {

    private static DatagramSocket sendData;
    private static DatagramSocket receiveACK;

    private static InetAddress clientAddr;
    private static int clientPort = 7736;

    private static String serverAddrString;
    private static InetAddress serverAddr;
    private static int serverPort;

    private static int mss;
    private static int winSize;

    private static long fileSize;
    private static long mssNum;
    private static int lastSeg;
    private static int header = 8;
    protected static ArrayList<byte[]> fileBytes;

    private static boolean[] ack;
    private static int unAck;

    /**
     * Windows "D://xxx//xxx"
     */
    private static String fileToSend = "/Users/Muchen/Desktop/";
    private static int leftSeqNum;
    private static int rightSeqNum;

    volatile static Timer[] timers;
    volatile static Retransmitter[] retransmitters;
    private static int timeOut = 1000;

    private static long beginTime;
    private static long endTime;

    /**
     *
     * @param data
     * @param currSequence
     * @throws IOException
     */
    protected static void rdt_send(byte[] data, int currSequence) throws IOException {

//		System.out.println("Sending! data length: " + data.length +
//				" Sequence: " + currSequence);

        //	32-bit Sequence Number
        ByteArrayOutputStream tmpBytes = new ByteArrayOutputStream();
        DataOutputStream tmpOut = new DataOutputStream(tmpBytes);
        tmpOut.writeInt(currSequence);
        byte[] tmpSeq = tmpBytes.toByteArray();

        //	16-bit checksum
        byte[] tmpCheck = Simple_ftp_helper.compChecksum(data);
//		System.out.print(tmpCheck[0] + " " + tmpCheck[1]);
//		System.out.println();

        //	16-bit 0101010101010101
        byte[] tail = new BigInteger("0101010101010101", 2).toByteArray();

        //	Packet bytes.
        byte[] sendBytes = new byte[data.length + 8];

        System.arraycopy(tmpSeq, 0, sendBytes, 0, 4);
        System.arraycopy(tmpCheck, 0, sendBytes, 4, 2);
        System.arraycopy(tail, 0, sendBytes, 6, 2);
        System.arraycopy(data, 0, sendBytes, 8, data.length);

//		System.out.println(currSequence + "th sending data size is: " + sendBytes.length);

        DatagramPacket p = new DatagramPacket(sendBytes, sendBytes.length, serverAddr, serverPort);
        sendData.send(p);

    }

    private static void transmit() throws IOException{

        //	Read and buffer file data;
        File file = new File(fileToSend);
        fileSize = file.length();
        mssNum = fileSize / mss;
        lastSeg = (int) (fileSize - mss * mssNum);
        FileInputStream fileInput = new FileInputStream(file);

        System.out.println("fileSize: " + fileSize);
//		System.out.println("mss: " + mss);
        System.out.println("mssNum: " + mssNum);
        System.out.println("lastSeq: " + lastSeg);

        ack = new boolean[(int)mssNum + 1];
        unAck = (int)mssNum + 1;

        //	Buffering data;
        fileBytes = new ArrayList<>();
        for(int i = 0; i < mssNum; i++){
            byte[] tmp = new byte[mss];
            fileInput.read(tmp, 0, mss);
            fileBytes.add(tmp);
        }
        byte[]tmp = new byte[lastSeg];
        fileInput.read(tmp, 0, lastSeg);
        fileBytes.add(tmp);

        if(fileBytes.size() != mssNum + 1){
            System.out.println("FileBuffer Error!");
            System.exit(0);
        }
        else
            System.out.println("FileBuffer Finish!");
        fileInput.close();

        //  Agreement about mss, mssNum, lastSeg;
        ByteArrayOutputStream bootBytes = new ByteArrayOutputStream();
        DataOutputStream bootOut = new DataOutputStream(bootBytes);
        bootOut.writeInt(mss);
        bootOut.writeInt((int) mssNum);
        bootOut.writeInt(lastSeg);

        byte[] bootOutBytes = bootBytes.toByteArray();
        DatagramPacket bootInfo = new DatagramPacket(bootOutBytes, bootOutBytes.length, serverAddr, serverPort);
        sendData.send(bootInfo);

        //	Check if server is online and agree with parameters;
        byte[] bootAckByte = new byte[8];
        DatagramPacket bootAckReceiver = new DatagramPacket(bootAckByte, 8);
        receiveACK.receive(bootAckReceiver);
        bootAckByte = bootAckReceiver.getData();

        byte[] bootAckMss = new byte[4];
        System.arraycopy(bootAckByte, 0, bootAckMss, 0, 4);
        int bootMss = java.nio.ByteBuffer.wrap(bootAckMss).getInt();

        if(bootMss == mss) {

            System.out.println("Successfully connect to server at " + serverAddr + " !");
            System.out.println("Using Selective-Repeat. Type in anything to start...");

            BufferedReader keybd = new BufferedReader(new InputStreamReader(System.in));
            String keybdi = keybd.readLine();

            //	Start ACKs Receiver;
            Receiver receiver = new Receiver();
            receiver.run();

        } else {
            System.out.println("Info not match, please restart !");
        }

        System.exit(0);

    }

    public static void main(String[] args) throws IOException {

        //        For test
        String[] test = {"10.139.85.19", "7735", "send", "64", "500"};
        args = test;

        if(args.length != 5){
            System.out.println("Input Parameters Error!");
            System.out.println("Simple_ftp_server port# file-name p");
            return;
        }

        serverAddrString = args[0];
        serverPort = Integer.parseInt(args[1]);
        String fileName = args[2];
        fileToSend = fileToSend + fileName;
        winSize = Integer.parseInt(args[3]);
        mss = Integer.parseInt(args[4]);

        System.out.println("Server Addr: " + serverAddrString);
        System.out.println("Server Port: " + serverPort);
        System.out.println("fileToSend: " + fileToSend);
        System.out.println("winSize: " + winSize);
        System.out.println("mss: " + mss);

        clientAddr = InetAddress.getLocalHost();

        serverAddr = InetAddress.getByName(serverAddrString);

        sendData = new DatagramSocket();

        receiveACK = new DatagramSocket(clientPort);

        transmit();

    }

    protected static void setTimer(int sequ){

        try {
            synchronized (retransmitters) {
                retransmitters[sequ] = new Retransmitter(sequ);
            }

            synchronized (timers) {
                timers[sequ] = new Timer();
                timers[sequ].schedule(retransmitters[sequ], timeOut);
            }
        }
        catch (IllegalStateException e){
//			System.out.println("Wrong Order : " + sequ);
        }

        return;

    }

    protected static void cancel(int sequ){

        synchronized (timers[sequ]) {
            synchronized (retransmitters){
                retransmitters[sequ].cancel();
            }
            synchronized (timers) {
                timers[sequ].cancel();
                timers[sequ].purge();
            }
        }

        return;

    }

    private static class Receiver implements Runnable {

        @Override
        public void run(){

            beginTime = System.currentTimeMillis();
//			System.out.println("Receiver Running!");

            leftSeqNum = 0;
            rightSeqNum = 0;

            timers = new Timer[(int)mssNum + 1];
            retransmitters = new Retransmitter[(int)mssNum + 1];

            for(int i = 0; i < winSize && i < fileBytes.size(); i++){
                try {

                    rdt_send(fileBytes.get(i), i);

                    setTimer(i);

                }
                catch (IOException e){
                    e.printStackTrace();
                }

                rightSeqNum = i;
            }

//			timer = new Timer();
//			timer.schedule(retransmit, timeOut);

            while(true){

                byte[] tmpRecByte = new byte[header];
                DatagramPacket tmpReceiver = new DatagramPacket(tmpRecByte, header);

                try {
                    receiveACK.receive(tmpReceiver);
                    tmpRecByte = tmpReceiver.getData();
                }
                catch (IOException e){
                    e.printStackTrace();
                }

                //	ACK Sequence field
                byte[] recSeq = new byte[4];
                System.arraycopy(tmpRecByte, 0, recSeq, 0, 4);
                int receiveSeqNum = java.nio.ByteBuffer.wrap(recSeq).getInt();

//				System.out.println("ACK: " + receiveSeqNum);

                cancel(receiveSeqNum);

                if(!ack[receiveSeqNum])
                    unAck--;
                ack[receiveSeqNum] = true;


                //	If all segments have been ACKed;
                if(unAck <= 0){

                    for(int i = 0; i <= mssNum; i++)
                        cancel(i);

                    endTime = System.currentTimeMillis();
                    System.out.println("File Transfer Complete! " +
                            "Begin Time: " + beginTime +
                            "  End Time: " + endTime +
                            "  Total Time(seconds): " + (double)(endTime - beginTime) / 1000);

                    break;

                }

                //	If receiveACK is expected, slide window rightward 1 slot;
                if(receiveSeqNum == leftSeqNum){

                    int slide = 0;
                    while(ack[leftSeqNum + slide])
                        slide++;

                    leftSeqNum += slide;

                    int prevRight = rightSeqNum;

                    if(rightSeqNum < (int) mssNum) {

                        if (rightSeqNum + slide > (int) mssNum)
                            rightSeqNum = (int) mssNum;
                        else
                            rightSeqNum += slide;

                        for(int t = prevRight; t <= rightSeqNum; t++){

                            try {
                                rdt_send(fileBytes.get(t), t);

                                setTimer(t);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }

                    }

                }
            }

            System.out.println("Closing Receiver()...");
        }

    }


}
