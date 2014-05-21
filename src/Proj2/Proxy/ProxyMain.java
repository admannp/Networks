package Proxy;

/*
 * Created by Nick Adman and Colin Miller, CSE 461
 * This class is the main proxy, listening for client connections
 * and creating new threads as they come in.
 */

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ProxyMain {
	
	public static void main(String[] args) {
		ServerSocket clientListener;
		try {
			// Listen for connections at the port given on the command line
			clientListener = new ServerSocket(Integer.parseInt(args[0]));
			// Loop, accepting new connections and sending them off to their own 
			// connection handling thread
			while (true) {
				Socket clientConnection = clientListener.accept();
				ConnectionThread connection = new ConnectionThread(clientConnection);
				(new Thread(connection)).start();
			}
		} catch (NumberFormatException e) {
			System.out.println("Could not interpret the given port number as a number.");
			System.out.println("Perhaps format is incorrect. Please try again. Error message:");
			System.out.println(e.getMessage());
		} catch (IOException e) {
			System.out.println("Error while accepting client connections:");
			System.out.println(e.getMessage());
		}
		

	}

}
