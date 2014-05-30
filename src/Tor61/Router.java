package Tor61;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;

import registrationProtocol.RegistrationHandler;

public class Router {
	
	private static final int NUM_HOPS = 3;
	
	public Map<RoutingTableKey, Connection> routingTable;
	public RouterConnection thisCircuit;
	public Map<String, RouterConnection> connections;
	// Maps from input circuit / socket to Queue
	public Map<RoutingTableKey, Queue<byte[]>> requestResponseMap;
	public int numHops;
	
	// Passed in reference to containing node, host name of registration service, and the port
	// to contact the registration service at.
	public Router(Node node, String registrationServiceAddress, int registrationPortInt, String groupNumber, String instanceNumber) {

		// 1. Initialize routing table
		// 2. Start the accepting port
		// 3. Register this node
		// 4. Create the circuit for this router
			// Create new Router Connection to send from
				// Do the following by calling methods that belong to the RouterConnection:	
				// Methods are in RouterConnection to allow flexibility: if an incoming message indicates
				// that one of these methods should be called it can be done by the RouterConnection, 
				// and does not have to involve this router. Further the circuit creation process will be 
				// orchestrated here so that the same methods/behavior can be used in different contexts.
				// IE the methods of RouterConnection do not do to large of a piece of work
			// Call router connection's connect method
			// Call router connection's open method
			// Repeat if failed
			// Relay extend
		
		// There are no hops on this circuit before its creation
		numHops = 0;
		System.out.println("Circuit currently contains " + numHops + " hops.");
		
		// Initialize set of connections
		System.out.println("Creating connections Map");
		connections = new HashMap<String, RouterConnection>();
		
		requestResponseMap = Collections.synchronizedMap(new HashMap<RoutingTableKey, Queue<byte[]>>());
		
		// Initialize routing table
		// Use a synchronized map for safe access from multiple threads
		System.out.println("Initializing the routing table.");
		routingTable = Collections.synchronizedMap(new HashMap<RoutingTableKey, Connection>());
		
		// Start the accepting port
		torNodeListener acceptPort = new torNodeListener(this);
		(new Thread(acceptPort)).start();
		int portNum = acceptPort.port;
		System.out.println("Creating Tor61 listening/accepting thread on port: " + portNum);
		
		/************************** REGISTER NODE *****************************/
		
		RegistrationHandler registrationHandler;
		while (true) {	
			// Register this router, get the RegistrationHandler object to use to query the registration service
			String response = "";
			registrationHandler = register(registrationServiceAddress, registrationPortInt, groupNumber, instanceNumber, portNum);
			if (registrationHandler == null) {
				System.out.println("REGISTRATION INFORMATION");
				System.out.println("Registration service address: 	" + registrationServiceAddress);
				System.out.println("Registration service port: 		" + registrationPortInt);
				System.out.println("Group number:					" + groupNumber);
				System.out.println("Instance number: 				" + instanceNumber);
				System.out.println("This port number: 				" + portNum);
				System.out.print("Would you like to try again? (y/n): ");
				response = new Scanner(System.in).next();
				if (response.equals("y")) {
					continue;
				}
			}
			System.out.println("Registration process complete.");
			if (response.equals("n")) {
				System.exit(1);
			}
			break;
		}
		
		/*
		 * Router calls router connection's create method and inserts a new row
		 * into the response map. This response map has key->value pairs of
		 * circuit / socket -> buffer. The router connection's create method
		 * makes a special "circuit creation create" thread to watch for the
		 * created cell. Once the created cell comes in, the router connection
		 * looks up in the response map to find which buffer to use. The router
		 * class watches this buffer for it to fill with some data. Once this
		 * occurs, it can continue by extending.
		 * 
		 * To extend, the router calls the router connection's extend method,
		 * which makes a special "circuit creation extend" thread for watch for
		 * an extended cell. As before, it will look up in the router's response
		 * map for the appropriate buffer to write to, and the router will keep
		 * checking for content from the buffer.
		 * 
		 * The router then makes one more extend call, repeating the process above.
		 * Once this completes, a circuit is established.
		 * 
		 * Note: Router connections will normally produce a "wait for created"
		 * or "wait for extended" thread to watch for responses to their
		 * create or extend cells. Circuit creation is a special case and thus
		 * requires these unique thread classes.
		 */
		
		/************************** START CIRCUIT CREATION *****************************/
		
		// Get all registered routers, and return information about a random one
		String[] nextCircuitNode = getRegisteredRouter(registrationHandler, groupNumber);
		
		// In this case only, "thisCircuit" should be set to the successfully connected node
		System.out.println("Creating first circuit hop to random neighbor node");
		try {
			System.out.println("Creating TCP connection.");
			Socket nextCircuitNodeSocket = new Socket(nextCircuitNode[0], Integer.parseInt(nextCircuitNode[1]));
			// Set "this circuit" to newly created RouterConnection
			thisCircuit = new RouterConnection(nextCircuitNodeSocket, this);
			(new Thread(thisCircuit)).start();
		} catch (NumberFormatException | IOException e) {
			// TODO Auto-generated catch block
			System.out.println("FAILED TO CONNECT TO NEIGHBOR NODE");
			e.printStackTrace();
		}
		
		// Send the open cell, with timeout
		System.out.println("Sending open cell.");
		boolean ret = false;
		while (!ret) {
			ret = openConnection(instanceNumber, nextCircuitNode, thisCircuit) && createConnection(thisCircuit);
		}
		
		// Success?
		
		
		// TODO: figure out how to add connections to the connections map when
		// they're incoming from another router
	}
	
