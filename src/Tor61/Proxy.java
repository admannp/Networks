package Tor61;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import Proxy.ConnectionThread;

public class Proxy {
	
	Node node;
	
	public Proxy(Node node, int HTTPProxyPort) {
		this.node = node;
		
	}
	
	private class ProxyConnectionAcceptor implements Runnable {
		
		ServerSocket clientListener;
		
		ProxyConnectionAcceptor(int HTTPProxyPort) {
			try {
				clientListener = new ServerSocket(HTTPProxyPort);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			while (true) {
				try {
					Socket clientConnection = clientListener.accept();
					ProxyConnection connection = new ProxyConnection(clientConnection);
					(new Thread(connection)).start();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.err.println(e.getMessage());
				}
			}
			
		}
		
	}
	
	// Skeleton of what we might use to set up an end-point connection
	public ProxyConnection createConnection(String hostname) {
		throw new RuntimeException();
	}
	
}
