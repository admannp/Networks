package registrationAgent;

import java.util.NoSuchElementException;
import java.util.Scanner;

import registrationProtocol.RegistrationClient;

public class RegistrationHandler {
	
	public static void main(String[] args) {
		 
		 if (args.length != 2) {
			 System.err.println("GIMME TWO ARGS, BETCH!!!1");
			 System.exit(1);
		 }
		 
		 RegistrationClient client = new RegistrationClient(args[0], args[1]);
		 Scanner in = new Scanner(System.in);
		 String input = "";
		 
		 while (in.hasNextLine()) {
		     
			 try {
				 input = in.nextLine();
				 Scanner lineScan = new Scanner(input);
				 String token = lineScan.next();
				 
			     if (token.equals("q")) {
			    	 break;
			     } else if (token.equals("r")) {
			    	 
			    	 String service = lineScan.next();
			    	 int port = lineScan.nextInt();
			    	 String data = lineScan.next();
			    	 
			    	 client.register(service, port, data);
			    	 
			     } else if (token.equals("u")) {
			    	 
			    	 String service = lineScan.next();
			    	 client.unregister(service);
			    	 
			     } else if (token.equals("f")) {
			    	 
			    	 if (lineScan.hasNext()) {
			    		 String prefix = lineScan.next();
			    		 client.fetch(prefix);
			    	 }
			    	 client.fetch();
			    	 
			     } else if (token.equals("p")) {
			    	 client.probe();
			     }
			     
		     } catch (NoSuchElementException e) {
	    		 System.err.println("Error reading input");
	    	 }
			 
		 }
		 
	}
	
}
