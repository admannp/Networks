package registrationProtocol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class ProbeHandler implements Runnable {
	
	private DatagramSocket socket;
	
	ProbeHandler(DatagramSocket socket, InetAddress address, int port) {
		this.socket = socket;
	}
	
	
	// Runs the handling of the probe responses to registration service
	// Waits for probe message, then sends ACK response
	public void run() {
		
		byte[] probeMessage = new byte[4];

		DatagramPacket probePacket = new DatagramPacket(probeMessage, probeMessage.length);
		
		try {

			while(!socket.isClosed()) {
				socket.receive(probePacket);
				
				// Only respond if message is of proper format
				if (probeMessage[0] == -60 && probeMessage[1] == 97 && 
						probeMessage[3] == (byte) 6) {
					byte[] responseMessage = new byte[4];
					responseMessage = probeMessage;
					
					// Change incoming message to an ACK format, send back
					responseMessage[3] = (byte) 7;
					DatagramPacket responsePacket = new DatagramPacket(responseMessage, responseMessage.length, probePacket.getAddress(), probePacket.getPort());
					socket.send(responsePacket);
				}
			}
		} catch (SocketException e) { 
			return;
		} catch (IOException e) {
			System.out.println("IO Exception: " + e.getMessage());
		} 
	}
}
