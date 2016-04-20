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

	private static int mss = 10000;
	private static int winSize = 32;

	private static long fileSize;
	private static long mssNum;
	private static int lastSeg;
	private static int header = 8;
	protected static ArrayList<byte[]> fileBytes;

	/**
	 * Windows "D://xxx//xxx"
	 */
	private static String fileToSend = "/Users/Muchen/Desktop/send";
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
		System.out.println("mss: " + mss);
		System.out.println("mssNum: " + mssNum);
		System.out.println("lastSeq: " + lastSeg);

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
			System.out.println("Type in anything to start...");

			BufferedReader keybd = new BufferedReader(new InputStreamReader(System.in));
			String keybdi = keybd.readLine();

			//	Start ACKs Receiver;
			Receiver receiver = new Receiver();
			receiver.run();

		}

		System.exit(0);

	}

	public static void main(String[] args) throws IOException {

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

				cancel(receiveSeqNum);

//				System.out.println("ACK: " + receiveSeqNum);

				//	If all segments have been ACKed;
				if(receiveSeqNum >= mssNum){

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
					if(rightSeqNum < (int) mssNum){
						rightSeqNum++;

						try{
							rdt_send(fileBytes.get(rightSeqNum), rightSeqNum);

							setTimer(rightSeqNum);
						}
						catch (IOException e){
							e.printStackTrace();
						}

//						timer = new Timer();
//						retransmit = new Retransmit();
//						timer.schedule(retransmit, timeOut);
					}
					leftSeqNum++;
				}
			}

			System.out.println("Closing Receiver()...");
		}

	}


}
