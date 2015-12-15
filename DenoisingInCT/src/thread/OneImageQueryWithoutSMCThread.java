package thread;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import base.LSHVector;
import base.Patch;
import base.PatchWithLSH;
import base.SimilarPatches;
import index.CashIndex;

public class OneImageQueryWithoutSMCThread extends Thread {
	
	private MyCountDown threadCounter;

	private short lshL;
	
	private String keyV;
	
	private String keyR;
	
	private int topK;
	
	private List<PatchWithLSH> queryPatches;
	
	private CashIndex cashIndex;
	
	private Map<Integer, Patch> rawDBPatchMap;
	
	private List<SimilarPatches> patches;

	public OneImageQueryWithoutSMCThread(String threadName, MyCountDown threadCounter, short lshL, String keyV, String keyR, int topK, List<PatchWithLSH> queryPatches, CashIndex cashIndex, Map<Integer, Patch> rawDBPatchMap, List<SimilarPatches> patches) {

        super(threadName);

        this.threadCounter = threadCounter;
        this.lshL = lshL;
        this.keyV = keyV;
        this.keyR = keyR;
        this.topK = topK;
        this.queryPatches = queryPatches;
        this.patches = patches;
        
        this.cashIndex = cashIndex;
        this.rawDBPatchMap = new HashMap<Integer, Patch>(rawDBPatchMap);
    }

	public void run() {
		
		//List<SimilarPatches> patches = new ArrayList<SimilarPatches>(queryPatches.size());
		
		for (int i = 0; i < queryPatches.size(); i++) {
			
			PatchWithLSH qp = queryPatches.get(i);

			LSHVector lshVector = new LSHVector(1, qp.getLshValues(), lshL);

			TreeMap<Integer, List<Integer>> searchResult = cashIndex.searchByOnePatch(lshVector, keyV, keyR);
			
			List<Patch> similarPatchesForOnePatch = new ArrayList<Patch>(topK);
			
			if (searchResult != null && searchResult.size() > 0) {
				
				Iterator<Map.Entry<Integer, List<Integer>>> entries = searchResult.entrySet().iterator();

				boolean isFullForOnePatch = false;
				while (entries.hasNext()) {

				    Map.Entry<Integer, List<Integer>> entry = entries.next();
				    
				    for (int j = 0; j < entry.getValue().size(); j++) {
				    	
				    	int pid = entry.getValue().get(j);
				    	
				    	//System.out.println(" " + pid + " - " + entry.getKey());
				    	
						similarPatchesForOnePatch.add(rawDBPatchMap.get(pid));
						
						if (similarPatchesForOnePatch.size() >= topK) {
							isFullForOnePatch = true;
							break;
						}
					}
				    
				    if (isFullForOnePatch) {
						break;
					}
				}
			}
			
			SimilarPatches sp = new SimilarPatches(i, qp, similarPatchesForOnePatch);
			
			patches.add(sp);

			//System.out.println("Patch No. " + (i + 1) + " is done.");
		}

		System.out.println(this.getName() + " is finished.");
		
		threadCounter.countDown();
	}
}