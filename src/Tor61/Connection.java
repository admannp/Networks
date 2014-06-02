package Tor61;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Queue;

public class Connection {
	private Queue<byte[]> writeBuffer;
	protected DataInputStream inputStream;
	protected DataOutputStream outputStream;
	protected Socket socket;
	
	public Connection() {
		
	}
	
	public Connection(Socket socket) {
		this.socket = socket;
		try {
			this.inputStream = new DataInputStream(socket.getInputStream());
			this.outputStream = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			System.out.println("Failed to construct Connection on socket. IP, port: " + socket.getInetAddress() + ", " + socket.getPort());
			System.out.println(e.getMessage());
		}
	}
	
	public void send(byte[] bytes) {
		
	}
	
}
