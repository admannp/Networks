package Proxy;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Scanner;

public class Proxy {
	public static void main(String[] args) {
			
		try {
			ServerSocket clientListener = new ServerSocket(Integer.parseInt(args[0]));
			Socket clientConnection = clientListener.accept();
			BufferedReader inFromClient = new BufferedReader(
					new InputStreamReader(clientConnection.getInputStream()));
			DataOutputStream outToClient = new DataOutputStream(
					clientConnection.getOutputStream());
			
			
			String host = "";
			int port;
			String header = parseHeader(inFromClient);
			Scanner headScan = new Scanner(header);
			while (headScan.hasNextLine()) {
				String line = headScan.nextLine();
				Scanner lineScan = new Scanner(line);
				String tag = lineScan.next();
				if (tag.equals("Host: ")) {
					host = lineScan.next();
				}
			}
			// First index will be hostname--second, if there, will be port
			String[] hostInformation = host.split(":");
			host = hostInformation[0];
			port = (hostInformation.length > 1) ? 
					Integer.parseInt(hostInformation[1]) : 80;
			

			
			
			
			// TODO: Don't change this name
			
			// Create socket connected to server, set up output stream
			Socket socketDaServerBetch = new Socket(host, port);
			DataOutputStream outToServer = new DataOutputStream(
					socketDaServerBetch.getOutputStream());
			// Send header, along with double new-line
			outToServer.writeBytes(header.toString() + "\r\n\r\n");
			
			
			// Input stream from server
			DataInputStream inFromServer = new DataInputStream(socketDaServerBetch.getInputStream());
			
			
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte buffer[] = new byte[1024];
			for(int s; (s=inFromServer.read(buffer)) != -1; ) {
			  baos.write(buffer, 0, s);
			}
			byte result[] = baos.toByteArray();
			
			
			System.out.println("-- inputBuffer.toString() --");
			System.out.println(result.toString());
			outToClient.write(result);
			
			
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	// Parse header, changing connection type to Connection: close, and returning 
	// the host information
	public static String parseHeader(BufferedReader input) throws IOException {
		StringBuffer header = new StringBuffer();
		
		// To store length of body following header
		int contentLength = 0;
		// For the host
		String host = "";
		int port = -1;
		String line = input.readLine();
		// Used to check to make sure we read any data at all
		boolean hasData = false;
		
		if (line == null) {
			System.err.println("The page is cached");
			return "";
		}
		while (line.length() > 0) {
			Scanner lineScan = new Scanner(line);
			hasData = true;
			line = line.trim();
			String tag = lineScan.next();
			if (tag.equals("Connection:")) {
				String connectionType = lineScan.next();
				if (!connectionType.equals("close")) {
					line = "Connection: close";
				}
			} /* else if (tag.equals("Host:")) {
				// First index will be hostname--second, if there, will be port
				String[] hostInformation = lineScan.next().split(":");
				host = hostInformation[0];
				port = (hostInformation.length > 1) ? 
						Integer.parseInt(hostInformation[1]) : 80;
			} else if (tag.equals("Content-Length:")) {
				contentLength = lineScan.nextInt();
			} */
			header.append(line + "\n");
			line = input.readLine();
			lineScan.close();
		}
		
		// Checking to make sure we've captured required information
		if (!hasData || contentLength == 0 || host.equals("")) {
			System.err.println("Invalid header");
			System.err.println("Has data: " + hasData);
			System.err.println("Content Length: " + contentLength);
			System.err.println("Host: " + host);
		}
		
		return header.toString();
	}
	
}
