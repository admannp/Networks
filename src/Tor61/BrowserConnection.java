package Tor61;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Arrays;
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;

public class BrowserConnection extends Connection implements Runnable {
	private Node node;
	private Queue<byte[]> writeBuffer;
	
	public BrowserConnection(Socket socket, Node node) {
		super(socket);
		this.node = node;
		this.writeBuffer = new LinkedBlockingQueue<byte[]>();
	}
	
	public void run() {
		// Put this ProxyConnection in the Proxy's stream table
		short streamID = generateStreamID();
		node.streamTable.put(new StreamTableKey(node.router.thisCircuitID, streamID), this);
		
		// Get the header send from the browser
		String header = "";
		try {
			header = parseHeader(new BufferedReader(new InputStreamReader(inputStream)));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Unable to create buffered reader on socket at IP, port " + 
							socket.getInetAddress() + " " + socket.getPort());
			System.out.println(e.getMessage());
		}
		
		// Get the host and port information from the header
		String headerInformation = getHeaderInformation(header);
		String[] hostInformation = headerInformation.split(":");
		String host = hostInformation[0];
		// If a port number is given, use it. Else, assume port 80
		int port = (hostInformation.length > 1) ? 
				Integer.parseInt(hostInformation[1]) : 80;
		
		// CREATE THE STREAM BY SENDING RELAY BEGIN CELL
		byte[] relayBegin = CellFormatter.relayBeginCell(node.router.thisCircuitID + "", streamID + "", host, port + "");
		node.circuit.send(relayBegin);
		
		waitForMessage(CellFormatter.CellType.RELAY_CONNECTED, header + "\r\n\r\n");
		waitForMessage(CellFormatter.CellType.RELAY_DATA, "");
	}
	
	public void send(byte[] bytes) {
		writeBuffer.add(bytes);
	}
	
	private void waitForMessage(CellFormatter.CellType type, String header) {
		while(true) {
			if(!writeBuffer.isEmpty()) {
				
				try {
					byte[] cell = writeBuffer.remove();
					String streamID = CellFormatter.getStreamIDFromCell(cell);
					CellFormatter.CellType messageType = CellFormatter.determineType(cell);
					
					// If relay connected, send the header
					if (messageType == type && type == CellFormatter.CellType.RELAY_CONNECTED) {
						System.out.println("Received RELAY_CONNECTED");
						byte[][] dataCells = CellFormatter.relayDataCell(node.router.thisCircuitID + "", streamID + "", header);
						
						// Since each data cell can be at max 512 bytes, we have to send
						// all of them that we received.
						for (int i = 0; i < dataCells.length; i++) {
							node.circuit.send(dataCells[i]);
						}
						// Stop waiting for connected response
						break;
					} else if (messageType == type && type == CellFormatter.CellType.RELAY_DATA) {
						
						
						System.out.println("Received relay data");
						System.out.println("Data to be sent: " + Arrays.toString(cell));
						String data = CellFormatter.getRelayDataInformation(cell);
						System.out.println(data);
						System.out.println("Writing relay data to socket at IP, port: " + 
											socket.getInetAddress() + " " + socket.getPort());
						
						outputStream.writeBytes(data);
						System.out.println("Data written");
						
					} else {
						System.out.println("Received wrong response type.");
					}
					
					
				} catch (IOException e) {
					System.out.println("Error reading writebuffer associated with IP, port: " + 
							socket.getInetAddress() + " " + socket.getPort());
					System.out.println(e.getMessage());
				}
			}
		}
	}
	
	public String getHeaderInformation(String header) {
		// Collect header and find host information within
		String host = "";
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
		return host;
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
	
	/**
	 * Parse the header of the incoming request from the browser
	 * @param input, input stream from the connection to the browser client
	 * @return, string of the header received from the browser
	 * @throws IOException
	 */
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
	
}
