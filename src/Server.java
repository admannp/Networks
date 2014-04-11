// Nick Adman, project 0

// Server Code 
// Creates a UDP server, prints its address and port, 
// then waits for incoming data, and prints it to
// the terminal. Also sends any input data from the 
// command line to the client that connects.

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class Server extends Connection {
	
	public static void main(String args[]) {		
		// Create null socket object for wide scope
		DatagramSocket aSocket = null;
		usage(args.length, 1);
		try {
			// Print useful address and port number
			String localAddress = getAddress();	
			
			// STEP 1
			// Get port number from command line and bind socket accordingly
			int port = Integer.parseInt(args[0]);
			aSocket = new DatagramSocket(port);
			
			// STEP 2
			// Print out the full address
			String fullAddress = localAddress + " " + port;
			System.out.println(fullAddress);
		    
			// STEP 3 - 7
			// Wait for and handle new connection
			handleClientConnection(fullAddress, aSocket);
			
			// STEP 8
			// Read incoming packets
			new Thread(new IncomingPacketThread(aSocket)).start();
			
			// STEP 9
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
	
	// Server specific connection handling
	// Accepts the full address of the server (including port) as a String
	// Waits for a connection from a client, then prints out the address information of that client
	// and sends server's address information back to client
	private static void handleClientConnection(String fullAddress, DatagramSocket aSocket) throws IOException {
		// Buffer to store incoming messages - optimal size of 1500 bytes
		byte[] buffer = new byte[1500];
		
		// Create packet object to handle received data
		DatagramPacket request = new DatagramPacket(buffer, buffer.length);
		aSocket.receive(request);  // Wait for incoming packet
		
		// Print address information of client
		System.out.println("[Contact from " + request.getAddress().toString().substring(1) + ":" + request.getPort() + "]");
		
		aSocket.connect(request.getAddress(), request.getPort());
		
		// Print message from packet
		for (int i = 0; i < request.getLength(); i++) 
			System.out.print((char) buffer[i]);
		System.out.println();
		
		// Reply with my IPv4 and port 
		DatagramPacket reply = new DatagramPacket(fullAddress.getBytes(), fullAddress.length(),
				request.getAddress(), request.getPort());
		aSocket.send(reply);
	}	
}
