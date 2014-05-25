package Tor61;

/*
 * Colin Miller, Nick Adman
 * 
 * The CellFormator is a helper class that provides static
 * methods to format cells for use on the Tor61 network.
 */

public class CellFormatter {
	
	public enum CellType {
		OPEN,
		OPENED,
		OPEN_FAILED,
		CREATE,
		CREATED,
		CREATE_FAILED,
		DESTROY,
		RELAY_BEGIN,
		RELAY_DATA,
		RELAY_END,
		RELAY_CONNECTED,
		RELAY_EXTEND,
		RELAY_EXTENDED,
		RELAY_BEGIN_FAILED,
		RELAY_EXTEND_FAILED,
		UNKNOWN
	}
	
	/**
	 * The openCell method takes in two strings representing
	 * the opener and opened agent IDs and properly formats
	 * them into a Tor61 cell.
	 * 
	 * @param openerAgentID, a string representing the opener
	 * 		  agent ID
	 * @param openedAgentID, a string representing the opened
	 *        agent ID
	 * @return byte[], a 512 byte array that is encoded as an
	 *         open cell. Any unnecessary space in the array
	 *         is filled with zeroes
	 */
	public static byte[] openCell(String openerAgentID, String openedAgentID) {
		System.out.println("Creating open cell for openerAgentID, openedAgentID: " + 
							openerAgentID + ", " + openedAgentID);
		return openHelper(openerAgentID, openedAgentID, (byte) 0x05);
	}
	
	/**
	 * The openedCell method takes in two strings representing
	 * the opener and opened agent IDs and properly formats
	 * them into a Tor61 cell.
	 * 
	 * @param openerAgentID, a string representing the opener
	 * 		  agent ID
	 * @param openedAgentID, a string representing the opened
	 *        agent ID
	 * @return byte[], a 512 byte array that is encoded as an
	 *         open cell. Any unnecessary space in the array
	 *         is filled with zeroes
	 */
	public static byte[] openedCell(String openerAgentID, String openedAgentID) {
		System.out.println("Creating opened cell for openerAgentID, openedAgentID: " + 
							openerAgentID + ", " + openedAgentID);
		return openHelper(openerAgentID, openedAgentID, (byte) 0x06);
	}
	
	/**
	 * The openFailedCell method takes in two strings representing
	 * the opener and opened agent IDs and properly formats
	 * them into a Tor61 cell.
	 * 
	 * @param openerAgentID, a string representing the opener
	 * 		  agent ID
	 * @param openedAgentID, a string representing the opened
	 *        agent ID
	 * @return byte[], a 512 byte array that is encoded as an
	 *         open cell. Any unnecessary space in the array
	 *         is filled with zeroes
	 */
	public static byte[] openFailedCell(String openerAgentID, String openedAgentID) {
		System.out.println("Creating open failed cell for openerAgentID, openedAgentID: " + 
							openerAgentID + ", " + openedAgentID);
		return openHelper(openerAgentID, openedAgentID, (byte) 0x07);
	}
	
	/*
	 * Helper method to format open cells, since they are all
	 * the same, except the third byte.
	 */
	private static byte[] openHelper(String openerAgentID, String openedAgentID, byte type) {
		byte[] message = new byte[512];
		byte[] openerAgentBytes = intToByteArray(Integer.parseInt(openerAgentID));
		byte[] openedAgentBytes = intToByteArray(Integer.parseInt(openedAgentID));
		message[0] = 0;
		message[1] = 0;
		message[2] = type;
		// Puts the bytes from the i2b arrays into the message cell
		for (int i = 0; i < openerAgentBytes.length; i++) {
			message[i + openerAgentBytes.length - 1] = openerAgentBytes[i];
			message[i + openerAgentBytes.length + openedAgentBytes.length - 1] = openedAgentBytes[i];
		}
		return message;
	}
	
	/**
	 * The createCell method takes a string, the circuit ID,
	 * and returns a properly formatted Tor61 cell.
	 * 
	 * @param circuitID, a string representing the circuit ID
	 * @return byte[], a 512 byte array that is encoded as a
	 * 		   create cell. Any unnecessary space in the array
	 * 		   is filled with zeroes.
	 */
	public static byte[] createCell(String circuitID) {
		System.out.println("Creating create cell with circuitID: " + circuitID);
		return createHelper(circuitID, (byte) 0x01);
	}
	
