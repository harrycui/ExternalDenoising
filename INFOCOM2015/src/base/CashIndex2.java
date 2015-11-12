package base;

import tool.PRF;

import java.util.*;

import thread.MyCountDown;

/**
 *
 * Created by HarryC on 1/7/15.
 */
public class CashIndex2 {

    private int L;

    public static HashMap<Long, Integer> rawIndex;

    public HashMap<Integer, SiftDescriptor> siftMap;

    public static HashMap<Integer, Integer> featureRank;

    public static int[] featureRankArray;

    private HashMap<Long, Long> maxC; // used to record the maximum c for each lsh tag;

    private HashMap<Integer, String> idMap;

    public CashIndex2(int limit, int L) {

        this.L = L;
        this.maxC = new HashMap<Long, Long>();
        this.idMap = new HashMap<Integer, String>(limit);
        this.siftMap = new HashMap<Integer, SiftDescriptor>(limit);

        CashIndex2.rawIndex = new HashMap<Long, Integer>(limit);
    }

    public void insert(LSHVector lshVector, String imageId, int fid, String keyV, String keyR, String siftStr) {

        for (int i = 0; i < lshVector.getDimension(); ++i) {

            Long lshValue = lshVector.getLSHValueByIndex(i);

            long c = 0; // start from 0

            // TODO: double check the connection method
            long k1 = PRF.HMACSHA1ToUnsignedInt("1xx" + lshValue + "xx" + i, keyV);
            //String k1 = 1 + "xx" + lshValue + "xx" + i;
            //long k2 = Long.parseLong(2 + "00" + lshValue + "00" + i);

            if (maxC.containsKey(k1)) {

                c = maxC.get(k1) + 1;
                maxC.put(k1, c);
            } else {
                maxC.put(k1, 1L);
            }

            boolean successInsert = false;

            while (!successInsert) {

                long a = serverPosition(k1, c);

                //long tag = Long.parseLong(c + "0000" + lshValue + "0" + i);

                // if does not exist, directly insert
                if (!rawIndex.containsKey(a)) {
                    //System.out.println(a);
                    rawIndex.put(a, fid);

                    idMap.put(fid, imageId);

                    successInsert = true;
                    maxC.put(k1, c);
                }

                ++c;
            }
        }

        this.siftMap.put(fid, new SiftDescriptor(siftStr));
    }

    public HashMap<String, Integer> search(List<LSHVector> query, String keyV, String keyR) {

        // Step 1: generate digests on client

        long timeFlag1 = System.currentTimeMillis();

        List<List<CashDigest>> digests = new ArrayList<List<CashDigest>>(query.size());

        for (LSHVector lshVector : query) {

            List<CashDigest> digestsInL = new ArrayList<CashDigest>(L);

            for (int i = 0; i < lshVector.getDimension(); ++i) {

                digestsInL.add(new CashDigest(lshVector.getLSHValueByIndex(i), i, keyV, keyR));
            }

            digests.add(digestsInL);
        }

        System.out.println("Client side digest generate cost: " + (System.currentTimeMillis() - timeFlag1) + "ms.");

        // Step 2: search on server side

        long timeFlag2 = System.currentTimeMillis();

        HashMap<String, Integer> result = new HashMap<String, Integer>();

        for (List<CashDigest> digestInL : digests) {

            // Step 1: for each LSH query vector, get the nearest features


            // <fid, counter>
            //ConcurrentHashMap<Integer, Integer> featureRank = new ConcurrentHashMap<Integer, Integer>();
            featureRank = new HashMap<Integer, Integer>();

            //long timeFlag0 = System.currentTimeMillis();


            MyCountDown threadCounter = new MyCountDown(L);

            for (int i = 0; i < L; i++) {

                QueryThread t = new QueryThread("Thread " + i, threadCounter, i, digestInL);

                t.start();
            }

            // wait for all threads done
            while (true) {
                if (!threadCounter.hasNext())
                    break;
            }

            //System.out.println("Query feature cost: " + (System.currentTimeMillis() - timeFlag2) + "ms");

            /*
            for (int i = 0; i < L; ++i) {

                long k1 = digestInL.get(i).getK1();
                //long k2

                long c = 1; // start from 1

                while (true) {

                    long a = serverPosition(k1, c);

                    // if does not exist, directly insert
                    if (rawIndex.containsKey(a)) {

                        //long timeFlag01 = System.currentTimeMillis();

                        int fid = rawIndex.get(a);

                        CashIndex2.featureRankArray[fid] = CashIndex2.featureRankArray[fid] + 1;
                    } else {
                        break;
                    }

                    ++c;
                }
            }*/

            //long timeFlagArray = System.currentTimeMillis();
            for (int i = 1; i < CashIndex2.featureRankArray.length; ++i) {

                if (CashIndex2.featureRankArray[i] >= 2) {
                    featureRank.put(i, CashIndex2.featureRankArray[i]);
                }
                CashIndex2.featureRankArray[i] = 0;
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

                        String iid = idMap.get(fid);

                        int cc = featureRank.get(fid);

                        if (!result.containsKey(iid)) {

                            result.put(iid, cc * cc);
                        } else {

                            result.put(iid, result.get(iid) + cc * cc);
                        }
                    }
                }
            }

