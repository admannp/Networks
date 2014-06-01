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

/*
 * TODO: (5/30)
 * 	- Extend functionality to include relay extend cells
 * 	- Deal with the threading issues involved in extending circuits as a middle node
 * 		- Have to create, wait for created, and propagate back.
 * 		- Have to extend, wait for extended, and propagate back.
 */



import java.util.TreeMap;

import registrationProtocol.RegistrationHandler;

public class Router {
	
	private static final int EXPECTED_NUM_HOPS = 3;
	
	
	// USE LIST OF SOCKETS, CHECK THEIR IP AND PORTS?
	// MAP TO PARITY?
	// MAP TO CONNECTION OBJECT?
	
	public Map<RoutingTableKey, RoutingTableKey> routingTable;
	public RouterConnection thisCircuit;
	public short thisCircuitID;
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
		

		// Initialize set of connections
		System.out.println("Creating connections Map");
		connections = Collections.synchronizedMap(new HashMap<String, RouterConnection>());
		
		requestResponseMap = Collections.synchronizedMap(new TreeMap<RoutingTableKey, Queue<byte[]>>());
		
		// Initialize routing table
		// Use a synchronized map for safe access from multiple threads
		System.out.println("Initializing the routing table.");
		routingTable = Collections.synchronizedMap(new HashMap<RoutingTableKey, RoutingTableKey>());
		
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
		
		// Get randomly chosen registered router's information
		String[] nextCircuitNode = getRegisteredRouter(registrationHandler, groupNumber);
		
		// TODO: Check that we are not already connected to this other node
		// TODO: In order to do this, must figure out how to register connections
		// other Tor nodes make with us
		
		// In this case only, "thisCircuit" should be set to the successfully connected node
		System.out.println("Creating first circuit hop to random neighbor node");
		try {
			System.out.println("Creating TCP connection.");
			System.out.println("nextCircuitNode[0] = " + nextCircuitNode[0] + "Integer.parseInt(nextCircuitNode[1] = " + Integer.parseInt(nextCircuitNode[1]));
			Socket nextCircuitNodeSocket = new Socket(nextCircuitNode[0], Integer.parseInt(nextCircuitNode[1]));
			// Set "this circuit" to newly created RouterConnection
			thisCircuit = new RouterConnection(nextCircuitNodeSocket, this, false);
			(new Thread(thisCircuit)).start();
		} catch (NumberFormatException | IOException e) {
			// TODO Auto-generated catch block
			System.out.println("FAILED TO CONNECT TO NEIGHBOR NODE");
			e.printStackTrace();
			System.exit(1);
		}
		
		// Send the open cell, with timeout
		System.out.println("Sending open cell.");
		boolean ret = false;
		// TODO: If ret returns false, try again
		ret = openCircuit(instanceNumber, nextCircuitNode, thisCircuit);
		
		ret = createCircuit(thisCircuit);
		
		// TODO: No need to put this into the routing table because incoming info
		// will be sent on "thisCircuit" ??
		
		// There are no hops on this circuit before its creation
		numHops = 1;
		System.out.println("Circuit currently contains " + numHops + " hops.");
		
		while(numHops != EXPECTED_NUM_HOPS) {
			//ret = extendCircuit(registrationHandler, groupNumber);
			numHops++;
			System.out.println("Circuit currently contains " + numHops + " hops.");
		}
		