	/**
	 * The createdCell method takes a string, the circuit ID,
	 * and returns a properly formatted Tor61 cell.
	 * 
	 * @param circuitID, a string representing the circuit ID
	 * @return byte[], a 512 byte array that is encoded as a
	 * 		   created cell. Any unnecessary space in the array
	 * 		   is filled with zeroes.
	 */
	public static byte[] createdCell(String circuitID) {
		System.out.println("Creating created cell with circuitID: " + circuitID);
		return createHelper(circuitID, (byte) 0x02);
	}
	
	/**
	 * The createFailedCell method takes a string, the circuit ID,
	 * and returns a properly formatted Tor61 cell.
	 * 
	 * @param circuitID, a string representing the circuit ID
	 * @return byte[], a 512 byte array that is encoded as a
	 * 		   create failed cell. Any unnecessary space in the array
	 * 		   is filled with zeroes.
	 */
	public static byte[] createFailedCell(String circuitID) {
		System.out.println("Creating created cell with circuitID: " + circuitID);
		return createHelper(circuitID, (byte) 0x08);
	}
	
	/*
	 * Helper method to format create cells, since they are all
	 * the same, except the third byte.
	 */
	private static byte[] createHelper(String circuitID, byte type) {
		byte[] message = new byte[512];
		byte[] circuitIDBytes = intToByteArray(Integer.parseInt(circuitID));
		for (int i = 2; i < circuitIDBytes.length; i++) {
			message[i - 2] = circuitIDBytes[i];
		}
		message[2] = type;
		return message;
	}
	
	/**
	 * The destroyCell method takes a string, the circuit ID,
	 * and returns a properly formatted Tor61 cell.
	 * 
	 * @param circuitID, a string representing the circuit ID
	 * @return byte[], a 512 byte array that is encoded as a
	 * 		   destroy cell. Any unnecessary space in the array
	 * 		   is filled with zeroes.
	 */
	public static byte[] destroyCell(String circuitID) {
		System.out.println("Creating destroy cell with circuitID: " + circuitID);
		return createHelper(circuitID, (byte) 0x04);
	}
	
	/**
	 * The relayBeginCell takes in a circuit ID, stream ID, a host identifier,
	 * and a port and returns a properly formatted Tor61 cell.
	 * 
	 * @param circuitID, a string representing the circuit ID
	 * @param streamID, a string representing the stream ID
	 * @param hostIdentifier, a string representing the host identifier
	 * @param port, a string representing the port
	 * @return byte[], a 512 byte array that is encoded as a relay begin
	 * 		   cell. Any unnecessary space in the array is filled with
	 * 		   zeroes.
	 */
	public static byte[] relayBeginCell(String circuitID, 
			String streamID, String hostIdentifier, String port) {
		byte[] message = relayShell(circuitID, streamID);
		// message [11-12] -> body length
		String body = hostIdentifier + ":" + port + '\0';
		byte[] bodyLengthBytes = intToByteArray(body.length());
		for (int i = 2; i < bodyLengthBytes.length; i++) {
			message[i + 9] = bodyLengthBytes[i];
		}
		// message [13] -> type of relay
		message[13] = (byte) 0x01;
		// message [14,] -> body
		byte[] bodyBytes = body.getBytes();
		for (int i = 0; i < bodyBytes.length; i++) {
			message [i + 14] = bodyBytes[i];
		}
		return message;
	}
	
	/**
	 * The relayDataCell takes in a circuit ID, stream ID, and some data, 
	 * returning properly formatted Tor61 cells.
	 * 
	 * @param circuitID, a string representing the circuit ID
	 * @param streamID, a string representing the stream ID
	 * @param data, a string representing the data to send in this cell
	 * @return byte[][], a variable length array where each index contains a 
	 * 		   512 byte array that is encoded as a relay data
	 * 		   cell. Any unnecessary space in a cell is filled with
	 * 		   zeroes.
	 */
	public static byte[][] relayDataCell(String circuitID, 
			String streamID, String data) {
		byte[][] cells = new byte[(int) Math.ceil(data.length() / 498.0)][512];
		int currentCell = 0;
		while (data.length() > 0) {
			byte[] message = relayShell(circuitID, streamID);
			// message [11-12] -> body length
			String body;
			if (data.length() > 498) {
				body = data.substring(0, 498);
				data = data.substring(498);
			} else {
				body = data;
				data = "";
			}
			byte[] bodyLengthBytes = intToByteArray(body.length());
			for (int i = 2; i < bodyLengthBytes.length; i++) {
				message[i + 9] = bodyLengthBytes[i];
			}
			// message [13] -> type of relay
			message[13] = (byte) 0x02;
			// message [14,] -> body
			byte[] bodyBytes = body.getBytes();
			for (int i = 0; i < bodyBytes.length; i++) {
				message [i + 14] = bodyBytes[i];
			}
			cells[currentCell++] = message;
		}
		return cells;
	}
	
