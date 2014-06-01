package Tor61;

public class RelayResponseTask extends ResponseTask implements Runnable {
	Router router;
	String circuitID;
	RouterConnection returnConnection;
	
	public RelayResponseTask(Router router, RouterConnection returnConnection, String circuitID) {
		super();
		System.out.println("Creating relay response task: circuitID: " + circuitID);
		this.router = router;
		this.circuitID = circuitID;
		this.returnConnection = returnConnection;
	}

	public void run() {
		System.out.println("Running relay response task");
		byte[] cell = this.waitOnResponse();
		System.out.println("Received data in buffer for relay response task.");
		CellFormatter.CellType type = CellFormatter.determineType(cell);
		// If the type we've received is what we expect
		if(router.checkTaskTypeReturn(type, CellFormatter.CellType.RELAY_EXTENDED, CellFormatter.CellType.RELAY_EXTEND_FAILED)) {
			
			System.out.println("Returning extend response from relay response task.");
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
