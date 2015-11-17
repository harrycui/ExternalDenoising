package test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import base.Image;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import base.Patch;
import java.util.TreeMap;

import javax.accessibility.AccessibleRelation;
import javax.security.auth.login.FailedLoginException;

import java.util.Comparator;
import base.SimilarPatches;


public class Testgroundtruth {

	public static void main(String[] args) {

		List<Image> queryImageList = new ArrayList<Image>();

		List<Image> dbImageList = new ArrayList<Image>();

		int numOfQueryImage = 2;

		int numOfPatchesInOneQueryImage = 3;

		int numOfDbImage = 2;

		int numOfPatchesInOneDbImage = 3; // By default: 1887

		int dim = 64;

		queryImageList = Tools.readImages(args[0], dim, numOfQueryImage, numOfPatchesInOneQueryImage);

		dbImageList = Tools.readImages(args[1], dim, numOfDbImage, numOfPatchesInOneDbImage);

		TreeMap<Integer, ArrayList<Patch>> distMap = new TreeMap<Integer, ArrayList<Patch>>();
		
		//process image
		
		List<SimilarPatches> similarPatchesList = new ArrayList<SimilarPatches>();
		
		for (int j = 0; j < numOfPatchesInOneQueryImage; j++) {

			queryImageList.get(0).getPatches().get(j).getPixels(); 
			
			int queryPid = queryImageList.get(0).getPatches().get(j).getPid();  

			for (int m = 0; m < numOfDbImage; m++) {

				for (int n = 0; n < numOfPatchesInOneDbImage; n++) {

					int tempDist = Tools.computeEuclideanDist(queryImageList.get(0).getPatches().get(j).getPixels(),
							dbImageList.get(m).getPatches().get(n).getPixels());
					
					int dbPid = dbImageList.get(m).getPatches().get(n).getPid();

					
					if (distMap.containsKey(tempDist)) {
						
						distMap.get(tempDist).add(dbImageList.get(m).getPatches().get(n));

//						distMap.get(tempDist).add(dbImageList.get(m).getPatches().get(dbPid));
					} else {

						ArrayList<Patch> patchList = new ArrayList<Patch>();
						
						patchList.add(dbImageList.get(m).getPatches().get(n));

						distMap.put(tempDist, patchList);
					}
				}
			}
			System.out.println("Up to here successfully!");
			

			//Get the IDs of the similar patches
			
			boolean flag=false;
			
			List <Patch> similarPatchList = new ArrayList<Patch>();
			
			int ctr = 0;
			
			int targetNumOfPatches = 5;
						
			for (Integer key : distMap.keySet()){
				
				for (int k=0; k<distMap.get(key).size(); k++ ){
					
					if(ctr>=targetNumOfPatches){
						
						flag = true;
						break;
					} else {
						
						similarPatchList.add(distMap.get(key).get(k));
						
					}
					
					ctr++;
					
					if(flag==true)
						break;
				}		
			}
			System.out.println(similarPatchList.get(2).getPid());
			
		}
		
		int[] tempSize = new int[2];
		
		tempSize = Tools.resizeImage(260, 360, 8, 1);
		
		
		

		

	}

}
