package Tor61;

/*
 * Colin Miller, Nick Adman
 * 
 * The CellFormator is a helper class that provides static
 * methods to format cells for use on the Tor61 network.
 * 
 * TODO: CHANGE THE REPRESENTATION OF INTEGERS TO UNSIGNED 32-BIT NUMBERS!!!!!!!!!!!!!!!!
 */

public class CellFormator {
	
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
		message[0] = 0;
		message[1] = 0;
		openerAgentID = String.format("%04d", Integer.parseInt(openerAgentID));
		openedAgentID = String.format("%04d", Integer.parseInt(openedAgentID));
		String builder = "000" + openerAgentID + openedAgentID;
		for (int i = 0; i < builder.length(); i++) {
			message[i] = (byte) Integer.parseInt(builder.substring(i, i + 1));
		}
		message[2] = type;
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
		circuitID = String.format("%02d", Integer.parseInt(circuitID));
		for (int i = 0; i < circuitID.length(); i++) {
			message[i] = (byte) Integer.parseInt(circuitID.substring(i, i + 1));
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
	
	public static void main(String[] args) {
		byte[] message = openCell("211", "98765");
	}
}
