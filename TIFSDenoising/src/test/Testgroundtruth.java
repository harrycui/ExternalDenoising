package test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import base.Image;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import base.Patch;
import base.RecoverImage;
import base.SimilarPatches;
import util.Tools;

import java.util.TreeMap;

import javax.accessibility.AccessibleRelation;
import javax.security.auth.login.FailedLoginException;

import org.opencv.core.Core;

import java.util.Comparator;


public class Testgroundtruth {
	
	public static final int PATCH_NUM_IN_ONE_IMAGE = 1924;
	
	public static final int IMAGE_HEIGHT = 365;
	
	public static final int IMAGE_WIDTH = 260;
	
	public static final int IMAGE_STEP = 8;
	
	public static final int IMAGE_OVERLAP = 1;
	
	public static final int NUM_OF_MAX_PATCH = 50;
	
	public static final int PATCH_WIDTH = 37; // 37 * 52

	public static void main(String[] args) {
		
		if (args.length < 6) {
			
			System.out.println("Please check the arguments: \n[db patch path] [query image path] [num of query images] [num of patches in one image] [threshold] [sigma]");
			
			return;
		}
		
		System.loadLibrary( Core.NATIVE_LIBRARY_NAME );

		int numOfQueryImages = Integer.parseInt(args[2]);
		int numOfPatchesInOneImage = Integer.parseInt(args[3]);
		int threshold = Integer.parseInt(args[4]);
		int sigma = Integer.parseInt(args[5]);
		
		Map<Integer, Patch> dbPatchesList = Tools.readPatches(args[0]);
		List<Image> queryImageList = Tools.readImages(args[1], numOfQueryImages, numOfPatchesInOneImage);
		
		for (int i = 0; i < queryImageList.size(); ++i) {
			
			System.out.println("Now, process image No. " + (i + 1));
			
			RecoverImage recoverImage = Testgroundtruth.findSimilarPatchesAtCloud(queryImageList.get(i), dbPatchesList, threshold);
			
			Tools.recoverImageFromPatches("/Users/cuihelei/Desktop/", recoverImage, threshold, sigma);
			
			System.out.println("Done.\n");
			
			break;
		}
	}

	private static RecoverImage findSimilarPatchesAtCloud(Image image, Map<Integer, Patch> dbPatchesMap, int threshold) {
		
		List<SimilarPatches> patches = new ArrayList<SimilarPatches>(image.getPatches().size());
		
		for (int i = 0; i < image.getPatches().size(); i++) {
			
			Patch qp = image.getPatches().get(i);
			
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
					
					if (similarPatchesForOnePatch.size() >= Testgroundtruth.NUM_OF_MAX_PATCH) {
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
		}
		
		return new RecoverImage(image.getName(), Testgroundtruth.PATCH_NUM_IN_ONE_IMAGE, Testgroundtruth.IMAGE_HEIGHT, Testgroundtruth.IMAGE_WIDTH, Testgroundtruth.IMAGE_STEP, Testgroundtruth.IMAGE_OVERLAP, Testgroundtruth.PATCH_WIDTH, patches);
	}

}
