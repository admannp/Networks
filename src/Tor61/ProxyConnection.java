package Tor61;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Scanner;

public class ProxyConnection extends Connection {
	
	Socket connection;
	
	ProxyConnection(Socket connection) {
		this.connection = connection;
	}
	
	@Override
	public void run() {
		
		try {
			// Connect a read buffer to the input from the client
			BufferedReader inFromClient = new BufferedReader(
					new InputStreamReader(connection.getInputStream()));
			// Setup output to this client
			DataOutputStream outToClient = new DataOutputStream(
					connection.getOutputStream());
			
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
			
//			// Prepare to accept input stream from server
//			DataInputStream inFromServer = new DataInputStream(serverSocket.getInputStream());
//			ByteArrayOutputStream serverResponse = new ByteArrayOutputStream();
//			
//			// Accept bytes from server, writing into given buffer 
//			byte buffer[] = new byte[1024];
//			for(int s; (s=inFromServer.read(buffer)) != -1; ) {
//			  serverResponse.write(buffer, 0, s);
//			}
//			byte result[] = serverResponse.toByteArray();
//			
//			// Send the bytes from the server back to the client
//			outToClient.write(result);
//			
		} catch (IOException e) {
			System.err.println("Error accepting data from client or server: " + e.getMessage());
		}
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

}
