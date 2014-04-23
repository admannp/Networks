package registrationProtocol;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;


// Takes care of the details of implementing client side commands of the protocol
// Allows register, unregister, probe, and fetch commands.
// Also responds to probe requests from the service
public class RegistrationHandler {
	
	
	// Private inner class used to keep information about the registered clients
	// for use with registering and unregistering existing clients
	private class RegisteredService {
		private String serviceName;
		private String addr;
		private String portNum;
		private String data;
		private TimerTask timerTask;
		
		
		// Create new RegisteredService, which keeps track of service name, address, portnumber, and
		// data associated with a given service to allow for automatic reregistration and 
		// unregistering using just a service name
		RegisteredService(String serviceName, String address, String portNum, String data) {
			this.serviceName = serviceName;
			this.addr = address;
			this.portNum = portNum;
			this.data = data;
			timerTask = new TimerTask() {
				@Override public void run() {
					reregister();
				}
			};
		}
		
		// To allow caller to set timing for task
		TimerTask getTask() {
			return timerTask;
		}
		
		// Allows caller to reregister this service
		public void reregister() {
			register(serviceName, addr, portNum, data);
		}
	}
	
	// Put at the beginning of all messages
	public static final int MAGIC_WORD = 0xC461;
	
	// Maps service names to registered service objects for easy 
	// reregister and unregistering
	private HashMap<String, RegisteredService> services;
	private Timer timer;
	
	// Sockets used to interface with server
	private DatagramSocket sendSocket;
	private DatagramSocket receiveSocket;
	
	// Keeping track of the number of messages we've send
	// Used as a message's unique identifier
	private int sequenceNumber;
	
	// Create an instance of this protocol handler that interfaces with the given host 
	// at the given port
	public RegistrationHandler(String hostname, int portnum) {
		
		sequenceNumber = 0;
		
		timer = new Timer();
		services = new HashMap<String, RegisteredService>();
		
		InetAddress serverAddress = null;
		
		// Attempt to bind sockets to adjacent ports on client machine
		try {
			sendSocket = new DatagramSocket();
			sendSocket.setSoTimeout(3000);
			receiveSocket = new DatagramSocket(sendSocket.getLocalPort() + 1);
			// Get IP and port of server to connect to
			serverAddress = InetAddress.getByName(hostname);
		} catch (SocketException e) {
			System.out.println("Socket: " + e.getMessage());
		} catch (UnknownHostException e) {
			System.out.println("Unknown host: " + e.getMessage());
		}
		
		// Connect send socket to server address + port so as to not
		// redundantly supply these in the future
		sendSocket.connect(serverAddress, portnum);
		
		// Run the probe handler to handle incoming probes from the 
		// registration service
		(new Thread(new ProbeHandler(receiveSocket, serverAddress, portnum))).start();
		
		// Print useful information to user
		System.out.println("Registration service started.");
		System.out.println("Server address: 	" + serverAddress.toString());
		System.out.println("Local sending port: 	" + sendSocket.getLocalPort());
		System.out.println("Local receiving port: 	" + receiveSocket.getLocalPort());
	}
	
