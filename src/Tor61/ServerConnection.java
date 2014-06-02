package Tor61;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerConnection extends Connection implements Runnable {
	private Queue<byte[]> writeBuffer;
	RouterConnection returnCircuit;
	String returnCircuitID;
	String streamID;
	
	ServerConnection(Socket socket, RouterConnection returnCircuit, String returnCircuitID) {
		super(socket);
		writeBuffer = new LinkedBlockingQueue<byte[]>();
		this.returnCircuit = returnCircuit;
		this.returnCircuitID = returnCircuitID;
	}
	
	public void send(byte[] cell) {
		writeBuffer.add(cell);
	}
	
	public void run() {
		writeToServer();
		readFromServer();
	}
	
	private void readFromServer() {
		System.out.println("Waiting to read data from server connected to socket at IP, port: " + 
							socket.getInetAddress() + " " + socket.getPort());
		
		// To collect the data given by the server
		ByteArrayOutputStream serverResponse = new ByteArrayOutputStream();
		
		// Accept bytes from server, writing into given buffer 
		byte buffer[] = new byte[1024];
		try {
			for(int s; (s=inputStream.read(buffer)) != -1; ) {
			  serverResponse.write(buffer, 0, s);
			}
		} catch (IOException e) {
			System.out.println("Unable to read response from server.");
			System.out.println(e.getMessage());
		}
		String result = serverResponse.toString();
		System.out.println("RESPONSE TO INCOMING INFORMATION");
		System.out.print(result);
		// SEND BYTES ALONG THE CIRCUIT
		byte[][] dataCells = CellFormatter.relayDataCell(returnCircuitID + "", streamID + "", result);
		for (int i = 0; i < dataCells.length; i++) {
			returnCircuit.send(dataCells[i]);
		}
	}
	
	private void writeToServer() {
		while(true) {
			if(!writeBuffer.isEmpty()){
				byte[] cell = writeBuffer.remove();
				streamID = CellFormatter.getStreamIDFromCell(cell);
				String message = CellFormatter.getRelayDataInformation(cell);
				System.out.println("Sending data to server on socket at IP, port: " + socket.getInetAddress() + ",  " + socket.getPort());
				System.out.println("Data sent: " + message);
				try {
					outputStream.writeBytes(message);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					System.out.println("Failed to write to server connected to socket at IP, port: " + 
											socket.getInetAddress() + ",  " + socket.getPort());
					System.out.println(e.getMessage());
				}
				
				// TODO: add check on the size of the message to for cases where the \r\n\r\n is split between cells
				if(message.contains("\r\n\r\n")) { 
					break; 
				}
			}
				
		}
	}
	
}
