package Tor61;

import java.util.Queue;

public class CreateResponseTask extends ResponseTask implements Runnable {
	Router router;
	RouterConnection returnConnection;
	
	public CreateResponseTask(Router router, RouterConnection returnConnection) {
		super();
		this.router = router;
		this.returnConnection = returnConnection;
	}

	public void run() {
		byte[] cell = waitOnResponse();
		CellFormatter.CellType type = CellFormatter.determineType(cell);
		// If the type we've received is what we expect
		if(router.checkTaskTypeReturn(type, CellFormatter.CellType.CREATED, CellFormatter.CellType.CREATE_FAILED)) {
			byte[] message = CellFormatter.createdCell(CellFormatter.getCircuitIDFromCell(cell));
			returnConnection.send(null);
			
			/********************
			 * TO DOOOOOOOOOOOOO
			 * 
			 * In our Routing table put 
			 * 		(node, circuitNum) => null
			 * 
			 * 
			 */
			
		} else {
			System.out.println("Response cell type was not expected type.");
		}
		
	}
}
