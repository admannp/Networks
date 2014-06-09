package bitcoins;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
/*
 * Nick Adman & Colin Miller
 * This program parses out a file containing information about
 * CSE461-coin transactions, verifying which are legitimate and 
 * which are not.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.xml.bind.DatatypeConverter;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMException;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;


public class Bitcoins {
	
	// If DEBUG is true, prints out information as it computes
	private static final boolean DEBUG = false;
	
	private static MessageDigest md;
	private static HashMap<ByteArrayWrapper, byte[]> transactions;
	private static ByteArrayWrapper genesisTransactionKey;

	/**
	 * @param args
	 * @throws IOException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchProviderException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 */
	public static void main(String[] args) 
			throws IOException, NoSuchAlgorithmException, InvalidKeyException, 
			NoSuchProviderException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		
		Path path = Paths.get("transactionData-10000-3.bin");
		byte[] fileBytes = Files.readAllBytes(path);
		
		
		
		Security.addProvider(new BouncyCastleProvider());
		
		md = MessageDigest.getInstance("SHA-256");
		
		getTransactions(fileBytes);
		
		write(fileBytes);
		
	}
	
	private static void write(byte[] fileBytes) throws IOException {
		PrintStream pS = new PrintStream(new File("blockchain.bin"));
		//System.out.println(Arrays.toString(Arrays.copyOfRange(fileBytes, 0, 82)));
		pS.write(fileBytes, 0, 86);
		pS.write(transactions.get(genesisTransactionKey));
		//System.out.println("Genesis transaction: " + DatatypeConverter.printHexBinary(transactions.get(genesisTransactionKey)));
		transactions.remove(genesisTransactionKey);
		byte[] coinbaseTransaction = writeCoinbase();
		byte[][] transactionArray = new byte[transactions.size() + 1][];
		transactionArray[0] = coinbaseTransaction;
		int i = 1;
		for (ByteArrayWrapper key : transactions.keySet()) {
			transactionArray[i] = transactions.get(key);
			i++;
		}
		byte[][] test = new byte[1][];
		test[0] = coinbaseTransaction;
		byte[] header = writeHeader(fileBytes, transactionArray);
		pS.write(header);
		
		// TODO: comment out this for loop 
		for (byte[] transaction : transactionArray) {
			pS.write(transaction, 0, transaction.length);
		}
		
	}
	
	private static byte[] writeCoinbase() {
		ByteBuffer buffer = ByteBuffer.allocate(40);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.putShort((short) 0);
		buffer.putShort((short) 1);
		// TODO: get this value from a real place
		buffer.putInt(4922);
		String pubKey = "-----BEGIN RSA PUBLIC KEY-----\n" + 
				"MIGJAoGBAN3MxXHcbc1VNKTOgdm7W+i/dVnjv8vYGlbkdaTKzYgi8rQm126Sri87\n" +
				"702UBNzmkkZyKbRKL/Bfc4EG8/Mt9Pd2xQlRyXCL9FnIFWHyhfIQtW+oBsGI5UhG\n" +
				"I8B8MiPOMfb6d/PdK+vd4riUxHAvCkHW5Lw0szAD1RVGbkG/7qnzAgMBAAE=\n" +
				"-----END RSA PUBLIC KEY-----";
		buffer.put(hash(pubKey));
		return buffer.array();
	}
	
	private static byte[] writeHeader(byte[] fileBytes, byte[][] transactions) {
		ByteBuffer buffer = ByteBuffer.allocate(86);
		long nonce = Long.MIN_VALUE;
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		byte[] header = new byte[82];
		buffer.putInt(1);
		buffer.put(hash(Arrays.copyOfRange(fileBytes, 0, 82)));
		buffer.put(merkleRootComputation(transactions));
		buffer.putInt((int) (System.currentTimeMillis() / 1000L));
		buffer.putShort((short) 3);
		buffer.position(0);
		buffer.get(header, 0, header.length);
		do {
			nonce++; 
			buffer.position(0);
			buffer.put(header);
			buffer.position(74);
			buffer.putLong(nonce);
			buffer.position(0);
			buffer.get(header, 0, header.length);
		 } while(!checkNonce(hash(header), 3));

		buffer.position(0);
		buffer.put(header);
		buffer.putInt(transactions.length);
		//System.out.println("Transactions length: " + transactions.length);
		return buffer.array();
	}
	
	private static boolean checkNonce(byte[] header, int difficulty) {
		for (int i = 0; i < difficulty; i++) {
			if (header[i] != 0) 
				return false;
		}
		//System.out.println(Arrays.toString(header));
		return true;
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
		
		// If there is just one entry, return
		if (data.length == 1) {
			return hashes.remove();
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
			if (DEBUG){
				//System.out.println("--- Beginning iteration ---");
			}
			byte[] concatenation = null;
			if (DEBUG) {
				//System.out.println("Beginning size of queue: " + hashes.size());
			}
			int currentSize = hashes.size();
			for (int i = 0; i < currentSize / 2; i++) {
				byte[] firstEntry = hashes.remove();
				if (DEBUG) {
					//System.out.println("Removed: " + DatatypeConverter.printHexBinary(firstEntry));
				}
				byte[] secondEntry = hashes.remove();
				if (DEBUG) {
					//System.out.println("Removed: " + DatatypeConverter.printHexBinary(secondEntry));
				}
				concatenation = new byte[firstEntry.length + secondEntry.length];
				System.arraycopy(firstEntry, 0, concatenation, 0, firstEntry.length);
				System.arraycopy(secondEntry, 0, concatenation, firstEntry.length, secondEntry.length);
				hashes.add(hash(concatenation));
				if (DEBUG) {
					//System.out.println("Added: " + DatatypeConverter.printHexBinary(hash(concatenation)));
				}
				md.reset();
			}
			// If the size is odd, add in the last one again
			if (hashes.size() % 2 == 1 && hashes.size() != 1) {
				hashes.add(hash(concatenation));
			}
			if (DEBUG) {
				//System.out.println("Ending size of queue: " + hashes.size());
			}
		}
		return hashes.remove();
	}
	
	/**
	 * Given a byte[] representing the file of transactions, parses them out
	 * into an ArrayList<byte[]> containing all the transactions, including
	 * the genesis transaction.
	 * @param fileBytes, a byte[] of the file contents
	 * @throws IOException 
	 * @throws NoSuchProviderException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws PEMException 
	 * @throws InvalidKeyException 
	 */
	private static HashMap<ByteArrayWrapper, byte[]> getTransactions(byte[] fileBytes) 
			throws InvalidKeyException, PEMException, NoSuchAlgorithmException, 
			NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, 
			NoSuchProviderException, IOException {
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
		transactions = new HashMap<ByteArrayWrapper, byte[]>();
		
		// Add gensis transaction
		byte[] genesisHash = hash(Arrays.copyOfRange(fileBytes, 86, 126));
		genesisTransactionKey = new ByteArrayWrapper(genesisHash);
		transactions.put(genesisTransactionKey, Arrays.copyOfRange(fileBytes, 86, 126));
		 
		//System.out.println(Arrays.toString(genesisHash));
		
		Set<byte[]> usedOutputs = new HashSet<byte[]>();
		
		// How many transactions are we dealing with here?
		buffer.put(Arrays.copyOfRange(fileBytes, 126, 130));
		int totalTransactions = buffer.getInt(0);
		if (DEBUG) {
			//System.out.println("Number of transactions: " + totalTransactions);
		}
		
		int beginningPosition = 130;
		int currentPosition = 130;
		
		// Looping through given transactions
		for (int i = 0; i < totalTransactions; i++) {
			
			// Reset the buffer for another round of fun!
			buffer.position(0);
			// Let's get the number of inputs, first.
			buffer.put(Arrays.copyOfRange(fileBytes, currentPosition, currentPosition + 2));
			currentPosition += 2;
			short numInputs = buffer.getShort(0);
			if (DEBUG) {
				//System.out.println("This transaction has " + numInputs + " inputs.");
			}
			
			// Let's get the size of this transaction, first.
			int transactionEndPosition = currentPosition;
			for (short j = 0; j < numInputs; j++) {
				buffer.put(Arrays.copyOfRange(fileBytes, transactionEndPosition + 162, transactionEndPosition + 164));
				short length = buffer.getShort(buffer.position() - 2);
				transactionEndPosition += 164 + length;
			}
			buffer.put(Arrays.copyOfRange(fileBytes, transactionEndPosition, transactionEndPosition + 2));
			transactionEndPosition += 2;
			short sumOutputs = buffer.getShort(buffer.position() - 2);
			transactionEndPosition += (36 * sumOutputs);
			
			byte[] curTransaction = Arrays.copyOfRange(fileBytes, currentPosition - 2, transactionEndPosition);
			
			//System.out.println("Current position: " + currentPosition);
			//System.out.println("transactionEndPosition: " + transactionEndPosition);
			
			// sizePosition now contains the length of the transaction
			
			// Initializing the running input sum for this transaction
			// "value" used as flag
			// Initialize set of outputs used by this transaction
			int inputSum = 0;
			Set<byte[]> curOutputs = new HashSet<byte[]>();
			
			boolean isValid = true;
			
			// Looping through inputs
			for (short j = 0; j < numInputs; j++) {
				
				// Check that the given output has not been used before
				byte[] prevTxRefAndIndex = Arrays.copyOfRange(fileBytes, currentPosition, currentPosition + 34);
				if (usedOutputs.contains(prevTxRefAndIndex) || curOutputs.contains(prevTxRefAndIndex)) {
					isValid = false;
				}
				
				// We know there are 32 + 2 + 128 = 162 bytes of offset until the
				// short field containing the public key. Let's get that value.
				// Let's get the prevTxRef first.
				byte[] prevTxRef = Arrays.copyOfRange(fileBytes, currentPosition, currentPosition + 32);
				//System.out.println(Arrays.toString(prevTxRef));
				currentPosition += 32;
				// Next, we'll get the prevTxOutputIndex
				buffer.put(Arrays.copyOfRange(fileBytes, currentPosition, currentPosition + 2));
				currentPosition += 2;
				short prevTxOutputIndex = buffer.getShort(buffer.position() - 2);
				// Signature
				byte[] signature = Arrays.copyOfRange(fileBytes, currentPosition, currentPosition + 128);
				currentPosition += 128;
				buffer.put(Arrays.copyOfRange(fileBytes, currentPosition, currentPosition + 2));
				currentPosition += 2;
				short keyLength = buffer.getShort(buffer.position() - 2);
				// We now know the length of the key in this transaction.
				byte[] publicKey = Arrays.copyOfRange(fileBytes, currentPosition, currentPosition + keyLength);
				currentPosition += keyLength;
				//System.out.println("Current position: " + currentPosition);

				
				// Check the output for this input
				int value = checkOutputReference(prevTxRef, prevTxOutputIndex, publicKey, signature, curTransaction);
				if (value == -1) {
					isValid = false;
				}
				
				// Update running sum, add to the current outputs set
				inputSum += value;
				curOutputs.add(prevTxRefAndIndex);
				// Update
				
				if (DEBUG) {
					//System.out.println("Length of the key is: " + keyLength);
				}
			}
			
			// Now let's get the number of outputs
			buffer.put(Arrays.copyOfRange(fileBytes, currentPosition, currentPosition + 2));
			currentPosition += 2;
			short numOutputs = buffer.getShort(buffer.position() - 2);
			if (DEBUG) {
				//System.out.println("This transaction has " + numOutputs + " outputs.");
			}
			// Since the outputs are all 36 bytes long, we now know how far we
			// have to go to get the whole transaction.
			currentPosition += (36 * numOutputs);
			if (DEBUG) {
				//System.out.println("current position: " + currentPosition);
				//System.out.println("beginning position: " + beginningPosition);
				//System.out.println("This transaction was " + (currentPosition - beginningPosition) + " bytes long.");
			}
			// Get the entire transaction
			byte[] entireTransaction = Arrays.copyOfRange(fileBytes, beginningPosition, currentPosition);		
			byte[] keyInMap = hash(entireTransaction);
			beginningPosition = currentPosition;
			
			
			// Check value of parsing current input before adding to the map out valid transactions
			if (isValid) {
				transactions.put(new ByteArrayWrapper(keyInMap), entireTransaction);
				for (byte[] outputPair : curOutputs) 
					usedOutputs.add(outputPair);
			}
			// Reset the beginning position for the next iteration
			
		}
		return transactions;
	}
	
	private static int checkOutputReference(byte[] prevTxRef, short index, byte[] pubKey, byte[] signature, byte[] curTransaction) 
			throws InvalidKeyException, PEMException, NoSuchAlgorithmException, NoSuchPaddingException, 
			IllegalBlockSizeException, BadPaddingException, NoSuchProviderException, IOException {
		byte[] prevTx = transactions.get(new ByteArrayWrapper(prevTxRef));
		if (prevTx == null) {
			//System.out.println("NULL");
			return -1;
		}
		
		byte[] output = getOutputAtIndex(prevTx, index);
		if (output == null) {
			return -1;
		}
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.put(Arrays.copyOfRange(output, 0, 4));
		int value = buffer.getInt(buffer.position() - 4);
		
		if (!Arrays.equals(hash(pubKey), Arrays.copyOfRange(output, 4, 36))) {
			//System.out.println("HASHES NOT EQUAL");
			return -1;
		}
		
		if (!checkSignature(pubKey, curTransaction, signature)) {
			//System.out.println("SIGNATURE NOT CORRECT");
			return -1;
		}
		
		return value;
	}
	
	private static boolean checkSignature(byte[] pubKey, byte[] transaction, byte[] signature) throws InvalidKeyException, 
			PEMException, IOException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, 
			BadPaddingException, NoSuchProviderException {
		
		Reader fRd = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(pubKey)));
	    PEMParser parser = new PEMParser(fRd);
	    
	    
	    RSAPublicKey myKey = null;
	    Object o;
	    while ((o = parser.readObject()) != null) {
	         if (o instanceof SubjectPublicKeyInfo) {
	            JcaPEMKeyConverter myConverter = new JcaPEMKeyConverter();
	            myKey = (RSAPublicKey) myConverter.getPublicKey((SubjectPublicKeyInfo) o);
	            BigInteger exponent = myKey.getPublicExponent();
	            BigInteger modulus = myKey.getModulus();
	            //System.out.println("Exponent:");
	            //System.out.println(exponent);
	            //System.out.println("Modulus:");
	            //System.out.println(modulus);
	         } else {
	            //System.out.println("Not an instance of SubjectPublicKeyInfo.");
	         }
	     }
		
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
		cipher.init(Cipher.DECRYPT_MODE, myKey);
		
		byte[] decrypted;
		try {
			 decrypted = cipher.doFinal(signature);
		} catch (BadPaddingException e) {
			return false;
		}
		
		//System.out.println(Arrays.toString(decrypted));
		
		byte[] transactionWithoutSignatures = transactionWithoutSignatures(transaction);
		
		//System.out.println(Arrays.toString(hash(transactionWithoutSignatures)));
		
		return Arrays.equals(decrypted, hash(transactionWithoutSignatures));
		
	}
	
	// Given a transaction, returns a copy of the transaction without
	// any of the signature bytes included
	private static byte[] transactionWithoutSignatures(byte[] transaction) {
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		int currentPosition = 0;
		buffer.put(Arrays.copyOfRange(transaction, currentPosition, currentPosition + 2));
		short numInputs = buffer.getShort(buffer.position() - 2);
		byte[] modifiedTransaction = new byte[transaction.length - (numInputs * 128)];
		int modifiedPosition = 0;
		for (int i = 0; i < 2 ; i++) {
			modifiedTransaction[modifiedPosition] = transaction[currentPosition];
			modifiedPosition++;
			currentPosition++;
		}
		// Go through each input
		for (short i = 0; i < numInputs; i++) {
			// Copy the first 
			for(int j = 0; j < 34; j++) {
				modifiedTransaction[modifiedPosition] = transaction[currentPosition];
				modifiedPosition++;
				currentPosition++;
			}
			currentPosition += 128;
			buffer.put(Arrays.copyOfRange(transaction, currentPosition, currentPosition + 2));
			short length = buffer.getShort(buffer.position() - 2);
			for (int j = 0; j <  2 + length; j++) {
				modifiedTransaction[modifiedPosition] = transaction[currentPosition];
				modifiedPosition++;
				currentPosition++;
			}
		}
		int bytesToGo = (transaction.length - currentPosition);
		for (int i = 0; i < bytesToGo; i++) {
			modifiedTransaction[modifiedPosition] = transaction[currentPosition];
			modifiedPosition++;
			currentPosition++;
		}
		return modifiedTransaction;
	}
	
	// 
	
	// Returns the hashed public key of an output at an index, or null if that
	// index does not exist.
	private static byte[] getOutputAtIndex(byte[] transaction, short index) {
		//System.out.println("Getting at index " + index);
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		int currentPosition = 0;
		buffer.put(Arrays.copyOfRange(transaction, currentPosition, currentPosition + 2));
		short numInputs = buffer.getShort(buffer.position() - 2);
		currentPosition += 2;
		for (short i = 0; i < numInputs; i++) {
			currentPosition += 162;
			buffer.put(Arrays.copyOfRange(transaction, currentPosition, currentPosition + 2));
			currentPosition += 2;
			short keyLength = buffer.getShort(buffer.position() - 2);
			currentPosition += keyLength;
		}
		buffer.put(Arrays.copyOfRange(transaction, currentPosition, currentPosition + 2));
		currentPosition += 2;
		short numOutputs = buffer.getShort(buffer.position() - 2);
		if (index >= numOutputs) {
			return null;
		} else {
			return Arrays.copyOfRange(transaction, currentPosition + (index * 36), currentPosition + (index * 36) + 36);
		}
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
