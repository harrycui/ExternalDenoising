package test;

import java.math.BigInteger;
import java.util.Random;

import index.IndexValue;
import util.BaseTool;
import util.MyAES;
import util.PRF;

public class CorrectnessTest {
	
	

	public static void main(String args[]) {

		Random rand = new Random();

		long lshValue = 123456789L;

		String keyR = "harry";

		for (int i = 0; i < 250000; ++i) {

			int pid = i;

			BigInteger k2 = PRF.HMACSHA1ToBigInteger("2xx" + lshValue + "xx" + 1, keyR);
			
			try {
				IndexValue indexValue = encryptIndexValue(k2, (long) pid, rand);

				int reId;

				reId = (int)decryptIndexValue(k2, indexValue, rand);

				if (pid != reId) {

					System.out.println(pid + " ?= " + reId);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		System.out.println("done.");
	}

	private static IndexValue encryptIndexValue(BigInteger k2, long id, Random rand) {

		IndexValue indexValue = null;

		// convert key
		byte[] keyBytes = BaseTool.bigIntegerTo128Bits(k2);

		// random gen IV
		byte[] iv = BaseTool.longTo128Bits(rand.nextLong());

		// data
		byte[] data = BaseTool.longTo128Bits(id);

		try {
			byte[] cipher = MyAES.aesEnc(keyBytes, data, iv);

			indexValue = new IndexValue(iv, cipher);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return indexValue;
	}

	private static long decryptIndexValue(BigInteger k2, IndexValue indexValue, Random rand) {

		long id = -1;

		// convert key
		byte[] keyBytes = BaseTool.bigIntegerTo128Bits(k2);

		byte[] cipher = indexValue.getCipher();
		
		byte[] iv = indexValue.getIv();
		//byte[] iv = BaseTool.longTo128Bytes(rand.nextLong());

		try {
			byte[] data = MyAES.aesDec(keyBytes, cipher, iv);

			id = BaseTool.bytesToLong(data);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return id;
	}
}