	/**
	 * The relayEndCell takes in a circuit ID and stream ID 
	 * and returns a properly formatted Tor61 cell.
	 * 
	 * @param circuitID, a string representing the circuit ID
	 * @param streamID, a string representing the stream ID
	 * @return byte[], a 512 byte array that is encoded as a relay end
	 * 		   cell. Any unnecessary space in the array is filled with
	 * 		   zeroes.
	 */
	public static byte[] relayEndCell(String circuitID, String streamID) {
		byte[] message = relayShell(circuitID, streamID);
		// message [13] -> type of relay
		message[13] = (byte) 0x03;
		return message;
	}
	
	/**
	 * The relayConnectedCell takes in a circuit ID and stream ID 
	 * and returns a properly formatted Tor61 cell.
	 * 
	 * @param circuitID, a string representing the circuit ID
	 * @param streamID, a string representing the stream ID
	 * @return byte[], a 512 byte array that is encoded as a relay connected
	 * 		   cell. Any unnecessary space in the array is filled with
	 * 		   zeroes.
	 */
	public static byte[] relayConnectedCell(String circuitID, String streamID) {
		byte[] message = relayShell(circuitID, streamID);
		// message [13] -> type of relay
		message[13] = (byte) 0x04;
		return message;
	}
	
	/**
	 * The relayExtendCell takes in a circuit ID, stream ID, an IP,
	 * port, and agent id and returns a properly formatted Tor61 cell.
	 * 
	 * @param circuitID, a string representing the circuit ID
	 * @param streamID, a string representing the stream ID
	 * @param IP, a string representing the IP of the node to extend to
	 * @param port, a string representing the port of the node to extend to
	 * @param agentID, a string representing the agent ID of the node to
	 * 		  extend to
	 * @return byte[], a 512 byte array that is encoded as a relay extend
	 * 		   cell. Any unnecessary space in the array is filled with
	 * 		   zeroes.
	 */
	public static byte[] relayExtendCell(String circuitID, 
			String streamID, String IP, String port, String agentID) {
		byte[] message = relayShell(circuitID, streamID);
		// message [11-12] -> body length
		String body = IP + ":" + port + '\0' + agentID;
		byte[] bodyLengthBytes = intToByteArray(body.length());
		for (int i = 2; i < bodyLengthBytes.length; i++) {
			message[i + 9] = bodyLengthBytes[i];
		}
		// message [13] -> type of relay
		message[13] = (byte) 0x06;
		// message [14,] -> body
		byte[] bodyBytes = body.getBytes();
		for (int i = 0; i < bodyBytes.length; i++) {
			message [i + 14] = bodyBytes[i];
		}
		return message;
	}
	
	/**
	 * The relayExtendedCell takes in a circuit ID and stream ID 
	 * and returns a properly formatted Tor61 cell.
	 * 
	 * @param circuitID, a string representing the circuit ID
	 * @param streamID, a string representing the stream ID
	 * @return byte[], a 512 byte array that is encoded as a relay extended
	 * 		   cell. Any unnecessary space in the array is filled with
	 * 		   zeroes.
	 */
	public static byte[] relayExtendedCell(String circuitID, String streamID) {
		byte[] message = relayShell(circuitID, streamID);
		// message [13] -> type of relay
		message[13] = (byte) 0x07;
		return message;
	}
	
	/**
	 * The relayBeginFailedCell takes in a circuit ID and stream ID 
	 * and returns a properly formatted Tor61 cell.
	 * 
	 * @param circuitID, a string representing the circuit ID
	 * @param streamID, a string representing the stream ID
	 * @return byte[], a 512 byte array that is encoded as a relay begin
	 * 		   failed cell. Any unnecessary space in the array is filled with
	 * 		   zeroes.
	 */
	public static byte[] relayBeginFailedCell(String circuitID, String streamID) {
		byte[] message = relayShell(circuitID, streamID);
		// message [13] -> type of relay
		message[13] = (byte) 0x0b;
		return message;
	}
	