            //System.out.println("Rank feature cost: " + (System.currentTimeMillis() - timeFlagArray) + "ms");

            featureRank.clear();
            featureRank = null;
        }

        System.out.println("Query feature at server cost: " + (System.currentTimeMillis() - timeFlag2) + "ms");
        return result;
    }

    public HashMap<String, Integer> searchWithDist(QueryFile qf, String keyV, String keyR) {

        List<List<Double>> distForL = new ArrayList<List<Double>>(L);

        for (int i = 0; i < L; ++i) {

            distForL.add(new ArrayList<Double>());
        }

        // Step 1: generate digests on client

        long timeFlag1 = System.currentTimeMillis();

        List<List<CashDigest>> digests = new ArrayList<List<CashDigest>>(qf.getLshVectors().size());

        for (LSHVector lshVector : qf.getLshVectors()) {

            List<CashDigest> digestsInL = new ArrayList<CashDigest>(L);

            for (int i = 0; i < lshVector.getDimension(); ++i) {

                digestsInL.add(new CashDigest(lshVector.getLSHValueByIndex(i), i, keyV, keyR));
            }

            digests.add(digestsInL);
        }

        System.out.println("Client side digest generate cost: " + (System.currentTimeMillis() - timeFlag1) + "ms.");

        // Step 2: search on server side

        long timeFlag2 = System.currentTimeMillis();

        HashMap<String, Integer> result = new HashMap<String, Integer>();

        int featureId = 0;

        for (List<CashDigest> digestInL : digests) {

            // Step 1: for each LSH query vector, get the nearest features


            featureRank = new HashMap<Integer, Integer>();

            MyCountDown threadCounter = new MyCountDown(L);

            for (int i = 0; i < L; i++) {

                QueryThread t = new QueryThread("Thread " + i, threadCounter, i, digestInL);

                t.start();
            }

            // wait for all threads done
            while (true) {
                if (!threadCounter.hasNext())
                    break;
            }


            for (int i = 1; i < CashIndex2.featureRankArray.length; ++i) {

                int times = CashIndex2.featureRankArray[i];

                if (times >= 1) {

                    if (distForL.get(times - 1).size() < 1000) {
                        distForL.get(times - 1).add(SiftDescriptor.calculateDistance(qf.getSifts().get(featureId), siftMap.get(i)));
                    }
                }

                if (times >= 2) {
                    featureRank.put(i, times);
                }
                CashIndex2.featureRankArray[i] = 0;
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

                        String iid = idMap.get(fid);

                        int cc = featureRank.get(fid);

                        if (!result.containsKey(iid)) {

                            result.put(iid, cc * cc);
                        } else {

                            result.put(iid, result.get(iid) + cc * cc);
                        }
                    }
                }
            }

            //System.out.println("Rank feature cost: " + (System.currentTimeMillis() - timeFlagArray) + "ms");

            featureRank.clear();
            featureRank = null;

            featureId++;
        }

        System.out.println("Query feature at server cost: " + (System.currentTimeMillis() - timeFlag2) + "ms");

        for (int i = 0; i < L; ++i) {

            double avgDist = 0;

            for (int j = 0; j < distForL.get(i).size(); ++j) {

                avgDist += distForL.get(i).get(j);
            }

            System.out.println("\nFor l = " + (i + 1) + ", the total number is :" + distForL.get(i).size() + ", the average distance is :" + avgDist / distForL.get(i).size());
        }

        return result;
    }

    public HashMap<String, Integer> searchByUserDefinedThread(List<LSHVector> query, String keyV, String keyR, int threadNum) {

        // Step 1: generate digests on client

        long timeFlag1 = System.currentTimeMillis();

        List<List<CashDigest>> digests = new ArrayList<List<CashDigest>>(query.size());

        for (LSHVector lshVector : query) {

            List<CashDigest> digestsInL = new ArrayList<CashDigest>(L);

            for (int i = 0; i < lshVector.getDimension(); ++i) {

                digestsInL.add(new CashDigest(lshVector.getLSHValueByIndex(i), i, keyV, keyR));
            }

            digests.add(digestsInL);
        }

        System.out.println("Client side digest generate cost: " + (System.currentTimeMillis() - timeFlag1) + "ms.");

        // Step 2: search on server side

        long timeFlag2 = System.currentTimeMillis();

        HashMap<String, Integer> result = new HashMap<String, Integer>();

        for (List<CashDigest> digestInL : digests) {

            // Step 1: for each LSH query vector, get the nearest features


            // <fid, counter>
            //ConcurrentHashMap<Integer, Integer> featureRank = new ConcurrentHashMap<Integer, Integer>();
            featureRank = new HashMap<Integer, Integer>();

            for (int i = 0; i < L; ++i) {

                long k1 = digestInL.get(i).getK1();

                if (!maxC.containsKey(k1)) {
                    continue;
                }

                long upC = maxC.get(k1) + 1;

                if (upC < 2000) {

                    for (long c = 0; c < upC; ++c) {

                        long a = serverPosition(k1, c);

                        int fid = CashIndex2.rawIndex.get(a);

                        CashIndex2.featureRankArray[fid] = CashIndex2.featureRankArray[fid] + 1;
                    }

                } else {

                    MyCountDown threadCounter = new MyCountDown(threadNum);

                    for (int j = 0; j < threadNum; j++) {

                        QueryThreadV2 t = null;

                        if (j == threadNum - 1) {

                            t = new QueryThreadV2("Thread " + j, threadCounter, k1, upC / threadNum * j, upC);
                        } else {

                            t = new QueryThreadV2("Thread " + j, threadCounter, k1, upC / threadNum * j, upC / threadNum * (j + 1));
                        }

                        t.start();
                    }

                    // wait for all threads done
                    while (true) {
                        if (!threadCounter.hasNext())
                            break;
                    }
                }
            }

            //System.out.println("Query feature cost: " + (System.currentTimeMillis() - timeFlag2) + "ms");

            /*
            for (int i = 0; i < L; ++i) {

                long k1 = digestInL.get(i).getK1();
                //long k2

                long c = 1; // start from 1

                while (true) {

                    long a = serverPosition(k1, c);

                    // if does not exist, directly insert
                    if (rawIndex.containsKey(a)) {

                        //long timeFlag01 = System.currentTimeMillis();

                        int fid = rawIndex.get(a);

                        CashIndex2.featureRankArray[fid] = CashIndex2.featureRankArray[fid] + 1;
                    } else {
                        break;
                    }

                    ++c;
                }
            }*/

            //long timeFlagArray = System.currentTimeMillis();
            for (int i = 1; i < CashIndex2.featureRankArray.length; ++i) {

                if (CashIndex2.featureRankArray[i] >= 2) {
                    featureRank.put(i, CashIndex2.featureRankArray[i]);
                }
                CashIndex2.featureRankArray[i] = 0;
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

                        String iid = idMap.get(fid);

                        int cc = featureRank.get(fid);

                        if (!result.containsKey(iid)) {

                            result.put(iid, cc * cc);
                        } else {

                            result.put(iid, result.get(iid) + cc * cc);
                        }
                    }
                }
            }

            //System.out.println("Rank feature cost: " + (System.currentTimeMillis() - timeFlagArray) + "ms");

            featureRank.clear();
            featureRank = null;
        }

        System.out.println("Query feature at server cost: " + (System.currentTimeMillis() - timeFlag2) + "ms");
        return result;
    }

    /*
    public HashMap<String, Integer> search(List<LSHVector> query, String keyV, String keyR) {

        // Step 1: generate digests on client

        long timeFlag1 = System.currentTimeMillis();

        List<List<CashDigest>> digests = new ArrayList<List<CashDigest>>(query.size());

        for (LSHVector lshVector : query) {

            List<CashDigest> digestsInL = new ArrayList<CashDigest>(L);

            for (int i = 0; i < lshVector.getDimension(); ++i) {

                digestsInL.add(new CashDigest(lshVector.getLSHValueByIndex(i), i, keyV, keyR));
            }

            digests.add(digestsInL);
        }

        System.out.println("Client side digest generate cost: " + (System.currentTimeMillis() - timeFlag1) + "ms.");

        // Step 2: search on server side



        HashMap<String, Integer> result = new HashMap<String, Integer>();

        for (List<CashDigest> digestInL : digests) {

            // Step 1: for each LSH query vector, get the nearest features

            long timeFlag2 = System.currentTimeMillis();

            // <fid, counter>
            HashMap<Integer, Integer> featureRank = new HashMap<Integer, Integer>();

            //long timeFlag0 = System.currentTimeMillis();

            for (int i = 0; i < L; ++i) {

                long k1 = digestInL.get(i).getK1();
                //long k2

                long c = 1; // start from 1

                while (true) {

                    long a = serverPosition(k1, c);

                    // if does not exist, directly insert
                    if (rawIndex.containsKey(a)) {

                        //long timeFlag01 = System.currentTimeMillis();

                        int fid = rawIndex.get(a);

                        //System.out.println("Compute position cost: " + (System.currentTimeMillis() - timeFlag01) + "ms");
                        if (!featureRank.containsKey(fid)) {

                            featureRank.put(fid, 1);
                        } else {

                            featureRank.put(fid, featureRank.get(fid) + 1);
                        }
                    } else {
                        break;
                    }

                    ++c;
                }
            }

            System.out.println("Query feature cost: " + (System.currentTimeMillis() - timeFlag2) + "ms");

            // Step 2: rank this feature set
            if (featureRank.size() > 0) {

                long timeFlag3 = System.currentTimeMillis();

                List<Integer> fids = rankFeature(2, featureRank);

                System.out.println("Rank feature cost: " + (System.currentTimeMillis() - timeFlag3) + "ms");

                if (fids.size() > 0) {
                    //System.out.println("For query feature id = " + queryFid + ":");

                    // Step 3: based on top features, mark the imageId
                    for (Integer fid : fids) {

                        String iid = idMap.get(fid);

                        int cc = featureRank.get(fid);

                        if (!result.containsKey(iid)) {

                            result.put(iid, cc * cc);
                        } else {

                            result.put(iid, result.get(iid) + cc * cc);
                        }
                    }
                }
            }
        }

        return result;
    }*/

    /*
    public HashMap<String, Integer> search(List<LSHVector> query) {

        HashMap<String, Integer> result = new HashMap<String, Integer>();

        //int queryFid = 0;

        for (LSHVector lshVector : query) {

            //queryFid++;

            // Step 1: for each LSH query vector, get the nearest features

            // <fid, counter>
            HashMap<Integer, Integer> featureRank = new HashMap<Integer, Integer>();

            //long timeFlag0 = System.currentTimeMillis();

            for (int i = 0; i < lshVector.getDimension(); ++i) {

                Long lshValue = lshVector.getLSHValueByIndex(i);

                long c = 1; // start from 1

                // TODO: double check the connection method
                String k1 = 1 + "xx" + lshValue + "xx" + i;
                //long k2 = Long.parseLong(2 + "00" + lshValue + "00" + i);

                //boolean notExist = false;

                //int tmp = 0;

                while (true) {

                    long a = serverPosition(k1, c);

                    // if does not exist, directly insert
                    if (rawIndex.containsKey(a)) {

                        //long timeFlag01 = System.currentTimeMillis();

                        int fid = rawIndex.get(a);

                        //System.out.println("Compute position cost: " + (System.currentTimeMillis() - timeFlag01) + "ms");
                        if (!featureRank.containsKey(fid)) {

                            featureRank.put(fid, 1);
                        } else {

                            featureRank.put(fid, featureRank.get(fid) + 1);
                        }
                    } else {
                        break;
                    }

                    ++c;
                }
            }

            //System.out.println("Query feature cost: " + (System.currentTimeMillis() - timeFlag0) + "ms");

            // Step 2: rank this feature set
            if (featureRank.size() > 0) {

                //long timeFlag1 = System.currentTimeMillis();

                List<Integer> fids = rankFeature(2, featureRank);

                //System.out.println("Rank feature cost: " + (System.currentTimeMillis() - timeFlag1) + "ms");

                if (fids.size() > 0) {
                    //System.out.println("For query feature id = " + queryFid + ":");

                    // Step 3: based on top features, mark the imageId
                    for (Integer fid : fids) {

                        String iid = idMap.get(fid);

                        int cc = featureRank.get(fid);

                        if (!result.containsKey(iid)) {

                            result.put(iid, cc*cc);
                        } else {

                            result.put(iid, result.get(iid) + cc*cc);
                        }
                    }
                }
            }
        }

        return result;
    }
    */

    public List<Integer> rankFeature(int threshold, HashMap<Integer, Integer> featureRank) {

        List<Integer> result = new ArrayList<Integer>();

        List<Map.Entry<Integer, Integer>> list = new ArrayList<Map.Entry<Integer, Integer>>();

        // 把map转化为Map.Entry然后放到用于排序的list里面
        list.addAll(featureRank.entrySet());


        /*
        for (Iterator<Map.Entry<Integer, Integer>> it = featureRank.entrySet().iterator(); it.hasNext(); ) {

            Map.Entry<Integer, Integer> tmp = it.next();

            if (tmp.getValue() >= threshold) {

                list.add(tmp);
            }
        }*/

        // 调用内部类的构造器，如果这个内部类是静态内部类，就比这个好办点了。。
        CashIndex2.ValueComparator2 mc = new ValueComparator2();
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

    public static List<String> topK(int topK, HashMap<String, Integer> cc) {

        List<String> result = new ArrayList<String>();

        List<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>();
        // 把map转化为Map.Entry然后放到用于排序的list里面
        list.addAll(cc.entrySet());
        // 调用内部类的构造器，如果这个内部类是静态内部类，就比这个好办点了。。
        CashIndex2.ValueComparator mc = new ValueComparator();
        // 开始排序，传入比较器对象
        Collections.sort(list, mc);

        // 遍历在list中排序之后的HashMap
        for (Iterator<Map.Entry<String, Integer>> it = list.iterator(); it.hasNext(); ) {

            result.add(it.next().getKey());

            if (--topK <= 0) {
                break;
            }
        }

        return result;
    }

    private static class ValueComparator implements Comparator<Map.Entry<String, Integer>> {
        public int compare(Map.Entry<String, Integer> m, Map.Entry<String, Integer> n) {
            return (int) (n.getValue() - m.getValue());
        }
    }

    private static class ValueComparator2 implements Comparator<Map.Entry<Integer, Integer>> {
        public int compare(Map.Entry<Integer, Integer> m, Map.Entry<Integer, Integer> n) {
            return (int) (n.getValue() - m.getValue());
        }
    }

    private int serverPosition(long k1Vj, long counter) {

        return (int) (PRF.HMACSHA1ToUnsignedInt(String.valueOf(counter), Long.toString(k1Vj)));
    }
}

