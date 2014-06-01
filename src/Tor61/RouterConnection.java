package Tor61;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

/*
 * Colin Miller, Nick Adman
 * 
 * Router connection class that encapsulates sending and receiving data from a router portion
 * of a given node to another node on some circuit
 * 
 * Should be connected to just one other node; each connection from this router to another will
 * be handled by a unique RouterConnection
 * 
 */

public class RouterConnection extends Connection {
	
	// The connection to another node
	Socket connection;
	// Buffer to write to - handled by separate thread
	RouterConnectionWriteBuffer writeBuffer;
	Router router;
	// True is even, false is odd
	boolean parity;
	
	Set<Short> circuitIDs;
	
	/**
	 * Creates a new RouterConnection associated with the given socket. The RouterConnection
	 * has two threads: one handles reading and the other writing on the given socket.
	 * 
	 * @param connection, The socket connected to the foreign router
	 */
	public RouterConnection(Socket connection, Router router, boolean parity) {
		this.router = router;
		System.out.println("ROUTER CONNECTION CREATED. ADDRESS, PORT: "+ connection.getLocalAddress() + ", " + connection.getLocalPort());
		this.connection = connection;
		circuitIDs = new HashSet<Short>();
		writeBuffer = new RouterConnectionWriteBuffer();
		(new Thread(writeBuffer)).start();
		this.parity = parity;
	}
	
	// TODO: make a circuit ID returning method
	
	/**
	 * Sends the given byte array along the circuit associated with this RouterConnection
	 * @param cell, the byte array to be sent
	 */
	public void send(byte[] cell) {
		writeBuffer.put(cell);
	}
	
	
	/**
	 * Handles listening for data from the foreign router
	 */
	@Override
	public void run() {	
		try {
			System.out.println("Waiting for incoming cells.");
			
			// Prepare to accept input stream from server
			DataInputStream input = new DataInputStream(connection.getInputStream());
			
			
			while (true) {
				
				// Create a new output stream for each incoming message
				ByteArrayOutputStream incomingMessageStream = new ByteArrayOutputStream();
				// Accept bytes from server, writing into given buffer 
				byte buffer[] = new byte[512];
				int bytesRead;
				int totalBytesRead = 0;
				while ((bytesRead = input.read(buffer)) != -1) {
					totalBytesRead += bytesRead;
					System.out.println("Beginning one iteration; bytesRead = " + bytesRead);
					incomingMessageStream.write(buffer, 0, bytesRead);
					System.out.println("Finished one iteration");
					if (totalBytesRead == 512) {
						break;
					}
				}
				//System.out.println("Done with that");
				byte[] incomingMessage = incomingMessageStream.toByteArray();
					
				//System.out.println(Arrays.toString(incomingMessage));
				CellFormatter.CellType type = CellFormatter.determineType(incomingMessage);
				System.out.println(type);
				byte[] response;
				Router.RoutingTableKey key;
				switch(type) {
					case OPEN:
						// TODO: figure out how to do timeouts
						// TODO: put the connection in the connection map
						// Correct??
						router.connections.put("" + connection.getInetAddress() + connection.getPort(), this);
						
						String[] IDs = CellFormatter.getIDsFromOpenCell(incomingMessage);
						response = CellFormatter.openedCell(IDs[0], IDs[1]);
						System.out.println("Received open cell with IDs: " + IDs[0] + " " + IDs[1]);
						send(response);
						break;
					case OPENED:
						// TODO: put the connection in the connection map
						// Here correct too??
						router.connections.put("" + connection.getInetAddress() + connection.getPort(), this);
						
						String[] agentIDs = CellFormatter.getIDsFromOpenCell(incomingMessage);
						key = router.new RoutingTableKey(this, agentIDs[0] + agentIDs[1]);
						if (router.requestResponseMap.containsKey(key)) {
							router.requestResponseMap.get(key).add(incomingMessage);
							router.requestResponseMap.remove(key);
						}
						break;
					case OPEN_FAILED:
						// TODO: Error handling
						System.out.println("Open failed");
						break;
					case CREATE:
						String circuitID = CellFormatter.getCircuitIDFromCell(incomingMessage);
						System.out.println("Received a create cell, responding with created. ID: " + circuitID);
						key = router.new RoutingTableKey(this, circuitID);
						router.routingTable.put(key, null);
						response = CellFormatter.createdCell("" + circuitID);
						send(response);
						break;
					case CREATED:
						String responseCircuitID = CellFormatter.getCircuitIDFromCell(incomingMessage);
						key = router.new RoutingTableKey(this, responseCircuitID);
						router.routingTable.put(key, null);
						if (router.requestResponseMap.containsKey(key)) {
							router.requestResponseMap.get(key).add(incomingMessage);
							router.requestResponseMap.remove(key);
						}
						System.out.println("Created circuit, now extending.");
						break;
					case CREATE_FAILED:
						break;
					case DESTROY:
						break;
					case RELAY_BEGIN:
						break;
					case RELAY_DATA:
						break;
					case RELAY_END:
						break;
					case RELAY_CONNECTED:
						break;
					case RELAY_EXTEND:
						
						/*******
						 * TOOO DOOOOOOOO
						 * 
						 * Check routing tabling to see if we send extend along or take action
						 * 
						 * If take action -> check to see if connection exists already (as is done below)
						 * 
						 */
						
						circuitID = CellFormatter.getCircuitIDFromCell(incomingMessage);
						String[] extendInformation = CellFormatter.getRelayExtendInformation(incomingMessage);
						/*
						 * extendInformation[0] -> IP of node we want
						 * extendInformation[1] -> port of node we want
						 * extendInformation[2] -> agentID of node we want
						 */
						if (router.connections.containsKey(extendInformation[0] + extendInformation[1])) {
							// The node already has a connection. Let's grab it and do work.
							RouterConnection connectionToNode = router.connections.get(extendInformation[0] + extendInformation[1]);
							
							// Key is associated with the RouterConnection in which we expect to hear the response
							// and the circuit it will be on
							key = router.new RoutingTableKey(connectionToNode, circuitID);
							CreateResponseTask task = new CreateResponseTask(router, this);
							router.requestResponseMap.put(key, task.response);
						} // TODO: deal with the case that it does not already have a connection
						
						
						
						// key = router.new RoutingTableKey();
						break;
					case RELAY_EXTENDED:
						break;
					case RELAY_BEGIN_FAILED:
						break;
					case RELAY_EXTEND_FAILED:
						break;
					case UNKNOWN:
						// TODO: don't lose points for this
						System.out.println("HOLY SHIT, ERROR!");
						System.exit(0);
						break;
				}
				System.out.println("Received incoming cell to router connection at " + 
									connection.getLocalAddress() + ", " + connection.getLocalPort());
				
			}
		} catch (IOException e) {
			System.out.println("Unable to read information incoming to router connection at: " + 
								connection.getLocalAddress() + ", " + connection.getLocalPort());
			e.printStackTrace();
		}
	}
	
