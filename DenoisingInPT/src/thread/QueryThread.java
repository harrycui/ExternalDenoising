package thread;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import base.Patch;
import base.SimilarPatches;
import test.TestgroundtruthOpt;
import util.Tools;

public class QueryThread extends Thread {
	
	private MyCountDown threadCounter;

	private int threshold;
	
	private List<Patch> queryPatches;
	
	private Map<Integer, Patch> dbPatchesMap;
	
	private List<SimilarPatches> patches;

	public QueryThread(String threadName, MyCountDown threadCounter, int threshold, List<Patch> queryPatches, Map<Integer, Patch> dbPatchesMap, List<SimilarPatches> patches) {

        super(threadName);

        this.threadCounter = threadCounter;
        this.threshold = threshold;
        this.queryPatches = queryPatches;
        this.patches = patches;
        
        this.dbPatchesMap = new HashMap<Integer, Patch>(dbPatchesMap);
    }

	public void run() {

		//List<SimilarPatches> patches = new ArrayList<SimilarPatches>(queryPatches.size());
		
		for (int i = 0; i < queryPatches.size(); i++) {
			
			Patch qp = queryPatches.get(i);
			
			// key - dist, value - patch list
			TreeMap<Integer, List<Integer>> distMap = new TreeMap<Integer, List<Integer>>();
			
			Iterator<Map.Entry<Integer, Patch>> entries = dbPatchesMap.entrySet().iterator();

			while (entries.hasNext()) {

			    Map.Entry<Integer, Patch> entry = entries.next();

				int tempDist = Tools.computeEuclideanDist(qp.getPixels(), entry.getValue().getPixels());
				
				//System.out.println(tempDist);
				
				if (tempDist <= threshold) {
					
					//System.out.println("find one");
					if (distMap.containsKey(tempDist)) {
						
						distMap.get(tempDist).add(entry.getKey());
					} else {

						List<Integer> patchList = new ArrayList<Integer>();
						
						patchList.add(entry.getKey());

						distMap.put(tempDist, patchList);
					}
				}
			}
			
			Iterator<Map.Entry<Integer, List<Integer>>> entries2 = distMap.entrySet().iterator();

			List<Patch> similarPatchesForOnePatch = new ArrayList<Patch>(50);
			boolean isFullForOnePatch = false;
			while (entries2.hasNext()) {

			    Map.Entry<Integer, List<Integer>> entry = entries2.next();
			    
			    //System.out.println("Dist: " + entry.getKey());
			    
			    for (int j = 0; j < entry.getValue().size(); j++) {
			    	
					similarPatchesForOnePatch.add(dbPatchesMap.get(entry.getValue().get(j)));
					
					if (similarPatchesForOnePatch.size() >= TestgroundtruthOpt.NUM_OF_MAX_PATCH) {
						isFullForOnePatch = true;
						break;
					}
				}
			    
			    if (isFullForOnePatch) {
					break;
				}
			}
			
			SimilarPatches sp = new SimilarPatches(i, qp, similarPatchesForOnePatch);
			
			patches.add(sp);
			
			System.out.println("Patch No. " + (i + 1) + " is done.");
		}

		System.out.println(this.getName() + " is finished.");
		
		threadCounter.countDown();
	}
}