package index;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import base.CashDigest;
import base.LSHVector;
import base.MyTreemapComparator;
import util.BaseTool;
import util.MyAES;
import util.PRF;

/**
 * This version remove the encryption module for the "value" part. This can save the time for the performance test.
 * 
 * Created by HarryC on 3/10/15.
 * Modified by HarryC on 20-Dec-2015
 */
public class SecureCashIndex2 {

	private int L;
	
	private Random rand;

	public ConcurrentHashMap<BigInteger, Integer> staticIndex;

	public ConcurrentHashMap<BigInteger, Integer> dynamicIndex;

	public HashSet<BigInteger> revidSet;

	private ConcurrentHashMap<BigInteger, Long> maxC; // used to record the maximum c for each lsh tag;

	public ConcurrentHashMap<Integer, String> idMap;


	public SecureCashIndex2(int limit, int L) {

		this.L = L;
		this.rand = new Random(L);
		
		this.maxC = new ConcurrentHashMap<BigInteger, Long>();

		this.idMap = new ConcurrentHashMap<Integer, String>(limit);

		this.staticIndex = new ConcurrentHashMap<BigInteger, Integer>(limit);
		this.dynamicIndex = new ConcurrentHashMap<BigInteger, Integer>(limit);
		this.revidSet = new HashSet<BigInteger>();
	}

	public void insert(LSHVector lshVector, String imageId, int fid, String keyV, String keyR) {

		for (int i = 0; i < lshVector.getDimension(); ++i) {

			Long lshValue = lshVector.getLSHValueByIndex(i);

			long c = 0; // start from 0

			// TODO: double check the connection method
			BigInteger k1 = PRF.HMACSHA1ToBigInteger("1xx" + lshValue + "xx" + i, keyV);
			//BigInteger k2 = PRF.HMACSHA1ToBigInteger("2xx" + lshValue + "xx" + i, keyR);

			if (maxC.containsKey(k1)) {

				c = maxC.get(k1) + 1;
				maxC.put(k1, c);
			} else {
				maxC.put(k1, 1L);
			}

			boolean successInsert = false;

			while (!successInsert) {

				BigInteger a = genIndexKey(k1, c);

				//IndexValue b = encryptIndexValue(k2, (long)fid);

				int b = fid;

				// if does not exist, directly insert
				if (!staticIndex.containsKey(a)) {
					// System.out.println(a);
					staticIndex.put(a, b);

					idMap.put(fid, imageId);

					successInsert = true;
					maxC.put(k1, c);
				}

				++c;
			}
		}
	}

	public boolean dynamicAdd(BigInteger key, Integer value) {

		if (dynamicIndex.containsKey(key)) {

			dynamicIndex.put(key, value);

			return false;
		} else {

			dynamicIndex.put(key, value);

			return true;
		}
	}

	public void dynamicDel(BigInteger revid) {

		revidSet.add(revid);
	}

	public TreeMap<Integer, List<Integer>> searchByOnePatch(LSHVector lshVector, String keyV, String keyR, boolean isShowTime) {

		// Step 1: generate digests on client
		long timeFlag1 = System.nanoTime();

		List<CashDigest> digestInL = new ArrayList<CashDigest>(L);

		for (int i = 0; i < lshVector.getDimension(); ++i) {

			digestInL.add(new CashDigest(lshVector.getLSHValueByIndex(i), i, keyV, keyR));
		}

		if (isShowTime) {
			System.out.println("Client-side cost: " + (System.nanoTime() - timeFlag1) + " ns.");
		}
		
		// Step 2: search on server side
		long timeFlag2 = System.nanoTime();

		//// <patch id, number of occurrence>
		HashMap<Integer, Integer> tempResult = new HashMap<Integer, Integer>();

		for (CashDigest cd : digestInL) {

			BigInteger k1 = cd.getK1();
			
			//BigInteger k2 = cd.getK2();

			long c = 0; // start from 0

			while (true) {

				BigInteger a = genIndexKey(k1, c);
				
				// if does not exist, directly insert
				if (staticIndex.containsKey(a)) {

					//IndexValue indexValue = staticIndex.get(a);
					
					//int pid = (int)decryptIndexValue(k2, indexValue);
					
					int pid = staticIndex.get(a);

					long revid = (int) (PRF.HMACSHA1ToUnsignedInt(String.valueOf(pid), k1.toString()));

					if (!revidSet.contains(revid)) {

						synchronized (this) {

							// CashIndex.featureRankArray[fid] =
							// CashIndex.featureRankArray[fid] + 1;
							if (tempResult.containsKey(pid)) {
								tempResult.put(pid, tempResult.get(pid) + 1);
							} else {
								tempResult.put(pid, 1);
							}
						}
					}
				} else {
					break;
				}

				++c;
			}

			// deal with the added table
			c = 0; // start from 0

			while (true) {

				BigInteger a = genIndexKey(k1, c);
				
				// if does not exist, directly insert
				if (dynamicIndex.containsKey(a)) {

					//IndexValue indexValue = staticIndex.get(a);
					
					//int pid = (int)decryptIndexValue(k2, indexValue);
					int pid = staticIndex.get(a);

					long revid = (int) (PRF.HMACSHA1ToUnsignedInt(String.valueOf(pid), k1.toString()));
					if (!revidSet.contains(revid)) {

						synchronized (this) {

							// CashIndex.featureRankArray[fid] =
							// CashIndex.featureRankArray[fid] + 1;
							// CashIndex.featureRankArray[fid] =
							// CashIndex.featureRankArray[fid] + 1;
							if (tempResult.containsKey(pid)) {
								tempResult.put(pid, tempResult.get(pid) + 1);
							} else {
								tempResult.put(pid, 1);
							}
						}
					}
				} else {
					break;
				}

				++c;
			}
		}
		
		// key - lsh occurrence, value - pid list
		TreeMap<Integer, List<Integer>> result = new TreeMap<Integer, List<Integer>>(new MyTreemapComparator());
		
		Iterator<Map.Entry<Integer, Integer>> entries = tempResult.entrySet().iterator();

		while (entries.hasNext()) {

		    Map.Entry<Integer, Integer> entry = entries.next();
		    
		    if (result.containsKey(entry.getValue())) {
				
		    	result.get(entry.getValue()).add(entry.getKey());
			} else {

				List<Integer> patchList = new ArrayList<Integer>();
				
				patchList.add(entry.getKey());

				result.put(entry.getValue(), patchList);
			}
		}
		
		if (isShowTime) {
			System.out.println("Server-side cost: " + (System.nanoTime() - timeFlag2) + " ns.");
		}

		return result;
	}