	// Private inner class used to write information to the next step of the 
	// circuit this router connection is attached to
	private class RouterConnectionWriteBuffer implements Runnable {
		Queue<byte[]> buffer;
		DataOutputStream out;
		
		/**
		 * Create a new RouterConnectionWriteBuffer to handle the writing of information
		 * along this circuit
		 */
		public RouterConnectionWriteBuffer() {
			// Using linked list for the queue
			buffer = new LinkedBlockingQueue<byte[]>();
			try {
				out = new DataOutputStream(connection.getOutputStream());
			} catch (IOException e) {
				System.out.println("Unable to create output stream for router connection at: " +
									connection.getLocalAddress() + ", " + connection.getLocalPort());
				e.getMessage();
			}
		}
		
		/**
		 * Put the given byte array into the write buffer (byte array is expected to be in some
		 * cell format)
		 * @param cell
		 */
		public void put(byte[] cell) {
			buffer.add(cell);
		}
		
		// Loop forever. When information is in the buffer, send it along the connection
		public void run() {
			while (true) {
				if(!buffer.isEmpty()) {
					System.out.println("Received data in write buffer for router connection at " + 
										connection.getLocalAddress() + ", " + connection.getLocalPort());
					System.out.println("Now sending data.");
					try {
						byte[] cell = buffer.remove();
						//System.out.println("Data to be sent: " + Arrays.toString(cell));
						out.write(cell);
					} catch (IOException e) {
						System.out.println("Unable to write to output stream for router connection at: " +
								connection.getLocalAddress() + ", " + connection.getLocalPort());
						e.printStackTrace();
					}
				}
			}
		}	
	}
}
