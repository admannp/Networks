package bitcoins;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;


public class Bitcoins {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		Path path = Paths.get("bitcoins/transactionData-10000-3.bin");
		byte[] fileBytes = Files.readAllBytes(path);
		
		PrintStream pS = new PrintStream(new File("bitcoins/REDME_BETCH"));
		pS.write(fileBytes, 0, 82);
		
		Security.addProvider(new BouncyCastleProvider());
		
		MessageDigest mD = new MessageDigest.getInstance("SHA-256");

	}

}
