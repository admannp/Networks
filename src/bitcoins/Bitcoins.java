package bitcoins;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import javax.xml.bind.DatatypeConverter;

import org.bouncycastle.jce.provider.BouncyCastleProvider;


public class Bitcoins {
	
	private static MessageDigest md;

	/**
	 * @param args
	 * @throws IOException 
	 * @throws NoSuchAlgorithmException 
	 */
	public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
		
		Path path = Paths.get("transactionData-10000-3.bin");
		byte[] fileBytes = Files.readAllBytes(path);
		
		PrintStream pS = new PrintStream(new File("REDME_BETCH"));
		pS.write(fileBytes, 0, 82);
		
		Security.addProvider(new BouncyCastleProvider());
		

		
		
		// changed 	"new MessageDigest.getInstance("SHA-256")"
		// to 		"MessageDigest.getInstance("SHA-256")"
		// we are STUPIDDUMB 
		md = MessageDigest.getInstance("SHA-256");
		byte[][] testWords = { "a".getBytes(), "five".getBytes(), "word".getBytes(), "input".getBytes(), "example".getBytes() };
		merkleRootComputation(testWords);
		/*md.update("a".getBytes());
		byte[] hash = md.digest();
		System.out.println(bytesToHex(hash));
		System.out.println("Size of hash: " + hash.length);*/

		System.out.println("Library solution: " + DatatypeConverter.printHexBinary(hash("-----BEGIN RSA PUBLIC KEY-----\n" + 
				"MIGJAoGBAN3MxXHcbc1VNKTOgdm7W+i/dVnjv8vYGlbkdaTKzYgi8rQm126Sri87\n" + 
				"702UBNzmkkZyKbRKL/Bfc4EG8/Mt9Pd2xQlRyXCL9FnIFWHyhfIQtW+oBsGI5UhG\n" +
				"I8B8MiPOMfb6d/PdK+vd4riUxHAvCkHW5Lw0szAD1RVGbkG/7qnzAgMBAAE=\n" +
				"-----END RSA PUBLIC KEY-----")).toLowerCase());
		
	}
	
	private static byte[] hash(byte[] hashMe) {
		md.update(hashMe);
		md.update(md.digest());
		return md.digest();
	}
	
	private static byte[] hash(String hashMe) {
		return hash(hashMe.getBytes());
	}
	
	private static byte[] merkleRootComputation(byte[][] data) {
		// We use a queue to store the hashes as the tree is processed
		Queue<byte[]> hashes = new LinkedList<byte[]>();
		for (byte[] entry : data) {
			hashes.add(hash(entry));
			md.reset();
		}
		
		// If there are an odd number of entries, add the last one twice
		if (data.length % 2 == 1) {
			hashes.add(hash(data[data.length - 1]));
			md.reset();
		}
		
		// Our end condition is that there is just one item left in the queue.
		// When this is true, return that element.
		while(hashes.size() > 1) {
			// Take the front two entries, concatenate them, hash the result,
			// and then add it back into the queue.
			System.out.println("--- Beginning iteration ---");
			byte[] concatenation = null;
			System.out.println("Beginning size of queue: " + hashes.size());
			int currentSize = hashes.size();
			for (int i = 0; i < currentSize / 2; i++) {
				byte[] firstEntry = hashes.remove();
				System.out.println("Removed: " + DatatypeConverter.printHexBinary(firstEntry));
				byte[] secondEntry = hashes.remove();
				System.out.println("Removed: " + DatatypeConverter.printHexBinary(secondEntry));
				concatenation = new byte[firstEntry.length + secondEntry.length];
				System.arraycopy(firstEntry, 0, concatenation, 0, firstEntry.length);
				System.arraycopy(secondEntry, 0, concatenation, firstEntry.length, secondEntry.length);
				hashes.add(hash(concatenation));
				System.out.println("Added: " + DatatypeConverter.printHexBinary(hash(concatenation)));
				md.reset();
			}
			// If the size is odd, add in the last one again
			if (hashes.size() % 2 == 1 && hashes.size() != 1) {
				hashes.add(hash(concatenation));
			}
			System.out.println("Ending size of queue: " + hashes.size());
		}
		return hashes.remove();
	}

}