	/*
	 * public HashMap<Integer, Integer> searchByOnePatch(LSHVector lshVector,
	 * String keyV, String keyR) {
	 * 
	 * 
	 * // Step 1: generate digests on client long timeFlag1 =
	 * System.currentTimeMillis();
	 * 
	 * List<CashDigest> digestInL = new ArrayList<CashDigest>(L);
	 * 
	 * for (int i = 0; i < lshVector.getDimension(); ++i) {
	 * 
	 * digestInL.add(new CashDigest(lshVector.getLSHValueByIndex(i), i, keyV,
	 * keyR)); }
	 * 
	 * System.out.println("Client side digest generate cost: " +
	 * (System.currentTimeMillis() - timeFlag1) + "ms.");
	 * 
	 * // Step 2: search on server side long timeFlag2 =
	 * System.currentTimeMillis();
	 * 
	 * // <patch id, number of occurrence> HashMap<Integer, Integer> result =
	 * new HashMap<Integer, Integer>();
	 * 
	 * // <pid, counter> tempPatchResult = new ConcurrentHashMap<Integer,
	 * Integer>();
	 * 
	 * // long timeFlag0 = System.currentTimeMillis();
	 * 
	 * MyCountDown threadCounter = new MyCountDown(L);
	 * 
	 * for (int i = 0; i < L; i++) {
	 * 
	 * OnePatchQueryThread t = new OnePatchQueryThread("Thread " + i,
	 * threadCounter, i, digestInL, tempPatchResult, staticIndex, dynamicIndex,
	 * revidSet);
	 * 
	 * t.start(); }
	 * 
	 * // wait for all threads done while (true) { if (!threadCounter.hasNext())
	 * break; }
	 * 
	 * result.putAll(tempPatchResult);
	 * 
	 * tempPatchResult.clear(); tempPatchResult = null;
	 * 
	 * System.out.println("Query patch at server cost: " +
	 * (System.currentTimeMillis() - timeFlag2) + "ms");
	 * 
	 * return result; }
	 */

	public List<Integer> rankFeature(int threshold, HashMap<Integer, Integer> featureRank) {

		List<Integer> result = new ArrayList<Integer>();

		List<Map.Entry<Integer, Integer>> list = new ArrayList<Map.Entry<Integer, Integer>>();

		// æŠŠmapè½¬åŒ–ä¸ºMap.Entryç„¶å�Žæ”¾åˆ°ç”¨äºŽæŽ’åº�çš„listé‡Œé�¢
		list.addAll(featureRank.entrySet());

		/*
		 * for (Iterator<Map.Entry<Integer, Integer>> it =
		 * featureRank.entrySet().iterator(); it.hasNext(); ) {
		 * 
		 * Map.Entry<Integer, Integer> tmp = it.next();
		 * 
		 * if (tmp.getValue() >= threshold) {
		 * 
		 * list.add(tmp); } }
		 */

		// è°ƒç”¨å†…éƒ¨ç±»çš„æž„é€ å™¨ï¼Œå¦‚æžœè¿™ä¸ªå†…éƒ¨ç±»æ˜¯é�™æ€�å†…éƒ¨ç±»ï¼Œå°±æ¯”è¿™ä¸ªå¥½åŠžç‚¹äº†ã€‚ã€‚
		SecureCashIndex2.IntegerValueComparator mc = new IntegerValueComparator();
		// å¼€å§‹æŽ’åº�ï¼Œä¼ å…¥æ¯”è¾ƒå™¨å¯¹è±¡
		Collections.sort(list, mc);

		// é��åŽ†åœ¨listä¸­æŽ’åº�ä¹‹å�Žçš„HashMap
		for (Iterator<Map.Entry<Integer, Integer>> it = list.iterator(); it.hasNext();) {

			// Map.Entry<Integer, Integer> tmp = it.next();

			// if (tmp.getValue() >= threshold) {

			// System.out.println("fid = " + tmp.getKey() + ", iid = " +
			// idMap.get(tmp.getKey()) + ", counter = " + tmp.getValue());
			// System.out.println(tmp.getKey());
			result.add(it.next().getKey());
			// }
		}

		return result;
	}

