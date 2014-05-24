package Tor61;

import java.net.Socket;

public class RouterConnection extends Connection {
	
	Socket connection;
	
	public RouterConnection(Socket connection) {
		this.connection = connection;
	}
	
	@Override
	public void run() {
		// TODO: Put stuff here
	}
	
	private class RouterConnectionWriteBuffer implements Runnable {
		
		public void run() {
			
		}
		
	}

}
