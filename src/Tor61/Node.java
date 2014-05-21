package Tor61;


public class Node {
	private Proxy proxy;
	private Router router;
	
	public Node(String registrationServiceAddress, String registrationPort, String groupNumber, String instanceNumber, String HTTPProxyPort) {
		//TODO: error handling on the above parameters
		
		int registrationPortInt = Integer.parseInt(registrationPort);
		router = new Router(this, registrationServiceAddress, registrationPortInt, groupNumber, instanceNumber);
		proxy = new Proxy(this, Integer.parseInt(HTTPProxyPort));
	}
	
	public static void main(String[] args) {
		new Node("cse461.cs.washington.edu", "46101", "3004", "3", "46110");
	}
}