	public static List<String> topK(int topK, HashMap<String, Integer> cc) {

		List<String> result = new ArrayList<String>();

		List<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>();
		// æŠŠmapè½¬åŒ–ä¸ºMap.Entryç„¶å�Žæ”¾åˆ°ç”¨äºŽæŽ’åº�çš„listé‡Œé�¢
		list.addAll(cc.entrySet());
		// è°ƒç”¨å†…éƒ¨ç±»çš„æž„é€ å™¨ï¼Œå¦‚æžœè¿™ä¸ªå†…éƒ¨ç±»æ˜¯é�™æ€�å†…éƒ¨ç±»ï¼Œå°±æ¯”è¿™ä¸ªå¥½åŠžç‚¹äº†ã€‚ã€‚
		SecureCashIndex2.StringValueComparator mc = new StringValueComparator();
		// å¼€å§‹æŽ’åº�ï¼Œä¼ å…¥æ¯”è¾ƒå™¨å¯¹è±¡
		Collections.sort(list, mc);

		// é��åŽ†åœ¨listä¸­æŽ’åº�ä¹‹å�Žçš„HashMap
		for (Iterator<Map.Entry<String, Integer>> it = list.iterator(); it.hasNext();) {

			result.add(it.next().getKey());

			if (--topK <= 0) {
				break;
			}
		}

		return result;
	}

	public static List<Integer> topKPatches(int topK, HashMap<Integer, Integer> cc) {

		List<Integer> result = new ArrayList<Integer>();

		List<Map.Entry<Integer, Integer>> list = new ArrayList<Map.Entry<Integer, Integer>>();
		// æŠŠmapè½¬åŒ–ä¸ºMap.Entryç„¶å�Žæ”¾åˆ°ç”¨äºŽæŽ’åº�çš„listé‡Œé�¢
		list.addAll(cc.entrySet());
		// è°ƒç”¨å†…éƒ¨ç±»çš„æž„é€ å™¨ï¼Œå¦‚æžœè¿™ä¸ªå†…éƒ¨ç±»æ˜¯é�™æ€�å†…éƒ¨ç±»ï¼Œå°±æ¯”è¿™ä¸ªå¥½åŠžç‚¹äº†ã€‚ã€‚
		SecureCashIndex2.IntegerValueComparator mc = new IntegerValueComparator();
		// å¼€å§‹æŽ’åº�ï¼Œä¼ å…¥æ¯”è¾ƒå™¨å¯¹è±¡
		Collections.sort(list, mc);

		// é��åŽ†åœ¨listä¸­æŽ’åº�ä¹‹å�Žçš„HashMap
		for (Iterator<Map.Entry<Integer, Integer>> it = list.iterator(); it.hasNext();) {

			result.add(it.next().getKey());

			if (--topK <= 0) {
				break;
			}
		}

		return result;
	}

	private static class StringValueComparator implements Comparator<Map.Entry<String, Integer>> {
		public int compare(Map.Entry<String, Integer> m, Map.Entry<String, Integer> n) {
			return (int) (n.getValue() - m.getValue());
		}
	}

	private static class IntegerValueComparator implements Comparator<Map.Entry<Integer, Integer>> {
		public int compare(Map.Entry<Integer, Integer> m, Map.Entry<Integer, Integer> n) {
			return (int) (n.getValue() - m.getValue());
		}
	}

	private BigInteger genIndexKey(BigInteger k1, long counter) {

		//System.out.println(k1.toString());
		return PRF.HMACSHA1ToBigInteger(String.valueOf(counter), k1.toString());
	}

    @SuppressWarnings("unused")
	private IndexValue encryptIndexValue(BigInteger k2, long id) {
    		
    		IndexValue indexValue = null;
    		
    		// convert key
    		byte[] keyBytes = BaseTool.bigIntegerTo128Bits(k2);
    		
    		// random gen IV
    		byte[] iv = BaseTool.longTo128Bits(rand.nextLong());
    		
    		// data
    		byte[] data =  BaseTool.longTo128Bits(id);
        
		try {
			byte[] cipher = MyAES.aesEnc(keyBytes, data, iv);
			
			indexValue = new IndexValue(iv, cipher);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    		return indexValue;
    }
    
    @SuppressWarnings("unused")
	private long decryptIndexValue(BigInteger k2, IndexValue indexValue) {
    	
    		long id = -1;
    		
    		// convert key
		byte[] keyBytes = BaseTool.bigIntegerTo128Bits(k2);
		
        byte[] cipher = indexValue.getCipher();
        
        byte[] iv = indexValue.getIv();
        
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