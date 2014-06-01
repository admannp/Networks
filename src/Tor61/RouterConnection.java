package Tor61;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import Tor61.Router.RoutingTableKey;
import Tor61.Router.RoutingTableValue;

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
	Socket socket;
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
		this.socket = connection;
		circuitIDs = Collections.synchronizedSet(new HashSet<Short>());
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
			DataInputStream input = new DataInputStream(socket.getInputStream());
			
			
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
				String circuitID;
				String thisIPAddress = socket.getInetAddress().toString().substring(1);
				switch(type) {
					case OPEN:
						// TODO: figure out how to do timeouts
						// TODO: put the connection in the connection map
						// Correct??
						
						router.connections.put("" + thisIPAddress + socket.getPort(), this);
						
						String[] IDs = CellFormatter.getIDsFromOpenCell(incomingMessage);
						response = CellFormatter.openedCell(IDs[0], IDs[1]);
						System.out.println("Received open cell with IDs: " + IDs[0] + " " + IDs[1]);
						send(response);
						break;
					case OPENED:
						// TODO: put the connection in the connection map
						// Here correct too??
						router.connections.put("" + thisIPAddress + socket.getPort(), this);
						System.out.println("This socket's hash code(in RouterConnection): " + socket.hashCode());
						String[] agentIDs = CellFormatter.getIDsFromOpenCell(incomingMessage);
						System.out.println("ID: " + agentIDs[0] + agentIDs[1]);
						key = router.new RoutingTableKey(this.socket, agentIDs[0] + agentIDs[1]);
						System.out.println("Key's hash (in RouterConnection): " + key.hashCode());
						
						System.out.println(router.requestResponseMap.containsKey(key));
						System.out.println("Hashes for keys in requestResponseMap:");
						for (RoutingTableKey keySetKey : router.requestResponseMap.keySet()) {
							System.out.println("Key: " + keySetKey.hashCode());
							System.out.println("This key is equal to what we want :" + (keySetKey.equals(key)));
						}
						
						if (router.requestResponseMap.containsKey(key)) {
							System.out.println("In here");
							router.requestResponseMap.get(key).add(incomingMessage);
							router.requestResponseMap.remove(key);
						}
						break;
					case OPEN_FAILED:
						// TODO: Error handling
						System.out.println("Open failed");
						break;
					case CREATE:
						circuitID = CellFormatter.getCircuitIDFromCell(incomingMessage);
						System.out.println("Received a create cell, responding with created. ID: " + circuitID);
						key = router.new RoutingTableKey(this.socket, circuitID);
						router.routingTable.put(key, null);
						response = CellFormatter.createdCell("" + circuitID);
						send(response);
						break;
					case CREATED:
						circuitID = CellFormatter.getCircuitIDFromCell(incomingMessage);
						key = router.new RoutingTableKey(this.socket, circuitID);
						router.routingTable.put(key, null);
						System.out.println("Received created for address, port, circuitID: " + thisIPAddress + 
								", " + this.socket.getPort() + ", " + circuitID);
						if (router.requestResponseMap.containsKey(key)) {
							System.out.println("Putting cell in Created ResponseTask");
							router.requestResponseMap.get(key).add(incomingMessage);
							router.requestResponseMap.remove(key);
						}
						System.out.println("Finished responding to CREATED cell.");
						break;
					case CREATE_FAILED:
						break;
					case DESTROY:
						break;
					case RELAY_BEGIN:
						
						// LOOK IN ROUTING TABLE
						//String[] relayInformation = CellFormatter
						
						// IF CONNECTION NOT PRESENT 
							// DROP IT
						
						// IF CONNECTION PRESENT
							// change circuit ID
							// forward data as specified in the routing table
						
						// IF CONNECTION IS NULL
							// create da new proxyconnection and connect to the IP / port in the
							// relay begin cell
						
						break;
					case RELAY_DATA:
						// LOOK IN ROUTING TABLE
						//String[] relayInformation = CellFormatter
						
						// IF CONNECTION NOT PRESENT 
							// DROP IT
						
						// IF CONNECTION PRESENT
							// change circuit ID
							// forward data as specified in the routing table
						
						// IF CONNECTION IS NULL
							// create da new proxyconnection and connect to the IP / port in the
							// relay begin cell
						
						break;
					case RELAY_END:
						break;
					case RELAY_CONNECTED:
						// LOOK IN ROUTING TABLE
						//String[] relayInformation = CellFormatter
						
						// IF CONNECTION NOT PRESENT 
							// DROP IT
						
						// IF CONNECTION PRESENT
							// change circuit ID
							// forward data as specified in the routing table
						
						// IF CONNECTION IS NULL
							// create da new proxyconnection and connect to the IP / port in the
							// relay begin cell
						
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
						System.out.println("Received relay extended cell, now processing.");
						String previousCircuitID = CellFormatter.getCircuitIDFromCell(incomingMessage);
						String[] extendInformation = CellFormatter.getRelayExtendInformation(incomingMessage);
						String extendToAddress = extendInformation[0] + extendInformation[1];
						
						System.out.println("Received RELAY_EXTEND on IP, port, circuitID: "+ socket.getInetAddress() + 
								", " + socket.getPort() + ", " + previousCircuitID);
						key = router.new RoutingTableKey(this.socket, previousCircuitID);
						RoutingTableValue value = router.routingTable.get(key);
						if (value != null) {
							// Forward the information
							System.out.println("Routing table has entry for next node to extend to: " + extendToAddress);
							byte[] forwardExtendMessage = CellFormatter.relayExtendCell(value.circuitNumber, "-1", extendInformation[0], 
																						extendInformation[1], extendInformation[2]);
							key = router.new RoutingTableKey(value.connection.socket, value.circuitNumber);
							RelayResponseTask task = new RelayResponseTask(router, this, previousCircuitID);
							new Thread(task).start();
							router.requestResponseMap.put(key, task.response);
							
							value.connection.send(forwardExtendMessage);
						} else {
							
							// Go through the create process
							/*
							 * extendInformation[0] -> IP of node we want
							 * extendInformation[1] -> port of node we want
							 * extendInformation[2] -> agentID of node we want
							 */
							System.out.println("Routing table has null entry for next node to extend to: " + extendToAddress);
							if (router.connections.containsKey(extendToAddress)) {
								System.out.println("Routing table and connections both contain IP:PORT to extend to.");
								// GET THE ROUTER CONNECTION CONNECTED TO THE NODE TO WHICH WE ARE SENDING INFORMATION
								// The node already has a connection. Let's grab it and do work.
								RouterConnection connectionToNode = router.connections.get(extendToAddress);
								
								// GET NEW CIRCUIT NUMBER
								String newCircuitID = generateCircuitID() + "";
								
								// START THE CREATE RESPONSE TASK TO ACT ON SUCCESS
								// Key is associated with the RouterConnection in which we expect to hear the response
								// and the circuit it will be on
								// Task is given reference to "this", through which it will send the response
								System.out.println("Creating CreateResponseTask.");
								System.out.println("Putting the CreateResponseTask for address, port, circuitID: " + connectionToNode.socket.getInetAddress() + 
										", " + connectionToNode.socket.getPort() + ", " + newCircuitID);
								key = router.new RoutingTableKey(connectionToNode.socket, newCircuitID);
								CreateResponseTask task = new CreateResponseTask(router, this, previousCircuitID, connectionToNode, newCircuitID);
								new Thread(task).start();
								router.requestResponseMap.put(key, task.response);
								System.out.println("Sending create cell.");
								
								
								// SEND THE CREATE MESSAGE USING THE ROUTER CONNECTION'S SEND BUFFER
								byte[] createCell = CellFormatter.createCell("" + newCircuitID);
								connectionToNode.send(createCell);
								
							} // TODO: deal with the case that it does not already have a connection
							
						}
										
						break;
					case RELAY_EXTENDED:
						circuitID = CellFormatter.getCircuitIDFromCell(incomingMessage);
						System.out.println("Received RELAY_EXTENDED on IP, port, circuitID: "+ socket.getInetAddress() + 
								", " + socket.getPort() + ", " + circuitID);
						key = router.new RoutingTableKey(this.socket, circuitID);
						for (RoutingTableKey keykey : router.requestResponseMap.keySet())
							System.out.println(keykey.circuitNumber);
						if (router.requestResponseMap.containsKey(key)) {
							System.out.println("Putting cell in Created ResponseTask");
							router.requestResponseMap.get(key).add(incomingMessage);
							router.requestResponseMap.remove(key);
						}
						System.out.println("Relay extended, now propagating message back.");
						break;
					case RELAY_BEGIN_FAILED:
						break;
					case RELAY_EXTEND_FAILED:
						break;
					case UNKNOWN:
						System.out.println("Unknown cell type received");
//						System.exit(0);
						break;
				}
				System.out.println("Received incoming cell to router connection at " + 
									socket.getLocalAddress() + ", " + socket.getLocalPort());
				
			}
		} catch (IOException e) {
			System.out.println("Unable to read information incoming to router connection at: " + 
								socket.getLocalAddress() + ", " + socket.getLocalPort());
			e.printStackTrace();
		}
	}
	
	// GENERATE A SHORT CIRCUIT ID
	// (don't blow a fuse :D)
	public short generateCircuitID() {
		Short circuitID;
		do {
			Random r = new Random();
			circuitID = (short) r.nextInt(Short.MAX_VALUE + 1);
		} while (circuitIDs.contains(circuitID) || ((circuitID % 2 == 0) != parity));
		return circuitID;
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
				out = new DataOutputStream(socket.getOutputStream());
			} catch (IOException e) {
				System.out.println("Unable to create output stream for router connection at: " +
									socket.getLocalAddress() + ", " + socket.getLocalPort());
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
										socket.getLocalAddress() + ", " + socket.getLocalPort());
					System.out.println("Now sending data.");
					try {
						byte[] cell = buffer.remove();
						//System.out.println("Data to be sent: " + Arrays.toString(cell));
						out.write(cell);
					} catch (IOException e) {
						System.out.println("Unable to write to output stream for router connection at: " +
								socket.getLocalAddress() + ", " + socket.getLocalPort());
						e.printStackTrace();
					}
				}
			}
		}	
	}
}
