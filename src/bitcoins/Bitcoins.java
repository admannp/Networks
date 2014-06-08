package bitcoins;

/*
 * Nick Adman & Colin Miller
 * This program parses out a file containing information about
 * CSE461-coin transactions, verifying which are legitimate and 
 * which are not.
 */

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import javax.xml.bind.DatatypeConverter;

import org.bouncycastle.jce.provider.BouncyCastleProvider;


public class Bitcoins {
	
	// If DEBUG is true, prints out information as it computes
	private static final boolean DEBUG = true;
	
	private static MessageDigest md;

	/**
	 * @param args
	 * @throws IOException 
	 * @throws NoSuchAlgorithmException 
	 */
	public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
		
		Path path = Paths.get("transactionData-10000-3.bin");
		byte[] fileBytes = Files.readAllBytes(path);
		
		// TODO: Remember to change this name or face the wrath Nat
		PrintStream pS = new PrintStream(new File("REDME_BETCH"));
		pS.write(fileBytes, 0, 82);
		
		Security.addProvider(new BouncyCastleProvider());
		
		md = MessageDigest.getInstance("SHA-256");
		byte[][] testWords = { "a".getBytes(), "five".getBytes(), "word".getBytes(), "input".getBytes(), "example".getBytes() };
		merkleRootComputation(testWords);

