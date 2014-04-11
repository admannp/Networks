// Nick Adman, project 0

// Client code 
// Sends local address to the address specified by the
// user in the command line. Then reads any data
// sent back and prints to the command line. 
// Also reads any input data from the command line
// and sends this to the previously specified address.

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Client extends Connection {
	
	public static void main(String args[]){
		DatagramSocket aSocket = null;
		usage(args.length, 2);
		
		try {
			// Step 2
			aSocket = new DatagramSocket();
			
			// Step 3
			String hostAddress = getAddress();
			hostAddress += " " + aSocket.getLocalPort();
			byte [] addressInBytes = hostAddress.getBytes();
			
			InetAddress serverAddress = InetAddress.getByName(args[0]);
			int serverPort = Integer.parseInt(args[1]);
			
			// Step 4
			DatagramPacket request = new DatagramPacket(addressInBytes, addressInBytes.length, serverAddress, serverPort);
			aSocket.send(request);
			
			// STEP 6
			// Read incoming packets
			new Thread(new IncomingPacketThread(aSocket)).start();
			
			// STEP 5
			// Send input from the console to client
			sendConsoleInput(request, aSocket);
			
		} catch (SocketException e) {
			System.out.println("Socket: " + e.getMessage());
		} catch (IOException e) {
			System.out.println("IO: " + e.getMessage());
		} finally {
			if (aSocket != null) 
				aSocket.close();
		}
	}
}
