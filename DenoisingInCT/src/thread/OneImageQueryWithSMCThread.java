package thread;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import base.LSHVector;
import base.Patch;
import base.PatchWithLSH;
import base.SimilarPatches;
import index.CashIndex;
import util.Tools;

public class OneImageQueryWithSMCThread extends Thread {
	
	private MyCountDown threadCounter;

	private short lshL;
	
	private String keyV;
	
	private String keyR;
	
	private int topK;
	
	private int threshold;
	
	private List<PatchWithLSH> queryPatches;
	
	private CashIndex cashIndex;
	
	private Map<Integer, Patch> rawDBPatchMap;
	
	private List<SimilarPatches> patches;

	public OneImageQueryWithSMCThread(String threadName, MyCountDown threadCounter, short lshL, String keyV, String keyR, int topK, int threshold, List<PatchWithLSH> queryPatches, CashIndex cashIndex, Map<Integer, Patch> rawDBPatchMap, List<SimilarPatches> patches) {

        super(threadName);

        this.threadCounter = threadCounter;
        this.lshL = lshL;
        this.keyV = keyV;
        this.keyR = keyR;
        this.topK = topK;
        this.threshold = threshold;
        this.queryPatches = queryPatches;
        this.patches = patches;
        
        this.cashIndex = new CashIndex(cashIndex);
        this.rawDBPatchMap = new HashMap<Integer, Patch>(rawDBPatchMap);
    }

	public void run() {

		//List<SimilarPatches> patches = new ArrayList<SimilarPatches>(queryPatches.size());
		
		for (int i = 0; i < queryPatches.size(); i++) {
			
			PatchWithLSH qp = queryPatches.get(i);

			LSHVector lshVector = new LSHVector(1, qp.getLshValues(), lshL);

			HashMap<Integer, Integer> searchResult = cashIndex.searchByOnePatch(lshVector, keyV, keyR);
			
			List<Patch> similarPatchesForOnePatch = new ArrayList<Patch>(topK);
			
			if (searchResult != null && searchResult.size() > 0) {
				
				boolean isFull = false;
				
				int numOfGood = 0;
				
				int currentRange = topK;
				
				Set<Integer> filteredResult = new HashSet<Integer>();
				
				do {

					currentRange *= 2;
					
					if (currentRange > searchResult.size()) {
						currentRange = searchResult.size();
					}

					List<Integer> topKId = CashIndex.topKPatches(currentRange, searchResult);

					for (Integer id : topKId) {

						// System.out.println(" " + id + " - " +
						// searchResult.get(id));
						
						if (!filteredResult.contains(id)) {
							
							int dist = Tools.computeEuclideanDist(rawDBPatchMap.get(id).getPixels(), qp.getPixels());
							
							if (dist <= threshold) {

								filteredResult.add(id);
								similarPatchesForOnePatch.add(rawDBPatchMap.get(id));
								
								if (++numOfGood >= topK) {
									isFull = true;
									break;
								}
							}
						}
					}
				} while (!isFull && currentRange != searchResult.size());
				
			}
			
			SimilarPatches sp = new SimilarPatches(i, qp, similarPatchesForOnePatch);
			
			patches.add(sp);

			System.out.println("Patch No. " + (i + 1) + " is done.");
		}

		System.out.println(this.getName() + " is finished.");
		
		threadCounter.countDown();
	}
}