	// TODO: don't forget timeout
	/**
	 * Send an open cell to the given next node, using information provided by the next 
	 * node information. On success adds the connection to the map of existing connections.
	 * @param nextNodeInformation, string array that stores information about the cell to 
	 * which we want to open a connection
	 * @param nextNodeConnection, RouterConnection already with a TCP connection to the next
	 * hop router
	 */
	private boolean openConnection(String thisAgentID, String[] nextNodeInformation, RouterConnection nextNodeConnection) {
		// Create open cell with agent IDs 
		
		// TODO: get the proper other agent ID
		int[] instanceGroupNumbers = convertGroupStringToInts(nextNodeInformation[2]);
		String otherAgentID = instanceGroupNumbers[0] + "";
		byte[] openCell = CellFormatter.openCell(thisAgentID, otherAgentID);
		System.out.println("Sending open cell");
		// Create queue for listening to response to request
		Queue<byte[]> responseQueue = new LinkedList<byte[]>();
		// Put the queue in the map, with a key associated with this open request
		RoutingTableKey key = new RoutingTableKey(nextNodeConnection.connection, thisAgentID, otherAgentID);
		requestResponseMap.put(key, responseQueue);
		System.out.println("Hash code for key in openConnection: " + key.hashCode());
		// Start your engines!!!
		ResponseTask task = new ResponseTask(responseQueue);
		// Send open method
		nextNodeConnection.send(openCell);
		task.run();
		if (task.type == CellFormatter.CellType.OPENED) {
			System.out.println("SUCCESS: Request to open returned OPENED");
			// Add this connection to the connections map
			connections.put(nextNodeInformation[2], nextNodeConnection);
			return true;
		} else if (task.type == CellFormatter.CellType.OPEN_FAILED) {
			System.out.println("FAILURE: Request to open returned OPEN_FAILED");
			return false;
			// TODO: Error handling
		} else {
			System.out.println("FAILURE: Request to open returned an inappropriate response");
			return false;
			// TODO: Error handling
		}
	}
	
