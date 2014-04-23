import registrationAgent.Agent;


public class SomeService {
	public static void main(String[] args) {
		
		if (args.length != 2) {
			 System.err.println("GIMME TWO ARGS, BETCH!!!1");
			 System.exit(1);
		 }
		
		 String serviceAddress = args[0];
		 int servicePort = Integer.parseInt(args[1]);
		
		Agent ourApp = new Agent();
		ourApp.startAgent();
	}
}
