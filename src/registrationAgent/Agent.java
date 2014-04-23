package registrationAgent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Scanner;

import registrationProtocol.RegistrationHandler;

public class Agent {
	
	public static void main(String[] args) {
		
		if (args.length != 2) {
			usage();
		}
		
		String hostname = args[0];
		int port = Integer.parseInt(args[1]);
		
		System.out.println("Starting registration service at address: " + getAddress()); 
		RegistrationHandler protocol = new RegistrationHandler(hostname, port);
		
		System.out.print("Enter r(egister), u(nregister), f(etch), p(robe), h(elp), or q(uit): ");
		// Read input from the terminal
		Scanner console = new Scanner(System.in);
		
		// When new input occurs, use first token to determine the proper call to use
		// as given in protocol specification
		while(console.hasNextLine()) {
			
			String input = console.nextLine();
			input = input.trim();
			checkCommandFormat(input);
			 
			// quit on 'q'
			if (input.contains("q")) {
				protocol.quit();
				break;
			}
			 
			Scanner inputScan = new Scanner(input);
			String prefix = inputScan.next();
			String serviceName = "";
			
			switch(prefix) {
				case "r": 
					serviceName = inputScan.next();
					String portNum = inputScan.next();
					String message = "";
					if (inputScan.hasNext())
						message = inputScan.next();
					String addr = getAddress();
					int lifetime = protocol.register(serviceName, addr, portNum, message);
					if (lifetime != -1) {
						System.out.println("Registration for " + addr + ":" + portNum + " successful. Lifetime = " + lifetime);
					} else {
						System.err.println("Registration failed.");
					}
					break;
				case "u":
					serviceName = inputScan.next();
					System.out.println("Unregistering " + serviceName);
					protocol.unregister(serviceName, true);
					break;
				case "f":
					String namePrefix = "";
					if (inputScan.hasNext())
						namePrefix = inputScan.next();
					String[][] response = protocol.fetch(namePrefix);
					if (response != null) {
						for (int i = 0; i < response.length; i++) {
							System.out.print("Content of response[" + i + "]: ");
							System.out.print(response[i][0] + " ");
							System.out.print(response[i][1] + " ");
							System.out.println(response[i][2]);
						}
					} else {
						System.out.println("Fetch failed");
					}
					break;
				case "p":
					if (protocol.probe())
						System.out.println("Success");
					else 
						System.out.println("Probe failed");
					break;
			}
			
			inputScan.close();
			System.out.print("Enter r(egister), u(nregister), f(etch), p(robe), h(elp), or q(uit): ");
			 
		 }
		console.close();
	}
	
	private static void usage() {
		System.err.println("Please supply a hostname and port");
		System.exit(1);
	}
	
	private static void checkCommandFormat(String input) {
		if (!(input.startsWith("r ") || input.startsWith("u ") || 
				input.startsWith("f ") || input.equals("p") || 
				input.equals("q") || input.equals("f"))) {
			System.out.println("Unable to understand command. Possible commands: ");
			System.out.println("Register: 	r serviceName portnum data");
			System.out.println("Unregister: u serviceName ");
			System.out.println("Fetch:		f <name prefix> ");
			System.out.println("Probe: 		p");
			System.out.println("Quit: 		q");
			
		}
	}
	
	// Get the useful address of the protocol
	// Includes a check to make sure this is not a LAN specific address
	protected static String getAddress() {
		String localAddress = "";
		try {
			localAddress = Inet4Address.getLocalHost().getHostAddress();
		
			// Check to make sure we aren't using LAN specific IP
			// If so, query JZ's web site
			if (localAddress.startsWith("192.168") || localAddress.contains("127.0.0.1")) {
				URL url = new URL("http://abstract.cs.washington.edu/~zahorjan/ip.cgi");
			    InputStream is = url.openConnection().getInputStream();
			    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			    reader.readLine();
			    localAddress = reader.readLine();
			    localAddress = localAddress.substring(0, localAddress.indexOf(" "));
			    reader.close();
			}
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return localAddress;
	}
	
}
