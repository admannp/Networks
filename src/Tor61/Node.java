package Tor61;

import java.util.Map;


public class Node {
	Proxy proxy;
	Router router;
	RouterConnection circuit;
	// Stores stream ID -> Proxy Connection
	Map<StreamTableKey, Connection> streamTable;
	
	public Node(String registrationServiceAddress, String registrationPort, String groupNumber, String instanceNumber, String HTTPProxyPort) {
		//TODO: error handling on the above parameters
		
		int registrationPortInt = Integer.parseInt(registrationPort);
		router = new Router(this, registrationServiceAddress, registrationPortInt, groupNumber, instanceNumber);
		proxy = new Proxy(this, Integer.parseInt(HTTPProxyPort));
	}
	
	public static void main(String[] args) {
		new Node("attu1.cs.washington.edu", "46101", "3004", "3000", "46110");
	}
}