		System.out.println("Library solution: " + DatatypeConverter.printHexBinary(hash("-----BEGIN RSA PUBLIC KEY-----\n" + 
				"MIGJAoGBAN3MxXHcbc1VNKTOgdm7W+i/dVnjv8vYGlbkdaTKzYgi8rQm126Sri87\n" + 
				"702UBNzmkkZyKbRKL/Bfc4EG8/Mt9Pd2xQlRyXCL9FnIFWHyhfIQtW+oBsGI5UhG\n" +
				"I8B8MiPOMfb6d/PdK+vd4riUxHAvCkHW5Lw0szAD1RVGbkG/7qnzAgMBAAE=\n" +
				"-----END RSA PUBLIC KEY-----")).toLowerCase());
		HashMap<byte[], byte[]> transactions = getTransactions(fileBytes);
		System.out.println("There are " + transactions.size() + " recorded transactions.");
		
	}
	
	/*
	 * Computes the dHash of byte[] given
	 */
	private static byte[] hash(byte[] hashMe) {
		md.update(hashMe);
		md.update(md.digest());
		return md.digest();
	}
	
	/*
	 * Computes the dHash of String given
	 */
	private static byte[] hash(String hashMe) {
		return hash(hashMe.getBytes());
	}
	
	/**
	 * Given a byte[][] of data blocks, returns the top hash using a merkle tree
	 * @param data, a byte[][] to be hashed
	 * @return byte[], the merkle hash
	 */
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
			if (DEBUG)
				System.out.println("--- Beginning iteration ---");
			byte[] concatenation = null;
			if (DEBUG)
				System.out.println("Beginning size of queue: " + hashes.size());
			int currentSize = hashes.size();
			for (int i = 0; i < currentSize / 2; i++) {
				byte[] firstEntry = hashes.remove();
				if (DEBUG)
					System.out.println("Removed: " + DatatypeConverter.printHexBinary(firstEntry));
				byte[] secondEntry = hashes.remove();
				if (DEBUG)
					System.out.println("Removed: " + DatatypeConverter.printHexBinary(secondEntry));
				concatenation = new byte[firstEntry.length + secondEntry.length];
				System.arraycopy(firstEntry, 0, concatenation, 0, firstEntry.length);
				System.arraycopy(secondEntry, 0, concatenation, firstEntry.length, secondEntry.length);
				hashes.add(hash(concatenation));
				if (DEBUG)
					System.out.println("Added: " + DatatypeConverter.printHexBinary(hash(concatenation)));
				md.reset();
			}
			// If the size is odd, add in the last one again
			if (hashes.size() % 2 == 1 && hashes.size() != 1) {
				hashes.add(hash(concatenation));
			}
			if (DEBUG)
				System.out.println("Ending size of queue: " + hashes.size());
		}
		return hashes.remove();
	}
	
	/**
	 * Given a byte[] representing the file of transactions, parses them out
	 * into an ArrayList<byte[]> containing all the transactions, including
	 * the genesis transaction.
	 * @param fileBytes, a byte[] of the file contents
	 */
	private static HashMap<byte[], byte[]> getTransactions(byte[] fileBytes) {
		/*
		 * First things first. We need to parse the genesis header out
		 * and grab relevent information, like the transaction associated.
		 * Since this is static, we'll use static numbers. For all the
		 * other transactions, however, we'll grab those based on
		 * how much space they take up.
		 */
		
		// Makes it easy to go from little -> big endian
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		// Store all of our transactions
		HashMap<byte[], byte[]> transactions = new HashMap<byte[], byte[]>();
		transactions.put(hash(Arrays.copyOfRange(fileBytes, 90, 126)), Arrays.copyOfRange(fileBytes, 90, 126));
		// How many transactions are we dealing with here?
		buffer.put(Arrays.copyOfRange(fileBytes, 126, 130));
		int totalTransactions = buffer.getInt(0);
		if (DEBUG)
			System.out.println("Number of transactions: " + totalTransactions);
		
		int beginningPosition = 130;
		int currentPosition = 130;
		for (int i = 0; i < totalTransactions; i++) {
			// Reset the buffer for another round of fun!
			buffer.position(0);
			// Let's get the number of inputs, first.
			buffer.put(Arrays.copyOfRange(fileBytes, currentPosition, currentPosition + 2));
			currentPosition += 2;
			short numInputs = buffer.getShort(0);
			if (DEBUG)
				System.out.println("This transaction has " + numInputs + " inputs.");
			// There are some number of inputs that are each variable length
			// depending on the public key
			for (short j = 0; j < numInputs; j++) {
				// We know there are 32 + 2 + 128 = 162 bytes of offset until the
				// short field containing the public key. Let's get that value.
				currentPosition += 162;
				buffer.put(Arrays.copyOfRange(fileBytes, currentPosition, currentPosition + 2));
				currentPosition += 2;
				short keyLength = buffer.getShort(buffer.position() - 2);
				// We now know the length of the key in this transaction.
				currentPosition += keyLength;
				if (DEBUG)
					System.out.println("Length of the key is: " + keyLength);
			}
			// Now let's get the number of outputs
			buffer.put(Arrays.copyOfRange(fileBytes, currentPosition, currentPosition + 2));
			currentPosition += 2;
			short numOutputs = buffer.getShort(buffer.position() - 2);
			if (DEBUG)
				System.out.println("This transaction has " + numOutputs + " outputs.");
			// Since the outputs are all 36 bytes long, we now know how far we
			// have to go to get the whole transaction.
			currentPosition += (36 * numOutputs);
			if (DEBUG)
				System.out.println("This transaction was " + (currentPosition - beginningPosition) + " bytes long.");
			byte[] entireTransaction = Arrays.copyOfRange(fileBytes, beginningPosition, currentPosition);
			byte[] keyInMap = hash(entireTransaction);
			transactions.put(keyInMap, entireTransaction);
			// Reset the beginning position for the next iteration
			beginningPosition = currentPosition;
		}
		return transactions;
	}
	
	// Made this by mistake. Not sure if we'll need it, but for now I'm keeping it.
	// Made it because I'm STUPIDDUMB!!!!!
	private static byte[][] parseBlockHeader(byte[] blockHeader) {
		byte[] versionNumber = new byte[4];
		for (int i = 0; i < 4; i++) {
			versionNumber[i] = blockHeader[i];
		}
		byte[] prevBlockRef = new byte[32];
		for (int i = 0; i < 32; i++) {
			prevBlockRef[i] = blockHeader[i + 4];
		}
		byte[] merkleRoot = new byte[32];
		for (int i = 0; i < 32; i++) {
			merkleRoot[i] = blockHeader[i + 36];
		}
		byte[] creationTime = new byte[4];
		for (int i = 0; i < 4; i++) {
			creationTime[i] = blockHeader[i + 68];
		}
		byte[] difficulty = new byte[2];
		for (int i = 0; i < 2; i++) {
			difficulty[i] = blockHeader[i + 72];
		}
		byte[] nonce = new byte[8];
		for (int i = 0; i < 8; i++) {
			nonce[i] = blockHeader[i + 74];
		}
		return new byte[][] {versionNumber, prevBlockRef, merkleRoot, creationTime, difficulty, nonce};
	}

}
