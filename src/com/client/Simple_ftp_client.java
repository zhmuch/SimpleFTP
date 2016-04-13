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

	private static InetAddress clientAddr;
	private static int clientPort = 7736;

	private static String serverAddrString = "192.168.1.3";
	private static InetAddress serverAddr;
	private static int serverPort = 7735;

	private static int mss = 8;
	private static int mssNum = 16;
	private static int lastSeg = 8;
	private static int header = 8;


	private static void rdt_send(byte[] data, int currSequence) throws IOException {

		//	32-bit Sequence Number
		ByteArrayOutputStream tmpBytes = new ByteArrayOutputStream();
		DataOutputStream tmpOut = new DataOutputStream(tmpBytes);
		tmpOut.writeInt(currSequence);
		byte[] tmpSeq = tmpBytes.toByteArray();

		//	16-bit checksum
		byte[] tmpCheck = Simple_ftp_helper.compChecksum(data);

		//	16-bit 0101010101010101
		byte[] tail = new BigInteger("0101010101010101", 2).toByteArray();

		//	Packet bytes.
		byte[] sendBytes = new byte[data.length + 8];

		System.arraycopy(tmpSeq, 0, sendBytes, 0, 4);
		System.arraycopy(tmpCheck, 0, sendBytes, 4, 2);
		System.arraycopy(tail, 0, sendBytes, 6, 2);
		System.arraycopy(data, 0, sendBytes, 8, data.length);

		System.out.println("Length of byte[] test is: " + sendBytes.length);

		DatagramPacket p = new DatagramPacket(sendBytes, sendBytes.length, serverAddr, serverPort);
		sendData.send(p);

	}

	private static void transmit() throws IOException{

		//	8 Bytes Data
		byte[] data = new byte[8];

		rdt_send(data, 0);

	}

	public static void main(String[] args) throws IOException {

		clientAddr = InetAddress.getLocalHost();

		serverAddr = InetAddress.getByName(serverAddrString);

		sendData = new DatagramSocket();

		transmit();

//		//	Receive
//		DatagramSocket receiver = new DatagramSocket(clientPort);
//		byte[] receiveACK = new byte[8];
//		DatagramPacket tmpReceiver = new DatagramPacket(receiveACK, 8);
//
//		receiver.receive(tmpReceiver);
//		receiveACK = tmpReceiver.getData();
//
//		for(int i = 0; i < 8; i++)
//			System.out.print(receiveACK[i] + " ");
//		System.out.println();
	}

	private static class Receiver implements Runnable {

		@Override
		public void run(){

		}

	}

}
