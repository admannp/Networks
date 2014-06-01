package Tor61;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class ResponseTask {
	public Queue<byte[]> response;
	
	public ResponseTask() {
		System.out.println("Creating ResponseTask.");
		this.response = new LinkedBlockingQueue<byte[]>();
	}
	
	public byte[] waitOnResponse() {
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
				return cell; 
			}
		}
	}
	
}