class QueryThread extends Thread {

    private MyCountDown threadCounter;

    private int lIndex;

    private List<CashDigest> digestInL;

    public QueryThread(String threadName, MyCountDown threadCounter, int lIndex, List<CashDigest> digestInL) {

        super(threadName);

        this.threadCounter = threadCounter;
        this.lIndex = lIndex;
        this.digestInL = digestInL;
    }

    public void run() {

        //System.out.println(getName() + " is running! at L index : " + this.lIndex);

        long k1 = digestInL.get(lIndex).getK1();
        //long k2

        long c = 0; // start from 0

        while (true) {

            long a = serverPosition(k1, c);

            // if does not exist, directly insert
            if (CashIndex2.rawIndex.containsKey(a)) {

                //long timeFlag01 = System.currentTimeMillis();

                int fid = CashIndex2.rawIndex.get(a);

                synchronized (this) {

                    CashIndex2.featureRankArray[fid] = CashIndex2.featureRankArray[fid] + 1;
                }

                //System.out.println("Compute position cost: " + (System.currentTimeMillis() - timeFlag01) + "ms");
                /*
                if (!CashIndex2.featureRank.containsKey(fid)) {

                    CashIndex2.featureRank.put(fid, 1);
                } else {

                    CashIndex2.featureRank.put(fid, CashIndex2.featureRank.get(fid) + 1);
                }*/
            } else {
                //System.out.println("Max c is " + c);
                break;
            }

            ++c;
        }
        threadCounter.countDown();
    }

