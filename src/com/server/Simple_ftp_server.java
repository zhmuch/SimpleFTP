/**
 * Created by Muchen on 4/1/16.
 */

package com.server;

import java.util.*;
import java.io.*;
import java.net.*;

import com.Simple_ftp_helper;

public class Simple_ftp_server {

    private static int portNum;
    private static double errProb;
    private static String fileName;

    private static void listen() throws IOException{

        DatagramSocket server = new DatagramSocket(portNum);

//        byte[] testCheck = { (byte) 0xed, 0x2A, 0x44, 0x10, 0x03, 0x30 };
//        System.out.println("Checksum: " + Simple_ftp_helper.compChecksum(testCheck));

    }

    public static void main(String[] args){

        String[] test = {"14000", "test", "0.1"};
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
