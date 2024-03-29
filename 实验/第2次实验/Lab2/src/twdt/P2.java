package twdt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class P2 {
	public static DatagramSocket socket = null;
	private static final int N = 5;
	public static final int timeout = 2000;
	public static int base = 1;
	public static int seqnum = 1;
	public static Timer timer = new Timer();
	private static HashMap<Integer,String> unacked = new HashMap<Integer, String>();
	public static void main(String args[]) throws FileNotFoundException {
		// set output stream
		PrintStream output = new PrintStream(new File("mess.txt"));
		System.setOut(output);
		try {
		//prepare data
		File file = new File("9999data.txt");
		Reader reader = new FileReader(file);
		//prepare socket
		socket = new DatagramSocket(9999);
		// begin to receive data from 9999
		new recvData2().start();
		//prepare data container
		DatagramPacket packet = null;
		char[] buffer = new char[1024];
		while(true) {
			if(seqnum < base + N) {
				int next = reader.read(buffer);
				if(next == -1) {
					String message = "seqnum" + new Integer(-1).toString() + "seqnum";
					packet = new DatagramPacket(message.getBytes(),message.getBytes().length,InetAddress.getLocalHost(),6666);
					socket.send(packet);
					return;
				}
				String message = buffer.toString();
				message = message + "seqnum" + new Integer(seqnum) + "seqnum";
				packet = new DatagramPacket(message.getBytes(),message.getBytes().length,InetAddress.getLocalHost(),6666);
				socket.send(packet);
				System.out.println("send message " + new Integer(seqnum).toString());
				unacked.put(new Integer(seqnum), message);
				if(seqnum == base) {
					//begin timer
					timer = new Timer();
					timer.schedule(new myTimer2(timer), timeout);
				}
				seqnum ++;
				System.out.println("base");
				System.out.println(base);
				System.out.println("seqnum");
				System.out.println(seqnum);
			}
			System.out.println("out");
		}
	} catch (Exception e) {
		e.printStackTrace();
	}
	}
	public static void func() throws IOException {
		DatagramSocket socket = P2.socket;
		for (int i = base;i < seqnum;i ++) {
			DatagramPacket packet = new DatagramPacket(unacked.get(new Integer(i)).getBytes(),unacked.get(new Integer(i)).getBytes().length,InetAddress.getLocalHost(),6666);
			socket.send(packet);
			System.out.println("send message " + new Integer(i).toString());
		}
	}
}
//class recvAck2 extends Thread{
//	public void run() {
//		try {
//			byte[] content = new byte[1024];
//			DatagramSocket socket  = P2.socket;
//			while(true) {
//				DatagramPacket packet = new DatagramPacket(content, content.length);
//				socket.receive(packet);
//				String ans = new String(packet.getData());
//				System.out.println("receive ack " + ans);
//				int next = (int)(new Integer(ans.split("ack")[1])) + 1;
//				if(next == 0) break;
//				P2.base = Math.max(P2.base, next);
//				if(P2.base == P2.seqnum) P2.timer.cancel();
//				else {
//					P2.timer = new Timer();
//					P2.timer.schedule(new myTimer(P2.timer), P2.timeout);
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//}
class myTimer2 extends TimerTask{
	private Timer timer;
	public myTimer2(Timer timer) {
		this.timer = timer;
	}
	public void run() {
		try {
			P2.func();
		} catch (IOException e) {
			e.printStackTrace();
		}
		timer.cancel();
		return;
	}
}
class recvData2 extends Thread{
	public static int seqnum = 0;
	public static File file = new File("9999recvdata.txt");
	public void run() {
		try {
			//output redirect
			PrintStream output = new PrintStream(new File("mess.txt"));
			System.setOut(output);
			if (file.exists() == false) file.createNewFile();
			FileWriter writer = new FileWriter(file);
			DatagramSocket socket = P2.socket;
			byte[] content = new byte[1024];
			while(true) {
				try {
					DatagramPacket packet = new DatagramPacket(content, content.length);
					socket.receive(packet);
					String ans = new String(packet.getData());
					if(ans.contains("ack")) {
						System.out.println("receive ack " + ans);
						int next = (int)(new Integer(ans.split("ack")[1])) + 1;
						if(next == 0) break;
						P2.base = Math.max(P2.base, next);
						if(P2.base == P2.seqnum) P2.timer.cancel();
						else {
							P2.timer = new Timer();
							P2.timer.schedule(new myTimer(P2.timer), P2.timeout);
						}
						continue;
					}
					String[] message = new String(packet.getData()).split("seqnum");
					if(message[0].equals("")) {
						String ackMessage = "ack" + new Integer(-1).toString() + "ack";
						packet = new DatagramPacket(ackMessage.getBytes(),ackMessage.getBytes().length,InetAddress.getLocalHost(),6666);
						socket.send(packet);
						break;
					}
					int recnum = (int)(new Integer(message[1]));
					if (recnum == seqnum + 1) {
						writer.write(message[0]);
						seqnum += 1;
					}
					String ackMessage = "ack" + new Integer(recnum).toString() + "ack";
					packet = new DatagramPacket(ackMessage.getBytes(),ackMessage.getBytes().length,InetAddress.getLocalHost(),6666);
					socket.send(packet);
				} catch (Exception e) {
					System.out.println();
				}
			}
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