	/**
	 * Sends a create cell on a supplied RouterConnection. The RouterConnection
	 * must have already sent an open cell to establish a Tor61 connection.
	 */
	private boolean createConnection(RouterConnection node) {
		
		// PICK A CIRCUIT ID FOR THIS CIRCUIT
		Random r = new Random();
		short circuitID = (short) r.nextInt(Short.MAX_VALUE + 1);
		// add it to the list of used circuit IDs to ensure future IDs are unique
		node.circuitIDs.add(circuitID);
		System.out.println("Sending create cell with circuit ID: " + circuitID);
		
		// CREATE A CREATE CELL WITH GIVEN CELL ID
		byte[] response = CellFormatter.createCell("" + circuitID);
		// Create queue for listening to response to request
		Queue<byte[]> responseQueue = new LinkedList<byte[]>();
		// Put the queue in the map, with a key associated with this open request
		RoutingTableKey key = new RoutingTableKey(node.connection, ""+ circuitID, "-1");
		requestResponseMap.put(key, responseQueue);
		System.out.println("Hash code for key in createConnection: " + key.hashCode());
		// Start your engines!!!
		ResponseTask task = new ResponseTask(responseQueue);
		// Send open method
		node.send(response);
		Thread t = new Thread(task);
		t.start();
		while (task.type == null) {
			// Wait...
		}
		if (task.type == CellFormatter.CellType.CREATED) {
			System.out.println("SUCCESS: Request to create returned CREATED");
			return true;
		} else if (task.type == CellFormatter.CellType.CREATE_FAILED) {
			System.out.println("FAILURE: Request to create returned CREATE_FAILED");
			return false;
			// TODO: Error handling
		} else {
			System.out.println("FAILURE: Request to create returned an inappropriate response");
			return false;
			// TODO: Error handling
		}
		
	}
	
	/**
	 * Registers the node with given group and instance numbers associated with a give listening port 
	 * with the registration service at the given address and port.
	 *
	 * @param registrationServiceAddress, address of the registration service
	 * @param registrationPortInt, port of the registration service
	 * @param groupNumber, group number for this node
	 * @param instanceNumber, instance number of this node
	 * @param portNum, port this node listens for new connections on
	 * @return the registration handler object that handles registration related queries
	 */
	private RegistrationHandler register(String registrationServiceAddress, int registrationPortInt, String groupNumber, String instanceNumber, int portNum) {
		System.out.println("Registering router as router number: " + instanceNumber);
		RegistrationHandler registrationHandler = new RegistrationHandler(registrationServiceAddress, registrationPortInt);
		int groupNum = Integer.parseInt(groupNumber);
		int groupNumHex = Integer.parseInt(groupNumber, 16);
		int instanceNum = Integer.parseInt(instanceNumber);
		int instanceNumHex = Integer.parseInt(instanceNumber, 16);
		// Register this Tor61 router with given group and instance numbers, to be contacted at the given portNum
		int ret = registrationHandler.register("Tor61Router-" + groupNum + "-" + instanceNum, getAddress(), "" + portNum, "" + (groupNumHex << 16 | instanceNumHex));
		if (ret == -1) {
			System.out.println("Node registration failed.");
			return null;
		}
		System.out.println("Node registered");
		return registrationHandler;
	}
	
	/**
	 * Choose a random router from the list of registered routers with the given prefix
	 * @param registrationHandler, the registration handler communicating with the registration service
	 * given to the node's constructor
	 * @param prefix, String prefix to use when fetching list of available routers
	 * @return the string array containing information about the randomly chosen router
	 */
	private String[] getRegisteredRouter(RegistrationHandler registrationHandler, String prefix) {
		String[][] routers = registrationHandler.fetch("Tor61Router-" + Integer.parseInt(prefix));
		Random r = new Random();
		int numNeighbors = routers.length;
		int neighborToChoose = r.nextInt(numNeighbors);
		// For debugging, print connected routers
		for (String[] router : routers) {
			System.out.println("Router:");
			for (String s : router) {
				System.out.println(s);
			}
		}
		// Return a randomly chosen neighbor Tor node
		return routers[neighborToChoose];
	}
	
