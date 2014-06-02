package Tor61;

import java.io.IOException;
import java.net.Socket;

import Tor61.Router.RoutingTableKey;

public class OpenResponseTask extends ResponseTask implements Runnable{
	
	RouterConnection newConnection;
	RouterConnection currentConnection;
	Router router;
	String[] extendInformation;
	Router.RoutingTableKey key;
	String circuitID;
	
	
	public OpenResponseTask(String[] extendInformation, Router router, RouterConnection currentConnection, 
								Router.RoutingTableKey key, String previousCircuitID) {
		System.out.println("Created new OpenResponseTask; making new TCP Connection");
		this.router = router;
		this.currentConnection = currentConnection;
		this.extendInformation = extendInformation;
		this.key = key;
		this.circuitID = circuitID;
	}

	@Override
	public void run() {
		
		System.out.println("STARTING CONNECTION WITH NEW NODE ---------------------------------------------");
		
		// GET OPEN CELL INFORMATION
		String openerAgentID = router.instanceNumber;
		String openedAgentID = extendInformation[2];
		
		// CREATE TCP CONNECTION
		try {
			System.out.println("Connection to IP: " + extendInformation[0] + ", Port:" + Integer.parseInt(extendInformation[1]));
			Socket socketToNextNode = new Socket(extendInformation[0], Integer.parseInt(extendInformation[1]));
			newConnection = new RouterConnection(socketToNextNode, router, false);	
		} catch (NumberFormatException | IOException e) {
			System.out.println("Failed to create a new connection.");
			// TODO: Send back RELAY_FAILED
			e.printStackTrace();
			byte[] relayFailed = CellFormatter.openFailedCell(openerAgentID, openedAgentID);
			currentConnection.send(relayFailed);
		}
		
		// SEND OPEN CELL / PUT "THIS" IN RESPONSE MAP
		byte[] openCell = CellFormatter.openCell(openerAgentID, openedAgentID);
		System.out.println("Running OpenResponseTask; waiting for OPENED cell");
		// Key is associated with the new connection's socket and the IDs that we are opening on
		System.out.println("Creating key associated with: " + openerAgentID + openedAgentID);
		key = router.new RoutingTableKey(newConnection.socket, openerAgentID + openedAgentID);
		
		router.requestResponseMap.put(key, this.response);
		newConnection.send(openCell);
		System.out.println("Sent the open cell.");
		
		
		// GET OPENED RESPONSE
		System.out.println("Waiting for response.");
		byte[] message = waitOnResponse();
		System.out.println("Received cell in buffer in OpenResponseTask");
		CellFormatter.CellType type = CellFormatter.determineType(message);
		if(router.checkTaskTypeReturn(type, CellFormatter.CellType.OPENED, CellFormatter.CellType.OPEN_FAILED)) {
			System.out.println("Succesfully opened connection with another node");
			// GET NEW CIRCUIT NUMBER
			String newCircuitID = newConnection.generateCircuitID() + "";
			
			// START THE CREATE RESPONSE TASK TO ACT ON SUCCESS
			// Key is associated with the RouterConnection in which we expect to hear the response
			// and the circuit it will be on
			// Task is given reference to "this", through which it will send the response
			System.out.println("Creating CreateResponseTask.");
			System.out.println("Putting the CreateResponseTask for address, port, circuitID: " + newConnection.socket.getInetAddress() + 
					", " + newConnection.socket.getPort() + ", " + newCircuitID);
			key = router.new RoutingTableKey(newConnection.socket, newCircuitID);
			
			System.out.println("Sending create cell.");
			
			
			// SEND THE CREATE MESSAGE USING THE ROUTER CONNECTION'S SEND BUFFER
			byte[] createCell = CellFormatter.createCell("" + newCircuitID);
			newConnection.send(createCell);
			CreateResponseTask task = new CreateResponseTask(router, currentConnection, circuitID, newConnection, newCircuitID);
			new Thread(task).start();
			router.requestResponseMap.put(key, task.response);
		} else {
			System.out.println("Response cell in OPEN RESPONSE TASK was not expected type.");
		}
		
		System.out.println("FINISHED CONNECTING WITH NEW NODE ---------------------------------------------");
		
	}
}
