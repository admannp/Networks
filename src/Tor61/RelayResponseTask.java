package Tor61;

import java.util.Queue;

public class RelayResponseTask extends ResponseTask implements Runnable {
	Router router;
	
	public RelayResponseTask(Router router) {
		super();
		this.router = router;
	}

	public void run() {
		
	}
}
