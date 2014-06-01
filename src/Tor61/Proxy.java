package Tor61;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import Proxy.ConnectionThread;

public class Proxy {
	
	Node node;
	
	public Proxy(Node node, int HTTPProxyPort) {
		this.node = node;
		node.streamTable = Collections.synchronizedMap(new HashMap<StreamTableKey, ProxyConnection>());
		ProxyConnectionAcceptor pca = new ProxyConnectionAcceptor(HTTPProxyPort, this);
		(new Thread(pca)).start();
	}
	
	private class ProxyConnectionAcceptor implements Runnable {
		
		ServerSocket clientListener;
		Proxy proxy;
		
		ProxyConnectionAcceptor(int HTTPProxyPort, Proxy proxy) {
			this.proxy = proxy;
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
					System.out.println("Accepted new browser connection; building new ProxyConnection. LocalPort: " + clientConnection.getLocalPort());
					ProxyConnection connection = new ProxyConnection(clientConnection, node);
					(new Thread(connection)).start();
					System.out.println("New ProxyConnection created in Proxy class");
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
