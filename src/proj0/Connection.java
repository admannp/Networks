// Nick Adman, project 0

// Parent class of client and server -
// contains code used by both the client and 
// the server in setting up a UDP connection to
// a remote address

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.URL;
import java.util.Scanner;

public class Connection {
	
	// Checks number of arguments passed - exit with error code 1 if there is not
	// one and only one argument
	protected static void usage(int actual, int expected) {
		if (actual != expected) {
			System.out.println("Usage: java Server <Port Number>");
			System.exit(1);
		}
	}
	
	// Get the "useful" address of the server
	// Includes a check to make sure this is not a LAN specific address
	protected static String getAddress() throws IOException {
		String localAddress = Inet4Address.getLocalHost().getHostAddress(); 
		
		// Check to make sure we aren't using LAN specific IP
		// If so, query JZ's web site
		if (localAddress.startsWith("192.168") || localAddress.contains("127.0.0.1")) {
			URL url = new URL("http://abstract.cs.washington.edu/~zahorjan/ip.cgi");
		    InputStream is = url.openConnection().getInputStream();
		    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		    reader.readLine();
		    localAddress = reader.readLine();
		    localAddress = localAddress.substring(0, localAddress.indexOf(" "));
		    reader.close();
		}
		return localAddress;
	}
	
	// Reads any input from the console and sends results to the address
	// of the given DatagramPacket
	protected static void sendConsoleInput(DatagramPacket request, DatagramSocket aSocket) throws IOException {
		Scanner in = new Scanner(System.in);
		String message;
		while (in.hasNextLine()) {
			message = in.nextLine();
			DatagramPacket reply = new DatagramPacket(message.getBytes(), message.length(),
					request.getAddress(), request.getPort());
			aSocket.send(reply);
		}
		in.close();
	}
}
