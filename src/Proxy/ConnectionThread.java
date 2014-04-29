package Proxy;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Scanner;

public class ConnectionThread implements Runnable {
	
	private Socket clientSocket;
	
	public ConnectionThread(Socket socket) {
		clientSocket = socket;
	}
	
	public void run() {
		
		try {
			BufferedReader inFromClient = new BufferedReader(
					new InputStreamReader(clientSocket.getInputStream()));
			DataOutputStream outToClient = new DataOutputStream(
					clientSocket.getOutputStream());
			
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
			port = (hostInformation.length > 1) ? 
					Integer.parseInt(hostInformation[1]) : 80;

			// Create socket connected to server, set up output stream
			Socket serverSocket = new Socket(host, port);
			DataOutputStream outToServer = new DataOutputStream(
					serverSocket.getOutputStream());
			// Send header, along with double new-line
			outToServer.writeBytes(header.toString() + "\r\n\r\n");
			
			// Input stream from server
			DataInputStream inFromServer = new DataInputStream(serverSocket.getInputStream());
			
			ByteArrayOutputStream serverResponse = new ByteArrayOutputStream();
			byte buffer[] = new byte[1024];
			for(int s; (s=inFromServer.read(buffer)) != -1; ) {
			  serverResponse.write(buffer, 0, s);
			}
			byte result[] = serverResponse.toByteArray();
			
			
			outToClient.write(result);
			
		} catch (IOException e) {
			System.err.println("IOException error: " + e.getMessage());
		}
	}
	
	
	// Parse header, changing connection type to Connection: close, and returning 
	// the host information
	public static String parseHeader(BufferedReader input) throws IOException {
		StringBuffer header = new StringBuffer();
		String line = input.readLine();
		
		// Print first line
		System.out.println(line);

		if (line == null) {
			System.err.println("The page is cached");
			return "";
		}
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
		
		return header.toString();
	}
	
}
