package TCPTest;

import java.io.*;
import java.net.*;

class TCPClient {
	public static void main(String argv[]) throws Exception {
		
		String sentence;
		String modifiedSentence;
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(
				System.in));
		Socket clientSocket = new Socket("128.208.1.221", 6789);
		DataOutputStream outToServer = new DataOutputStream(
				clientSocket.getOutputStream());
		BufferedReader inFromServer = new BufferedReader(new InputStreamReader(
				clientSocket.getInputStream()));
		while(true) {
			sentence = inFromUser.readLine();
			if (sentence.equals("close")) {
				clientSocket.close();
				System.exit(0);
			}
			System.out.println("Now writing");
			outToServer.writeBytes(sentence + '\n');
			System.out.println("Now reading");
			modifiedSentence = inFromServer.readLine();
			System.out.println("FROM SERVER: " + modifiedSentence);
		}
		//clientSocket.close();
	}
}