// Nick Adman, project 0

// Runs alongside the client and server code
// accepting incoming data from the connected
// remote address and printing it to the command line

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class IncomingPacketThread implements Runnable {

	private DatagramSocket socket;
	
	IncomingPacketThread(DatagramSocket socket) {
		this.socket = socket;
	}
	
	// Runs separate thread, listening for incoming data in aSocket
	// and printing it to the the console
	public void run() {
		byte[] buffer = new byte[1500];
		DatagramPacket request = new DatagramPacket(buffer, buffer.length);
		while (!socket.isClosed()) {
			try {
				socket.receive(request);  // Wait for incoming packet
				// Print message from packet
				for (int i = 0; i < request.getLength(); i++) 
					System.out.print((char) buffer[i]);
				System.out.println();
			} catch (IOException e) { } // Thrown once when user exits server - do nothing
		}
	}		
}
