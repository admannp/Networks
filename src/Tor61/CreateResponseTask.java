package Tor61;

import Tor61.Router.RoutingTableKey;


// Class that takes care of an extend when the current node is the last on the circuit
public class CreateResponseTask extends ResponseTask implements Runnable {
	Router router;
	RouterConnection returnConnection;
	String circuitID;
	RouterConnection forwardConnection;
	String forwardCircuitID;
	
	public CreateResponseTask(Router router, RouterConnection returnConnection, String circuitID, 
			RouterConnection forwardConnection, String forwardCircuit) {
		super();
		System.out.println("Constructing CreateResponseTask.");
		this.router = router;
		this.returnConnection = returnConnection;
		this.circuitID = circuitID;
		this.forwardCircuitID = forwardCircuit;
		this.forwardConnection = forwardConnection;
	}

	public void run() {
		System.out.println("Running CreateResponseTask");
		byte[] cell = this.waitOnResponse();
		System.out.println("Received data in buffer for CreateResponseTask.");
		CellFormatter.CellType type = CellFormatter.determineType(cell);
		// If the type we've received is what we expect
		if(router.checkTaskTypeReturn(type, CellFormatter.CellType.CREATED, CellFormatter.CellType.CREATE_FAILED)) {
			// TODO: update routing table
			
			// UPDATE ROUTING TABLE: incoming information on the socket we are responding to with the circuit ID we've 
			// been passed from it will be routed to the router that we've sent to with the circuit ID we used to send 
			// to that router
			// AND VICE-VERSA
			RoutingTableKey key = router.new RoutingTableKey(returnConnection.socket, circuitID);
			router.routingTable.put(key, router.new RoutingTableValue(forwardConnection, forwardCircuitID));
			
			RoutingTableKey reverseKey = router.new RoutingTableKey(forwardConnection.socket, forwardCircuitID);
			router.routingTable.put(reverseKey, router.new RoutingTableValue(returnConnection, circuitID));
			
			
			System.out.println("Returning extend response from CreateResponseTask");
			String cellCircuitID = CellFormatter.getCircuitIDFromCell(cell);
			byte[] message = CellFormatter.relayExtendedCell(circuitID, "-1");
			System.out.println("Sending RELAY_EXTENDED on IP, port, circuitID: "+ returnConnection.socket.getInetAddress() + 
					", " + returnConnection.socket.getPort() + ", " + circuitID);
			returnConnection.send(message);
			
			
		} else {
			System.out.println("Response cell type was not expected type.");
		}
		
	}
}