	/**
	 * Given the service data number attached to a call on fetch, returns an
	 * int[] where index 0 is the group number and index 1 is the instance
	 * number.
	 * 
	 * @param s, a string representing the service data number
	 * @return int[], the group number in index 0 and instance number in
	 * 		   index 1
	 */
	private static int[] convertGroupStringToInts(String s) {
		String hexGroup = Integer.toHexString(Integer.parseInt(s));
		String instanceNumber = hexGroup.substring(hexGroup.length() - 4, hexGroup.length());
		String groupNumber = hexGroup.substring(0, hexGroup.length() - 4);
		return new int[] {Integer.parseInt(instanceNumber), Integer.parseInt(groupNumber)};
	}
	
	
	private class ResponseTask implements Runnable {
		private Queue<byte[]> response;
		public CellFormatter.CellType type;
		
		public ResponseTask(Queue<byte[]> responseQueue) {
			System.out.println("Creating ResponseTask.");
			this.response = responseQueue;
			type = null;
		}
		
		public void run() {
			System.out.println("Running response task.");
			while(true) {
				if (!response.isEmpty()) {
					// Grab cell
					// Switch on type of cell
					System.out.println("The ResponseTask has seen an item. Removing the cell.");
					//System.exit(0);
					byte[] cell = response.remove();
					CellFormatter.CellType type = CellFormatter.determineType(cell);
					System.out.println("The cell is of type " + type);
					this.type = type; 
					break;
				}
			}
		}
		
	}
	
	// Private class used in the routing table storing the necessary information
	// for keys in the table
	public class RoutingTableKey {
		Socket socket;
		String circuitNumber;
		String streamNumber;
		
		public RoutingTableKey(Socket socket, String cN, String sN) {
			this.socket = socket;
			circuitNumber = cN;
			streamNumber = sN;
		}

		// So that we can use a hash map with this custom object and get expected
		// behavior
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + circuitNumber.hashCode();
			result = prime * result + socket.hashCode();
			result = prime * result + streamNumber.hashCode();
			return result;
		}
		
		// So that an equals call will produce expected results for this 
		// custom class
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			RoutingTableKey other = (RoutingTableKey) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (!circuitNumber.equals(other.circuitNumber))
				return false;
			if (socket != other.socket)
				return false;
			if (!streamNumber.equals(other.streamNumber))
				return false;
			return true;
		}

		private Router getOuterType() {
			return Router.this;
		}
		
	}
	
	// Private class that acts at the Tor listener, listening for and accepting 
	// TCP connections from other Tor61 nodes
	private class torNodeListener implements Runnable {
		
		// Port to listen on
		ServerSocket routerListener;
		int port;
		Router router;
		
		// Accept port number, listen on it
		torNodeListener(Router router) {
			this.router = router;
			try {
				
				routerListener = new ServerSocket(0);
				port = routerListener.getLocalPort();
				System.out.println("Listening for Tor connections on port: " + port);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// Listen for connections to the port
		// Spin off new RouterConnection to handle new connections
		@Override
		public void run() {
			while (true) {
				try {
					Socket routerConnection = routerListener.accept();
					System.out.println("Accepted new Tor connection; building routerConnection. Local port: " + routerConnection.getLocalPort());
					RouterConnection connection = new RouterConnection(routerConnection, router);
					(new Thread(connection)).start();
					System.out.println("New RouterConnection created in Router class.");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.err.println(e.getMessage());
				}
			}
		}
		
	}
	
	/**
	 * Get the address of the local machine this code is running on. Robust in that it
	 * avoids returning LAN-specific IP addresses
	 * @return the address of the local machine
	 */
	private String getAddress() {
		String localAddress = "";
		try {
			localAddress = Inet4Address.getLocalHost().getHostAddress();
		
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
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return localAddress;
	}

}
