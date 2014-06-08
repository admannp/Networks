package bitcoins;

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
		
		md = MessageDigest.getInstance("SHA-256");
		byte[][] testWords = { "a".getBytes(), "five".getBytes(), "word".getBytes(), "input".getBytes(), "example".getBytes() };
		merkleRootComputation(testWords);

		System.out.println("Library solution: " + DatatypeConverter.printHexBinary(hash("-----BEGIN RSA PUBLIC KEY-----\n" + 
				"MIGJAoGBAN3MxXHcbc1VNKTOgdm7W+i/dVnjv8vYGlbkdaTKzYgi8rQm126Sri87\n" + 
				"702UBNzmkkZyKbRKL/Bfc4EG8/Mt9Pd2xQlRyXCL9FnIFWHyhfIQtW+oBsGI5UhG\n" +
				"I8B8MiPOMfb6d/PdK+vd4riUxHAvCkHW5Lw0szAD1RVGbkG/7qnzAgMBAAE=\n" +
				"-----END RSA PUBLIC KEY-----")).toLowerCase());
		
		getTransactions(fileBytes);
		
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
	
	private static void getTransactions(byte[] fileBytes) {
		byte[] genesisBlock = Arrays.copyOfRange(fileBytes, 0, 82);
		ByteBuffer nickIsCool = ByteBuffer.allocate(100);
		byte[] genesisTransactionCount = Arrays.copyOfRange(fileBytes, 82, 86);
		
		short counts = (short) byteArrayToInt(new byte[]{
				0, 0, genesisTransactionCount[0], genesisTransactionCount[1]
		});
		nickIsCool.putShort(counts);
		nickIsCool.position(0);
		nickIsCool.order(ByteOrder.LITTLE_ENDIAN);
		System.out.println("Genesis Block Transaction Count: " + nickIsCool.getShort());

		byte[] genesisTransactionInputs = Arrays.copyOfRange(fileBytes, 86, 88);
		short inputs = (short) little2big(byteArrayToInt(new byte[]{
				0, 0, genesisTransactionInputs[1], genesisTransactionInputs[0]
		}));
		nickIsCool.putShort(inputs);
		nickIsCool.position(nickIsCool.position() - 2);

		System.out.println("Genesis Transaction Inputs: " + nickIsCool.getShort());
		
		byte[] genesisTransactionOutputs = Arrays.copyOfRange(fileBytes, 88, 90);
		
		short outputs = (short) byteArrayToInt(new byte[]{
				0, 0, genesisTransactionOutputs[1], genesisTransactionOutputs[0]
		});
		nickIsCool.putShort(outputs);
		nickIsCool.position(nickIsCool.position() - 2);

		System.out.println("Genesis Transaction Outputs: " + nickIsCool.getShort());
		/*int outputs = byteArrayToInt(new byte[]{
				0, 0, genesisTransactionOutputs[0], genesisTransactionOutputs[1]
		});
		System.out.println("Genesis Block outputs: " + outputs);*/
		byte[] transactionCount = Arrays.copyOfRange(fileBytes, 126, 130);
		int bigTotalTransactionCount = little2big(byteArrayToInt(transactionCount));
		System.out.println("Number of transactions: " + bigTotalTransactionCount);
	}
	
	private static int byteArrayToInt(byte[] b) {
	    return   b[3] & 0xFF |
	            (b[2] & 0xFF) << 8 |
	            (b[1] & 0xFF) << 16 |
	            (b[0] & 0xFF) << 24;
	}
	
	private static int little2big(int i) {
	    return((i&0xff)<<24)+((i&0xff00)<<8)+((i&0xff0000)>>8)+((i>>24)&0xff);
	}
	
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
