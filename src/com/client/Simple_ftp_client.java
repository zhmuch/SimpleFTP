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

	private static String serverAddrString = "192.168.1.3";
	private static InetAddress serverAddr;
	private static int serverPort = 7735;

	private static int mss = 8;
	private static long fileSize;
	private static long mssNum;
	private static int lastSeg = 8;
	private static int header = 8;
	private static ArrayList<byte[]> fileBytes;

	private static String fileToSend = "/Users/Muchen/Desktop/send";
	private static int winSize = 2;

	private static long beginTime;
	private static long endTime;

	/**
	 *
	 * @param data
	 * @param currSequence
	 * @throws IOException
	 */
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

		//	Read and buffer file data;
		File file = new File(fileToSend);
		fileSize = file.length();
		mssNum = fileSize / mss;
		lastSeg = (int) (fileSize - mss * mssNum);
		FileInputStream fileInput = new FileInputStream(file);

		fileBytes = new ArrayList<>();
		for(int i = 0; i < mssNum; i++){
			byte[] tmp = new byte[mss];
			fileInput.read(tmp, 0, mss);

//			for(byte j:tmp)
//				System.out.print(j + " ");
//			System.out.println();

			fileBytes.add(tmp);
		}
		byte[]tmp = new byte[lastSeg];
		fileInput.read(tmp, 0, lastSeg);
		fileBytes.add(tmp);

		if(fileBytes.size() != mssNum + 1)
			System.out.println("FileBuffer Error!");
		else
			System.out.println("FileBuffer Finish!");
		fileInput.close();

//		System.out.println("lastSeg Size " + lastSeg);
//		System.out.println("fileBytes Size " + fileBytes.size());
//		for(byte[] i:fileBytes){
//			for(byte j:i)
//				System.out.print(j + " ");
//			System.out.println();
//		}

		//	Start ACKs Receiver;
		Receiver receiver = new Receiver();
		receiver.run();

	}

	public static void main(String[] args) throws IOException {

		clientAddr = InetAddress.getLocalHost();

		serverAddr = InetAddress.getByName(serverAddrString);

		sendData = new DatagramSocket();

		receiveACK = new DatagramSocket(clientPort);

		transmit();

	}

	private static class Receiver implements Runnable {

		@Override
		public void run(){

			beginTime = System.currentTimeMillis();

			System.out.println("Receiver Running!");

			while(true){

				byte[] tmpRecByte = new byte[header];
				DatagramPacket tmpReceiver = new DatagramPacket(tmpRecByte, header);

				try {
					receiveACK.receive(tmpReceiver);
				}
				catch (IOException e){
					e.printStackTrace();
				}

				//	ACK Sequence field
				byte[] recSeq = new byte[4];
				System.arraycopy(tmpRecByte, 0, recSeq, 0, 4);
				int recSeqNum = java.nio.ByteBuffer.wrap(recSeq).getInt();

				//	If all segments have been ACKed;
				if(recSeqNum >= mssNum){

					endTime = System.currentTimeMillis();
					System.out.println("File Transfer Complete! " +
							"Begin Time: " + beginTime +
							"  End Time: " + endTime +
							"  Total Time(seconds): " + (endTime - beginTime) / 1000);

					break;

				}

			}

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

	}

}
