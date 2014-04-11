// Nick Adman, project 0

// Client code 
// Sends its own address to the address given by the
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
			// Create new socket on arbitrary port
			aSocket = new DatagramSocket();
			
			// Get the local address and port num, store in string
			String hostAddress = getAddress();
			hostAddress += " " + aSocket.getLocalPort();
			byte [] addressInBytes = hostAddress.getBytes();
			
			// Get IP and port of server to connect to
			InetAddress serverAddress = InetAddress.getByName(args[0]);
			int serverPort = Integer.parseInt(args[1]);
			
			// Connect socket to server address + port
			aSocket.connect(serverAddress, serverPort);

			// Create DatagramPacket which stores message (currently message = local address)
			// Then send packet
			DatagramPacket request = new DatagramPacket(addressInBytes, addressInBytes.length);
			aSocket.send(request);
			
			// Read incoming packets
			new Thread(new IncomingPacketThread(aSocket)).start();

			// Send input from the console to client
			sendConsoleInput(aSocket);
			
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
