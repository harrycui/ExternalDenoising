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
import index.SecureCashIndex;
import test.DenoisingPhaseOneTest;
import util.Tools;

public class OneImageQueryWithSMCThread extends Thread {
	
	private MyCountDown threadCounter;

	private short lshL;
	
	private String keyV;
	
	private String keyR;
	
	private int topK;
	
	private int threshold;
	
	private List<PatchWithLSH> queryPatches;
	
	private SecureCashIndex cashIndex;
	
	private Map<Integer, Patch> rawDBPatchMap;
	
	private List<SimilarPatches> patches;
	
	private boolean isShowTime;

	public OneImageQueryWithSMCThread(String threadName, MyCountDown threadCounter, short lshL, String keyV, String keyR, int topK, int threshold, List<PatchWithLSH> queryPatches, SecureCashIndex cashIndex, Map<Integer, Patch> rawDBPatchMap, List<SimilarPatches> patches, boolean isShowTime) {

        super(threadName);

        this.threadCounter = threadCounter;
        this.lshL = lshL;
        this.keyV = keyV;
        this.keyR = keyR;
        this.topK = topK;
        this.threshold = threshold;
        this.queryPatches = queryPatches;
        this.patches = patches;
        
        this.cashIndex = cashIndex;
        this.rawDBPatchMap = new HashMap<Integer, Patch>(rawDBPatchMap);
        
        this.isShowTime = isShowTime;
    }

	public void run() {

		//List<SimilarPatches> patches = new ArrayList<SimilarPatches>(queryPatches.size());
		
		for (int i = 0; i < queryPatches.size(); i++) {
			
			PatchWithLSH qp = queryPatches.get(i);

			LSHVector lshVector = new LSHVector(1, qp.getLshValues(), lshL);

			TreeMap<Integer, List<Integer>> searchResult = cashIndex.searchByOnePatch(lshVector, keyV, keyR, isShowTime);
			
			List<Patch> similarPatchesForOnePatch = new ArrayList<Patch>(topK);
			
			if (searchResult != null && searchResult.size() > 0) {
				
				Iterator<Map.Entry<Integer, List<Integer>>> entries = searchResult.entrySet().iterator();

				boolean isFullForOnePatch = false;
				
				while (entries.hasNext()) {

				    Map.Entry<Integer, List<Integer>> entry = entries.next();
				    
				    for (int j = 0; j < entry.getValue().size(); j++) {
				    	
				    	int pid = entry.getValue().get(j);
				    	
				    	int dist = Tools.computeEuclideanDist(rawDBPatchMap.get(pid).getPixels(), qp.getPixels());
						
						if (dist <= threshold) {

							//System.out.println(" " + pid + " - " + entry.getKey() + " - " + dist);
							
							similarPatchesForOnePatch.add(DenoisingPhaseOneTest.rawDBPatchMap.get(pid));
							
							if (similarPatchesForOnePatch.size() >= topK) {
								isFullForOnePatch = true;
								break;
							}
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