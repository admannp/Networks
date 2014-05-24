package Tor61;

/*
 * Colin Miller, Nick Adman
 * 
 * The CellFormator is a helper class that provides static
 * methods to format cells for use on the Tor61 network.
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
		System.out.println("Creating open cell for openerAgentID, openedAgentID: " + 
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
		System.out.println("Creating open cell for openerAgentID, openedAgentID: " + 
							openerAgentID + ", " + openedAgentID);
		return openHelper(openerAgentID, openedAgentID, (byte) 0x07);
	}
	
	/*
	 * Helper method to format open cells, since they are all
	 * the same, except the third bit.
	 */
	private static byte[] openHelper(String openerAgentID, String openedAgentID, byte type) {
		byte[] message = new byte[512];
		message[0] = 0;
		message[1] = 0;
		String builder = "000" + openerAgentID + openedAgentID;
		for (int i = 0; i < builder.length(); i++) {
			message[i] = (byte) Integer.parseInt(builder.substring(i, i + 1));
		}
		message[2] = type;
		return message;
	}
}