	// Helper method that increases sequence number and keeps track of wrapping accordingly
	private void increaseSequenceNumber() {
		sequenceNumber = (sequenceNumber + 1) % 256;
	}
	
	
	// Helper method that sends the given byte array of given message type, and waits for
	// given response number
	private byte[] send(byte[] message, int responseLength, String messageType, byte responseType) {
		
		DatagramPacket request = new DatagramPacket(message, message.length);
		
		byte[] responsePacket = new byte[responseLength];
		DatagramPacket response = new DatagramPacket(responsePacket, responsePacket.length);
		
		boolean hasMagicWord = false;
		
		try {
			
			// Wait for response
			// If given bad response, try again. Repeat up to three times.
			for (int i = 0; i < 3 && !hasMagicWord; i++) {
				sendSocket.send(request);
				try {
					sendSocket.receive(response);
				} catch (SocketTimeoutException e) { 
					System.err.println("Timed out waiting for reply to " + messageType + " message");
				}
				// -60 is 0xC4, 97 is 0x61 in signed representation
				hasMagicWord = responsePacket[0] == -60 && responsePacket[1] == 97 && 
						responsePacket[2] == (byte) (sequenceNumber & 0xff) && responsePacket[3] == responseType;
			}
			
			if (!hasMagicWord) {
				System.err.println("Sent 3 " + messageType + " messages and got no proper response.");
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
		
		// Increase the sequence number for subsequent messages
		increaseSequenceNumber();
		
		if (hasMagicWord)
			return responsePacket;
		else 
			return null;
	}
	
	// Fetch the list of registered service with optional prefix
	public String[][] fetch(String prefix) {
		
		ByteBuffer buffer = ByteBuffer.allocate(5 + prefix.length());
		
		buffer.putShort((short) (MAGIC_WORD & 0xffff));
		buffer.put((byte) (sequenceNumber & 0xff));
		// Command number
		buffer.put((byte) 3);
		
		// Put optional prefix
		buffer.put((byte) prefix.length());
		buffer.put(prefix.getBytes());
		
		byte[] response = send(buffer.array(), 1500, "FETCH", (byte) 4);
		
		String[][] result = null;
		
		if (response != null) {
			int numEntries = (int) response[4];
			
			result = new String[numEntries][3];
			for (int i = 0; i < numEntries; i++) {
				ByteBuffer entry = ByteBuffer.wrap(response, 5 + i * 10, 10);
				result[i][0] = (int) (entry.get() & 0xff) + "." 
						+ (int) (entry.get() & 0xff) + "." 
						+ (int) (entry.get() & 0xff) + "." 
						+ (int) (entry.get() & 0xff);
				result[i][1] = "" + (int) (entry.getShort() & 0xffff);
				result[i][2] = "" + (long) ((entry.getInt()) & 0x00000000ffffffffL);
			}
		}
		
		return result;
	}
	
	// Register the given service name with associated address and port number and data
	public int register(String serviceName, String address, String portNum, String serviceData) {
		int size = 15 + serviceName.length();
		ByteBuffer buffer = ByteBuffer.allocate(size);
		
		buffer.putShort((short) (MAGIC_WORD & 0xffff));
		buffer.put((byte) (sequenceNumber & 0xff));
		// Command number
		buffer.put((byte) 1);
		// address
		String[] addrBytes = address.split("\\.");
		buffer.put((byte) (Integer.parseInt(addrBytes[0]) & 0xff));
		buffer.put((byte) (Integer.parseInt(addrBytes[1]) & 0xff));
		buffer.put((byte) (Integer.parseInt(addrBytes[2]) & 0xff));
		buffer.put((byte) (Integer.parseInt(addrBytes[3]) & 0xff));
		// port
		int port = Integer.parseInt(portNum);
		buffer.putShort((short) (port & 0xffff));
		// data
		try {
			buffer.putInt((int) (Long.parseLong(serviceData) & 0xffffffff));
		} catch (NumberFormatException e) {
			return -1;
		}
		// length
		buffer.put((byte) serviceName.length());
		//data
		buffer.put(serviceName.getBytes());
		 
		byte[] registered = send(buffer.array(), 6, "REGISTER", (byte) 2);
		
		// return value for unsuccessful register attempt
		int lifetime = -1;
		
		if (registered != null) {
		
			lifetime = (int) ((int) (registered[4] & 0xff) * Math.pow(2,8) + (int) (registered[5] & 0xff));
			
			RegisteredService service = new RegisteredService(serviceName, address, portNum, serviceData);
			
			timer.schedule(service.getTask(), lifetime * 1000);
			
			services.put(serviceName, service);
		}
		
		return lifetime;
		
	}
	
	// Unregister the service with given name
	// If "remove" is true, remove the service from the services set too
	public boolean unregister(String serviceName, boolean remove) {
		int size = 5 + serviceName.length();
		ByteBuffer buffer = ByteBuffer.allocate(size);
		
		buffer.putShort((short) (MAGIC_WORD & 0xffff));
		buffer.put((byte) (sequenceNumber & 0xff));
		// Command number
		buffer.put((byte) 5);
		// length
		buffer.put((byte) serviceName.length());
		buffer.put(serviceName.getBytes());
		
		byte[] ret = send(buffer.array(), 4, "UNREGISTER", (byte) 7);
		boolean metSuccess = ret != null;
		if (metSuccess) {
			services.get(serviceName).getTask().cancel();
			if (remove)
				services.remove(serviceName);
		}
		
		return metSuccess;
	}

	// Probe service server
	public boolean probe() {
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.putShort((short) (MAGIC_WORD & 0xffff));
		buffer.put((byte) sequenceNumber);
		buffer.put((byte) 6);
		return send(buffer.array(), 4, "PROBE", (byte) 7) != null;
	}
	
	// Cleanup on user exit
	public void quit() {
		timer.cancel();
		for(String service : services.keySet()) {
			unregister(service, false);
		}
		services = null;
		sendSocket.close();
		receiveSocket.close();
	}

	
}