    private int serverPosition(long k1Vj, long counter) {

        return (int) (PRF.HMACSHA1ToUnsignedInt(String.valueOf(counter), Long.toString(k1Vj)));
    }
}

class QueryThreadV2 extends Thread {

    private MyCountDown threadCounter;

    private long startC;

    private long endC;

    private long k1;

    public QueryThreadV2(String threadName, MyCountDown threadCounter, long k1, long startC, long endC) {

        super(threadName);

        this.threadCounter = threadCounter;
        this.k1 = k1;
        this.startC = startC;
        this.endC = endC;
    }

    public void run() {

        //System.out.println(getName() + " is running! startC = " + this.startC + ", endC = " + this.endC);

        for (long c = startC; c < endC; ++c) {

            long a = serverPosition(k1, c);

            int fid = CashIndex2.rawIndex.get(a);

            synchronized (this) {

                CashIndex2.featureRankArray[fid] = CashIndex2.featureRankArray[fid] + 1;
            }

            /*if (!CashIndex2.featureRank.containsKey(fid)) {

                CashIndex2.featureRank.put(fid, 1);
            } else {

                CashIndex2.featureRank.put(fid, CashIndex2.featureRank.get(fid) + 1);
            }*/
        }

        threadCounter.countDown();
    }

    private int serverPosition(long k1Vj, long counter) {

        return (int) (PRF.HMACSHA1ToUnsignedInt(String.valueOf(counter), Long.toString(k1Vj)));
    }
}