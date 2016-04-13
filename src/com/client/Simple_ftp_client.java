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

	private static int port;
	private static String serverAddr = "192.168.1.3";

	public static void main(String[] args) throws IOException {


		//	8 Bytes Data
		byte[] data = new byte[8];

		//	32-bit Sequence Number
		ByteArrayOutputStream tmpBytes = new ByteArrayOutputStream();
		DataOutputStream tmpOut = new DataOutputStream(tmpBytes);
		tmpOut.writeInt(0);
		byte[] tmpSeq = tmpBytes.toByteArray();

		//	16-bit checksum
		byte[] tmpCheck = Simple_ftp_helper.compChecksum(data);

		//	16-bit 0101010101010101
		byte[] tail = new BigInteger("0101010101010101", 2).toByteArray();


		byte[] test = new byte[16];


		System.arraycopy(tmpSeq, 0, test, 0, 4);
		System.arraycopy(tmpCheck, 0, test, 4, 2);
		System.arraycopy(tail, 0, test, 6, 2);
		System.arraycopy(data, 0, test, 8, 8);

		for(int i=0; i<16; i++)
			System.out.println(test[i]);

		System.out.println("Length of byte[] test is: " + test.length);

		DatagramPacket p = new DatagramPacket(test, test.length, InetAddress.getByName(serverAddr), 14000);
		DatagramSocket sendSocket = new DatagramSocket();
		sendSocket.send(p);




	}

}
