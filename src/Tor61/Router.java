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
import java.util.Map;
import java.util.Random;

import registrationProtocol.ProbeHandler;
import registrationProtocol.RegistrationHandler;

public class Router {
	
	public Map<RoutingTableKey, Connection> routingTable;
	public RouterConnection circuitBeginning;
	
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
		
		// Initialize routing table
		// Use a synchronized map for safe access from multiple threads
		System.out.println("Initializing the routing table.");
		routingTable = Collections.synchronizedMap(new HashMap<RoutingTableKey, Connection>());
		
		// Start the accepting port
		torNodeListener acceptPort = new torNodeListener();
		(new Thread(acceptPort)).start();
		int portNum = acceptPort.port;
		System.out.println("Creating Tor61 listening/accepting thread on port: " + portNum);
		
		// Register this router
		System.out.println("Registering router as router number: " + instanceNumber);
		RegistrationHandler registrationHandler = new RegistrationHandler(registrationServiceAddress, registrationPortInt);
		int groupNum = Integer.parseInt(groupNumber);
		int groupNumHex = Integer.parseInt(groupNumber, 16);
		int instanceNum = Integer.parseInt(instanceNumber);
		int instanceNumHex = Integer.parseInt(instanceNumber, 16);
		// Register this Tor61 router with given group and instance numbers, to be contacted at the given portNum
		registrationHandler.register("Tor61Router-" + groupNum + "-" + instanceNum, getAddress(), "" + portNum, "" + (groupNumHex << 16 | instanceNumHex));
		System.out.println("Router registered");
		
		// Print out all registered routers
		String[][] routers = registrationHandler.fetch("Tor61Router-" + groupNum);
		Random r = new Random();
		int numNeighbors = routers.length;
		int neighborToChoose = r.nextInt(numNeighbors);
		for (String[] router : routers) {
			System.out.println("Router:");
			for (String s : router) {
				System.out.println(s);
			}
		}
		String[] neighbor = routers[neighborToChoose];
		try {
			System.out.println("Creating socket to neighbor node");
			Socket neighborSocket = new Socket(neighbor[0], Integer.parseInt(neighbor[1]));
			RouterConnection neighborConnection = new RouterConnection(neighborSocket);
			circuitBeginning = neighborConnection;
		} catch (NumberFormatException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// BEGIN THE CELL CREATION! DUN-DUN-DUUUUN
		int openedAgentID = Integer.parseInt(neighbor[2]);// + "").substring(4);
		//byte[] openCell = createOpenCell(instanceNumber, openedAgentID);
		//System.out.println("Sending the following open cell: " + openCell.toString());
		
	}
	
	// Private class used in the routing table storing the necessary information
	// for keys in the table
	private class RoutingTableKey {
		int port;
		int circuitNumber;
		int streamNumber;
		
		public RoutingTableKey(int port, int cN, int sN) {
			this.port = port;
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
			result = prime * result + circuitNumber;
			result = prime * result + port;
			result = prime * result + streamNumber;
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
			if (circuitNumber != other.circuitNumber)
				return false;
			if (port != other.port)
				return false;
			if (streamNumber != other.streamNumber)
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
		
		// Accept port number, listen on it
		torNodeListener() {
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
					System.out.println("Accepting new connection and building routerConnection.");
					RouterConnection connection = new RouterConnection(routerConnection);
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
	
	// Get the useful address of the protocol
	// Includes a check to make sure this is not a LAN specific address
	private static String getAddress() {
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
