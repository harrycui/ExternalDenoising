package thread;

import tool.PRF;

import java.util.*;

import base.CashDigest;
import base.CashIndex3;

/**
 * Created by Helei on 12/3/2015.
 */
public class TMCQueryThreadV3 extends Thread {

    private MyCountDown threadCounter;

    private List<List<CashDigest>> digests;

    private int[] threadFeatureRankArray;

    public TMCQueryThreadV3(String threadName, MyCountDown threadCounter, List<List<CashDigest>> digests) {

        super(threadName);

        this.threadCounter = threadCounter;
        this.digests = digests;
        this.threadFeatureRankArray = new int[CashIndex3.featureRankArray.length];
    }

    public void run() {

        //System.out.println(getName() + " is running!");// startIndex = " + this.startIndex + ", endIndex = " + this.endIndex);

        for (List<CashDigest> digestInL : digests) {

            // <fid, counter>
            //ConcurrentHashMap<Integer, Integer> featureRank = new ConcurrentHashMap<Integer, Integer>();
            HashMap<Integer, Integer> featureRank = new HashMap<Integer, Integer>();

            for (int i = 0; i < digestInL.size(); ++i) {

                long k1 = digestInL.get(i).getK1();

                long c = 0; // start from 0

                while (true) {

                    long a = serverPosition(k1, c);
                    // if does not exist, directly insert
                    if (CashIndex3.rawIndex.containsKey(a)) {

                        int fid = CashIndex3.rawIndex.get(a);

                        long revid = (int) (PRF.HMACSHA1ToUnsignedInt(String.valueOf(fid), Long.toString(k1)));
                        if (!CashIndex3.revidSet.contains(revid)) {

                            this.threadFeatureRankArray[fid] = this.threadFeatureRankArray[fid] + 1;
                        }
                    } else {
                        break;
                    }

                    ++c;
                }

                // deal with the added table
                c = 0; // start from 0

                while (true) {

                    long a = serverPosition(k1, c);
                    // if does not exist, directly insert
                    if (CashIndex3.dynamicIndex.containsKey(a)) {

                        int fid = CashIndex3.dynamicIndex.get(a);

                        long revid = (int) (PRF.HMACSHA1ToUnsignedInt(String.valueOf(fid), Long.toString(k1)));
                        if (!CashIndex3.revidSet.contains(revid)) {

                            this.threadFeatureRankArray[fid] = this.threadFeatureRankArray[fid] + 1;
                        }
                    } else {
                        break;
                    }

                    ++c;
                }
            }

            //long timeFlagArray = System.currentTimeMillis();
            for (int i = 1; i < this.threadFeatureRankArray.length; ++i) {

                if (this.threadFeatureRankArray[i] >= 2) {
                    featureRank.put(i, this.threadFeatureRankArray[i]);
                }
                this.threadFeatureRankArray[i] = 0;
            }
            //System.out.println("Filter feature cost: " + (System.currentTimeMillis() - timeFlagArray) + "ms");

            //long timeFlagRank = System.currentTimeMillis();

            // Step 2: rank this feature set
            if (featureRank.size() > 0) {

                //long timeFlag3 = System.currentTimeMillis();

                List<Integer> fids = rankFeature(2, featureRank);

                //System.out.println("Rank feature cost: " + (System.currentTimeMillis() - timeFlag3) + "ms");

                if (fids.size() > 0) {
                    //System.out.println("For query feature id = " + queryFid + ":");

                    // Step 3: based on top features, mark the imageId
                    for (Integer fid : fids) {

                        String iid = CashIndex3.idMap.get(fid);

                        int cc = featureRank.get(fid);

                        synchronized (this) {
                            if (!CashIndex3.searchResult.containsKey(iid)) {

                                CashIndex3.searchResult.put(iid, cc * cc);
                            } else {

                                CashIndex3.searchResult.put(iid, CashIndex3.searchResult.get(iid) + cc * cc);
                            }
                        }
                    }
                }
            }

            //System.out.println("Rank feature cost: " + (System.currentTimeMillis() - timeFlagArray) + "ms");

            featureRank.clear();
            featureRank = null;
        }



        threadCounter.countDown();
    }

    private int serverPosition(long k1Vj, long counter) {

        return (int) (PRF.HMACSHA1ToUnsignedInt(String.valueOf(counter), Long.toString(k1Vj)));
    }

    public List<Integer> rankFeature(int threshold, HashMap<Integer, Integer> featureRank) {

        List<Integer> result = new ArrayList<Integer>();

        List<Map.Entry<Integer, Integer>> list = new ArrayList<Map.Entry<Integer, Integer>>();

        // 把map转化为Map.Entry然后放到用于排序的list里面
        list.addAll(featureRank.entrySet());

        // 调用内部类的构造器，如果这个内部类是静态内部类，就比这个好办点了。。
        TMCQueryThreadV3.ValueComparator mc = new ValueComparator();
        // 开始排序，传入比较器对象
        Collections.sort(list, mc);

        // 遍历在list中排序之后的HashMap
        for (Iterator<Map.Entry<Integer, Integer>> it = list.iterator(); it.hasNext(); ) {

            //Map.Entry<Integer, Integer> tmp = it.next();

            //if (tmp.getValue() >= threshold) {

            //System.out.println("fid = " + tmp.getKey() + ", iid = " + idMap.get(tmp.getKey()) + ", counter = " + tmp.getValue());
            //System.out.println(tmp.getKey());
            result.add(it.next().getKey());
            //}
        }

        return result;
    }

    private static class ValueComparator implements Comparator<Map.Entry<Integer, Integer>> {
        public int compare(Map.Entry<Integer, Integer> m, Map.Entry<Integer, Integer> n) {
            return (int) (n.getValue() - m.getValue());
        }
    }
}
