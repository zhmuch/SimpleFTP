package com.client;


import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;

/**
 * Created by Muchen on 4/1/16.
 */


public class Simple_ftp_client {

	private static int port;
	private static String serverAddr = "192.168.1.3";

	public static void main(String[] args) throws IOException {

		ByteArrayOutputStream tmpBytes = new ByteArrayOutputStream();
		DataOutputStream tmpOut = new DataOutputStream(tmpBytes);
		tmpOut.writeInt(0);

		byte[] test = tmpBytes.toByteArray();


		System.out.println(InetAddress.getLocalHost());

		DatagramPacket p = new DatagramPacket(test, test.length, InetAddress.getByName(serverAddr), 14000);

		DatagramSocket sendSocket = new DatagramSocket();
		sendSocket.send(p);




	}

}
