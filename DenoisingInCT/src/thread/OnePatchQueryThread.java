package thread;

import java.util.List;

import base.CashDigest;
import index.CashIndex;
import util.PRF;

public class OnePatchQueryThread extends Thread {
	private MyCountDown threadCounter;

	private int lIndex;

	private List<CashDigest> digestInL;

	public OnePatchQueryThread(String threadName, MyCountDown threadCounter, int lIndex, List<CashDigest> digestInL) {

        super(threadName);

        this.threadCounter = threadCounter;
        this.lIndex = lIndex;
        this.digestInL = digestInL;
    }

	public void run() {

		// System.out.println(getName() + " is running! at L index : " +
		// this.lIndex);

		long k1 = digestInL.get(lIndex).getK1();

		long c = 0; // start from 0

		while (true) {

			long a = serverPosition(k1, c);
			// if does not exist, directly insert
			if (CashIndex.staticIndex.containsKey(a)) {

				int pid = CashIndex.staticIndex.get(a);

				long revid = (int) (PRF.HMACSHA1ToUnsignedInt(String.valueOf(pid), Long.toString(k1)));

				if (!CashIndex.revidSet.contains(revid)) {

					synchronized (this) {

						//CashIndex.featureRankArray[fid] = CashIndex.featureRankArray[fid] + 1;
						if(CashIndex.tempPatchResult.containsKey(pid)) {
							CashIndex.tempPatchResult.put(pid, CashIndex.tempPatchResult.get(pid) + 1);
						} else {
							CashIndex.tempPatchResult.put(pid, 1);
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

			long a = serverPosition(k1, c);
			// if does not exist, directly insert
			if (CashIndex.dynamicIndex.containsKey(a)) {

				int pid = CashIndex.dynamicIndex.get(a);

				long revid = (int) (PRF.HMACSHA1ToUnsignedInt(String.valueOf(pid), Long.toString(k1)));
				if (!CashIndex.revidSet.contains(revid)) {

					synchronized (this) {

						//CashIndex.featureRankArray[fid] = CashIndex.featureRankArray[fid] + 1;
						//CashIndex.featureRankArray[fid] = CashIndex.featureRankArray[fid] + 1;
						if(CashIndex.tempPatchResult.containsKey(pid)) {
							CashIndex.tempPatchResult.put(pid, CashIndex.tempPatchResult.get(pid) + 1);
						} else {
							CashIndex.tempPatchResult.put(pid, 1);
						}
					}
				}
			} else {
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