	/**
	 * The relayExtendFailedCell takes in a circuit ID and stream ID 
	 * and returns a properly formatted Tor61 cell.
	 * 
	 * @param circuitID, a string representing the circuit ID
	 * @param streamID, a string representing the stream ID
	 * @return byte[], a 512 byte array that is encoded as a relay extend
	 * 		   failed cell. Any unnecessary space in the array is filled with
	 * 		   zeroes.
	 */
	public static byte[] relayExtendFailedCell(String circuitID, String streamID) {
		byte[] message = relayShell(circuitID, streamID);
		// message [13] -> type of relay
		message[13] = (byte) 0x0c;
		return message;
	}
	
	/*
	 * Helper method that fills in the details that all relay
	 * cells share. However, it's the responsability of the
	 * caller to fill in the body length, relay cmd, and body
	 * fields in the cell.
	 */
	private static byte[] relayShell(String circuitID, String streamID) {
		byte[] message = new byte[512];
		// message[0-1] -> circuit ID
		byte[] circuitIDBytes = intToByteArray(Integer.parseInt(circuitID));
		for (int i = 2; i < circuitIDBytes.length; i++) {
			message[i - 2] = circuitIDBytes[i];
		}
		// magic relay number
		message[2] = (byte) 0x03;
		// message[3-4] -> stream id
		byte[] streamIDBytes = intToByteArray(Integer.parseInt(streamID));
		for (int i = 2; i < streamIDBytes.length; i++) {
			message[i + 1] = streamIDBytes[i];
		}
		// message [5-10] -> all zeroes. We don't use them for tor61.
		for (int i = 5; i <= 10; i++) {
			message[i] = (byte) 0x00;
		}
		return message;
	}
	
	/*
	 * Helper method to convert a byte array to an integer
	 */
	private static int byteArrayToInt(byte[] bytes) {
	    return   bytes[3] & 0xFF |
	            (bytes[2] & 0xFF) << 8 |
	            (bytes[1] & 0xFF) << 16 |
	            (bytes[0] & 0xFF) << 24;
	}
	
	/*
	 * Helper method to convert an int to a byte array
	 */
	private static byte[] intToByteArray(int num) {
	    return new byte[] {
	        (byte) ((num >> 24) & 0xFF),
	        (byte) ((num >> 16) & 0xFF),   
	        (byte) ((num >> 8) & 0xFF),   
	        (byte) (num & 0xFF)
	    };
	}
	
	/**
	 * Takes in a cell and returns a CellType enum displaying the type
	 * 
	 * @param cell, a byte[] 
	 * @return CellType, an enum describing the type of cell
	 */
	public static CellType determineType(byte[] cell) {
		if (cell.length != 512)
			return CellType.UNKNOWN;
		byte cellType = cell[2];
		switch(cellType) {
			case 0x05: return CellType.OPEN;
			case 0x06: return CellType.OPENED;
			case 0x07: return CellType.OPEN_FAILED;
			case 0x01: return CellType.CREATE;
			case 0x02: return CellType.CREATED;
			case 0x08: return CellType.CREATE_FAILED;
			case 0x04: return CellType.DESTROY;
			case 0x03:
				byte relayType = cell[13];
				switch(relayType) {
					case 0x01: return CellType.RELAY_BEGIN;
					case 0x02: return CellType.RELAY_DATA;
					case 0x03: return CellType.RELAY_END;
					case 0x04: return CellType.RELAY_CONNECTED;
					case 0x06: return CellType.RELAY_EXTEND;
					case 0x07: return CellType.RELAY_EXTENDED;
					case 0x0b: return CellType.RELAY_BEGIN_FAILED;
					case 0x0c: return CellType.RELAY_EXTEND_FAILED;
				}
			default: return CellType.UNKNOWN;
		}
	}
	
	/*public static void main(String[] args) {
		byte[][] message = relayDataCell("321", "654", "testing all o'er dis place. Mmm.");
		byte[] circID = new byte[] {0, 0, message[0][0], message[0][1]};
		byte[] streamID = new byte[] {0, 0, message[0][3], message[0][4]};
		byte[] bodyLength = new byte[] {0, 0, message[0][11], message[0][12]};
		
		int circuitIDnum = byteArrayToInt(circID);
		int streamIDnum = byteArrayToInt(streamID);
		int bodyLengthNum = byteArrayToInt(bodyLength);
		String body = "";
		for (int i = 0; i < bodyLengthNum; i++) {
			body += (char) message[0][i + 14];
		}
		System.out.println(body);
		CellType type = determineType(new byte[] {0, 0, 0});
	}*/
}