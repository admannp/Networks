package bitcoins;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.xml.bind.DatatypeConverter;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;


public class Bitcoins {
	
	private static MessageDigest md;

	/**
	 * @param args
	 * @throws IOException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchProviderException 
	 */
	public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, NoSuchPaddingException {
		
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
		
		String pubKey = "-----BEGIN RSA PUBLIC KEY-----\n" + 
				"MIGJAoGBAN3MxXHcbc1VNKTOgdm7W+i/dVnjv8vYGlbkdaTKzYgi8rQm126Sri87\n" + 
				"702UBNzmkkZyKbRKL/Bfc4EG8/Mt9Pd2xQlRyXCL9FnIFWHyhfIQtW+oBsGI5UhG\n" +
				"I8B8MiPOMfb6d/PdK+vd4riUxHAvCkHW5Lw0szAD1RVGbkG/7qnzAgMBAAE=\n" +
				"-----END RSA PUBLIC KEY-----";

		System.out.println("Library solution: " + DatatypeConverter.printHexBinary(hash(pubKey)).toLowerCase());
		
		String privKey = "-----BEGIN RSA PRIVATE KEY-----\n" +
				"MIICXAIBAAKBgQDbtl/Yo5SIQ22rmufsfQSv76257VOh0HNZTtUUQqUXyvQ1oZM8\n" +
				"ycCAAQZtTRFqUzeA4Ur9OKnGzKUs6kg2DMc/f8d5YOs5oUVsKuU+6mtYVwzgen34\n" +
				"RacVdJNV+SdBQv2IlT4PvEoMFMuRvSuLfYjy1XnaD3XW2MtO8KQ65zx9YQIDAQAB\n" +
				"AoGAeBtAVftGTR8fKroponvNPig1vffgygpbpCyWCtdLzK/jxBWpmYdothDZZJLG\n" +
				"vGr1YnzGM5rwJH7mpKEGDJX7rNVufTrcRIjquR2GFvhogNLr/I49XT2fehvgwjD1\n" +
				"7IxaQYU43wFazCyW5iKrdeAlVQ0luKJjawWofBYmRSHRWUkCQQDufDjcWYFMzJam\n" +
				"8CbCk6ZyM6jxcUOGfpzomHrK9NrCo/aryQ8Wuf0ka6IHaEJkX7CwSbGiRBfGtEex\n" +
				"HDdz+AofAkEA69kz5z1rhSOhDONTpZEdnI6tYThpnD1EQnHqnffhjCYUTzj6OnNs\n" +
				"BcaDnRz89QKFOaXR2V1hxPaeEvCd2lGIfwJAPFGvEAyTZ5lXgWG8a/psXvYyBN9g\n" +
				"9OORTENEy5CixBg0i76O0nC4Vj3i/XyhTkHlrrD0/NW8LcXrXCCG5g4WgQJAUqig\n" +
				"WUYcfeAb3MF7moZ+o1UaDP3RfdG3L7ZvLQgog48BBTcJ9Bxp2qhVjmYPfetxN+AW\n" +
				"6SCiWH66rhaorFBxDwJBAMoGgm9cxUCYsl2FQugr+wL0Kx8ECI5727TEzCmuVFUx\n" +
				"X+kXCiTBgiO0WtTnd/uQmtqcLi3w19ko2her4Ctm8/k=\n" +
				"-----END RSA PRIVATE KEY-----\n";
		
		Reader fRd = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(pubKey.getBytes())));
	    PEMParser parser = new PEMParser(fRd);
	    
	    
	    RSAPublicKey myKey = null;
	    Object o;
	    while ((o = parser.readObject()) != null) {
	         if (o instanceof SubjectPublicKeyInfo) {
	            JcaPEMKeyConverter myConverter = new JcaPEMKeyConverter();
	            myKey = (RSAPublicKey) myConverter.getPublicKey((SubjectPublicKeyInfo) o);
	            BigInteger exponent = myKey.getPublicExponent();
	            BigInteger modulus = myKey.getModulus();
	            System.out.println("Exponent:");
	            System.out.println(exponent);
	            System.out.println("Modulus:");
	            System.out.println(modulus);
	         } else {
	            System.out.println("Not an instance of SubjectPublicKeyInfo.");
	         }
	      }
		
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.ENCRYPT_MODE, myKey);
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		CipherOutputStream cOut = new CipherOutputStream(bOut, cipher);

		cOut.write("plaintext".getBytes());
		cOut.close();
		
		System.out.println(bOut.toString());
		
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
