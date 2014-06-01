package Tor61;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Arrays;
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;

public class ProxyConnection extends Connection {
	
	Socket connection;
	Node node;
	boolean connected;
	ProxyConnectionWriteBuffer writeBuffer;
	
	ProxyConnection(Socket connection, Node node) {
		this.connection = connection;
		this.node = node;
		this.connected = false;
		writeBuffer = new ProxyConnectionWriteBuffer();
		(new Thread(writeBuffer)).start();
	}
	
	@Override
	public void run() {
		
		try {
			// Connect a read buffer to the input from the client
			BufferedReader inFromClient = new BufferedReader(
					new InputStreamReader(connection.getInputStream()));

			
			// Put this ProxyConnection in the Proxy's stream table
			short streamID = generateStreamID();
			node.streamTable.put(streamID, this);
			
			// Collect header and find host information within
			String host = "";
			int port;
			String header = parseHeader(inFromClient);
			Scanner headScan = new Scanner(header);
			// Find the host information
			while (headScan.hasNextLine()) {
				String line = headScan.nextLine();
				Scanner lineScan = new Scanner(line);
				String tag = lineScan.next();
				if (tag.equals("Host:")) {
					host = lineScan.next();
				}
			}
			headScan.close();
			
			// First index will be hostname--second, if there, will be port
			String[] hostInformation = host.split(":");
			host = hostInformation[0];
			// If a port number is given, use it. Else, assume port 80
			port = (hostInformation.length > 1) ? 
					Integer.parseInt(hostInformation[1]) : 80;
					
					
			// CREATE THE STREAM
					// we have IP + port
					// get stream number
			byte[] relayBegin = CellFormatter.relayBeginCell(node.router.thisCircuitID + "", streamID + "", host, port + "");
			node.circuit.send(relayBegin);
			
			// Wait to receive the connected message
			while(!connected) {}
			
			
			

			// Create socket connected to server, set up output to this server
			//Socket serverSocket = new Socket(host, port);
			//DataOutputStream outToServer = new DataOutputStream(
					//serverSocket.getOutputStream());
			// Send header, along with double new-line to mark end of header
			//outToServer.writeBytes(header.toString() + "\r\n\r\n");
					
			/***********
			 *
			 * This is where we make the cell to go on the TOR network
			 * 
			 */
					
			byte[][] dataCells = CellFormatter.relayDataCell(node.router.thisCircuitID + "", streamID + "", header + "");
			
			// Since each data cell can be at max 512 bytes, we have to send
			// all of them that we received.
			for (int i = 0; i < dataCells.length; i++) {
				node.circuit.send(dataCells[i]);
			}
			
			
			// DONE WITH STREAM CREATION, START SENDING ANY INCOMING DATA ALONG STREAM
			
			while(true) {
				
				// if there is information from the client
				// pack information into realy data cells
				// send relay data cells along our stream
				
				
			}
			
			
			
			
			
			
			
				
		} catch (IOException e) {
			System.err.println("Error accepting data from client or server: " + e.getMessage());
		}
	}
	
	/**
	 * Generates a stream ID that is not already in use on this node
	 * @return short, the stream ID to use
	 */
	public short generateStreamID() {
		Short streamID;
		do {
			Random r = new Random();
			streamID = (short) r.nextInt(Short.MAX_VALUE + 1);
		} while (node.streamTable.keySet().contains(streamID));
		return streamID;
	}
	
	// Parse header, changing connection type to Connection: close, and returning 
	// the entire header as a String
	public static String parseHeader(BufferedReader input) throws IOException {
		StringBuffer header = new StringBuffer();
		String line = input.readLine();
		
		// Print first line, as requested in the spec
		System.out.println(line);
		
		// Sometimes the request does not include a header, which happened
		// for us when we used the back arrow, or did not hard refresh some
		// web pages. These missed connections
		// should not affect the functionality of the proxy,
		// but we cannot make the connection without being supplied a header
		if (line == null) {
			System.err.println("Header not included. The page may be cached");
			return "";
		}
		
		// Collect the contents of the header and add to our header String
		// if the current line contains connection information, makes sure
		// it specifies Connection: close so that connections are not
		// kept alive
		while (line.length() > 0) {
			Scanner lineScan = new Scanner(line);
			line = line.trim();
			String tag = lineScan.next();
			if (tag.equals("Connection:")) {
				String connectionType = lineScan.next();
				if (!connectionType.equals("close")) {
					line = "Connection: close";
				}
			} 
			header.append(line + "\n");
			line = input.readLine();
			lineScan.close();
		}
		
		// Return the header
		return header.toString();
	}
	
	/**
	 * Sends the given byte array to the connected client
	 * @param cell, the byte array to be sent
	 */
	public void send(byte[] cell) {
		writeBuffer.put(cell);
	}
	
	// Private inner class used to write information to the outside service,
	// be it a browser or web server, with which this connection speaks
	private class ProxyConnectionWriteBuffer implements Runnable {
		Queue<byte[]> buffer;
		DataOutputStream out;
		
		/**
		 * Create a new ProxyConnectionWriteBuffer to handle the writing of information
		 * to a browser or web server
		 */
		public ProxyConnectionWriteBuffer() {
			// Using linked list for the queue
			buffer = new LinkedBlockingQueue<byte[]>();
			try {
				out = new DataOutputStream(connection.getOutputStream());
			} catch (IOException e) {
				System.out.println("Unable to create output stream for proxy connection at: " +
									connection.getLocalAddress() + ", " + connection.getLocalPort());
				e.getMessage();
			}
		}
		
		/**
		 * Put the given byte array into the write buffer (byte array is expected to be in some
		 * cell format)
		 * @param cell
		 */
		public void put(byte[] cell) {
			buffer.add(cell);
		}
		
		// Loop forever. When information is in the buffer, send it along the connection
		public void run() {
			while (true) {
				if(!buffer.isEmpty()) {
					System.out.println("Received data in write buffer for proxy connection at " + 
										connection.getLocalAddress() + ", " + connection.getLocalPort());
					System.out.println("Now sending data.");
					try {
						byte[] cell = buffer.remove();
						
						// CHECK TYPE OF CELL
						// - if relay connected, flip boolean flag
						// - if relay data, send to connected browser / server
						CellFormatter.CellType type = CellFormatter.determineType(cell);
						if (type == CellFormatter.CellType.RELAY_CONNECTED) {
							System.out.println("Received relay connected");
							
						} else if (type == CellFormatter.CellType.RELAY_CONNECTED) {
							System.out.println("Received relay data");
							connected = true;
						} else {
							System.out.println("Received unexpected type: " + type);
						}
						
						System.out.println("Data to be sent: " + Arrays.toString(cell));
						out.write(cell);
					} catch (IOException e) {
						System.out.println("Unable to write to output stream for proxy connection at: " +
								connection.getLocalAddress() + ", " + connection.getLocalPort());
						e.printStackTrace();
					}
				}
			}
		}	
	}

}