		System.out.println("Keys in the connection map: " + connections.keySet());
		System.out.println("Keys in the routing table:  " + routingTable.keySet());
		System.out.println("Size of request/response map (should be 0): " + requestResponseMap.size());
		
		
		// TODO: figure out how to add connections to the connections map when
		// they're incoming from another router
	}
	
	/**
	 * Extend the length of the circuit by one node. 
	 */
	private boolean extendCircuit(RegistrationHandler registrationHandler, String groupNumber) {
		
		// RANDOMLY CHOOSE A NEW NODE
		// CREATE EXTEND CELL WITH IP:PORT agentid
		// Get randomly chosen registered router's information
		String[] nextCircuitNode = getRegisteredRouter(registrationHandler, groupNumber);
		
		// CREATE A RESPONSE TASK TO WAIT FOR RESPONSE, PUT IN MAP WITH (thisCircuit, circuit n) => task's queue
		// CREATE NEW RESPONSE TASK TO GRAB THE RESPONSE
		// Create response task, waiting on a response sent to the starting RouterConnection of the circuit
		// belonging to this node, and the circuit ID used for this circuit on this RouterConnection
		RoutingTableKey key = new RoutingTableKey(thisCircuit.socket, "" + thisCircuitID);
		ResponseTask task = new ResponseTask();
		requestResponseMap.put(key, task.response);
		
		// CREATE THE RELAY EXTEND CELL
		
		
		// SEND THE EXTEND CELL ON THIS CIRCUIT
		//thisCircuit.send(openCell);
		
		// WAIT FOR TASK TO GET RESPONSE
		byte[] cell = task.waitOnResponse();
		CellFormatter.CellType type = CellFormatter.determineType(cell);
		return checkTaskTypeReturn(type, CellFormatter.CellType.RELAY_EXTENDED, CellFormatter.CellType.RELAY_EXTEND_FAILED);

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
	private boolean openCircuit(String thisAgentID, String[] nextNodeInformation, RouterConnection thisConnection) {
		System.out.println("Opening circuit");
		
		// GET AGENT IDs AND CREATE OPEN CELL
		// TODO: get the proper other agent ID
		int[] instanceGroupNumbers = convertGroupStringToInts(nextNodeInformation[2]);
		String otherAgentID = instanceGroupNumbers[0] + "";
		System.out.println("OTHER AGENT ID!!!!!: " + otherAgentID);
		byte[] openCell = CellFormatter.openCell(thisAgentID, otherAgentID);
		
		// CREATE NEW RESPONSE TASK TO GRAB THE RESPONSE
		// Create response task
		System.out.println("This connection's hash (in router): " + thisConnection.hashCode());
		System.out.println("ID: " + thisAgentID + otherAgentID);
		RoutingTableKey key = new RoutingTableKey(thisConnection.socket, thisAgentID + otherAgentID);
		System.out.println("Key's hash (in Router): " + key.hashCode());
		ResponseTask task = new ResponseTask();
		requestResponseMap.put(key, task.response);
		System.out.println("Hash code for key in openConnection: " + key.hashCode());
		

		// Send the open cell
		// Use the response task to wait for response to the open cell

		thisConnection.send(openCell);
		byte[] cell = task.waitOnResponse();
		System.out.println("Stopped waiting for open");
		CellFormatter.CellType type = CellFormatter.determineType(cell);
		return checkTaskTypeReturn(type, CellFormatter.CellType.OPENED, CellFormatter.CellType.OPEN_FAILED);
	}
	
	/**
	 * Sends a create cell on a supplied RouterConnection. The RouterConnection
	 * must have already sent an open cell to establish a Tor61 connection.
	 */
	private boolean createCircuit(RouterConnection node) {
		
		// PICK A CIRCUIT ID FOR THIS CIRCUIT
		// TODO: USE THE ROUTERCONNECTION OBJECT ITSELF TO GENERATE A NEW CIRCUIT ID!!
		Random r = new Random();
		thisCircuitID = (short) r.nextInt(Short.MAX_VALUE + 1);
		
		// add it to the list of used circuit IDs to ensure future IDs are unique
		node.circuitIDs.add(thisCircuitID);
		System.out.println("Sending create cell with circuit ID: " + thisCircuitID);
		
		// CREATE A CREATE CELL WITH GIVEN CELL ID
		byte[] createCell = CellFormatter.createCell("" + thisCircuitID);
		
		// PUT RESPONSE QUEUE INTO MAP, FOR USE ON RETURN
		// Create queue for listening to response to request
		// And put the queue in the map, with a key associated with this open request
		RoutingTableKey key = new RoutingTableKey(node.socket, "" + thisCircuitID);
		ResponseTask task = new ResponseTask();
		requestResponseMap.put(key, task.response);
		System.out.println("Hash code for key in createConnection: " + key.hashCode());
		
		// CREATE NEW RESPONSE TASK TO GRAB THE RESPONSE
		// Create response task
		// Send the create cell
		// Use the response task to wait for response
		
		node.send(createCell);
		byte[] cell = task.waitOnResponse();
		
		/********************
		 * TO DOOOOOOOOOOOOO
		 * 
		 * In our Routing table put 
		 * 		(node, circuitNum) => null
		 * 
		 * 
		 */
		
		CellFormatter.CellType type = CellFormatter.determineType(cell);
		
		return checkTaskTypeReturn(type, CellFormatter.CellType.CREATED, CellFormatter.CellType.CREATE_FAILED);
	}
	
	/**
	 * Checks the a Cell Formatter Cell Type to check if it is either the expected success
	 * or expected fail type, print out appropriate message. If type is expected success type
	 * returns true, else returns false.
	 * @param returnedType, type that is to be checked
	 * @param expectedSuccessType, type that is expected on success
	 * @param expectedFailureType, type that is expected on failure
	 * @return
	 */
	public boolean checkTaskTypeReturn(CellFormatter.CellType returnedType, CellFormatter.CellType expectedSuccessType,  
			CellFormatter.CellType expectedFailureType) {
		if (returnedType == expectedSuccessType) {
			System.out.println("SUCCESS: Request to create returned " + expectedSuccessType);
			return true;
		} else if (returnedType == expectedFailureType) {
			System.out.println("FAILURE: Request to create returned " + expectedSuccessType);
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
	
	// Private class used in the routing table storing the necessary information
	// for keys in the table
	public class RoutingTableKey {
		Socket socket;
		String circuitNumber;
		
		public RoutingTableKey(Socket socket, String cN) {
			this.socket = socket;
			circuitNumber = cN;
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
			if (socket.equals(other.socket))
				return false;
			return true;
		}

		private Router getOuterType() {
			return Router.this;
		}
		
	}
	
	// Private class used in the routing table storing the necessary information
	// for keys in the table
	public class RoutingTableValue {
		RouterConnection connection;
		String circuitNumber;
		
		public RoutingTableValue(RouterConnection connection, String cN) {
			this.connection = connection;
			circuitNumber = cN;
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
					RouterConnection connection = new RouterConnection(routerConnection, router, true);
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
