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
			clientListener = new ServerSocket(Integer.parseInt(args[0]));
			while (true) {
				Socket clientConnection = clientListener.accept();
				ConnectionThread connection = new ConnectionThread(clientConnection);
				(new Thread(connection)).start();
			}
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}

}
