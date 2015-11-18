package test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import base.ImageInPatch;
import base.Imshow;
import base.Patch;
import base.RecoverImage;
import base.SimilarPatches;
import util.ConfigParser;
import util.Tools;


public class Testgroundtruth {
	
	public static final int NUM_OF_MAX_PATCH = 50;

	public static void main(String[] args) {
		
		if (args.length < 1) {

			System.out.println("Error: no argument provided!");
			
			return;
		}
		
		System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
		
		ConfigParser config = new ConfigParser(args[0]);
		
		
		String testImageName = config.getString("testImageName");

		int step = config.getInt("step");
		int overlap = config.getInt("overlap");
		int sigma = config.getInt("sigma");
		double k = Double.parseDouble(config.getString("k"));
		
		String rootPath = config.getString("rootPath").replace("\\", "/");
		
		String dbPatchPath = rootPath + config.getString("dbPatchPath") + "patchDB-" + step + "-" + overlap + ".txt";
		String queryPatchPath = rootPath + config.getString("queryPatchPath") + step + "-" + overlap + "/" + sigma + "/"
				+ testImageName + "." + sigma + ".jpg.txt";
		String queryImagePath = rootPath + config.getString("queryImagePath") + sigma + "/" + testImageName + "." + sigma + ".jpg";
		String oriImagePath = rootPath + config.getString("oriImagePath") + testImageName + ".jpg";
		;
		String outputPath = rootPath + config.getString("outputPath") + step + "-" + overlap + "/" + sigma + "/";
		
		
		Mat oriImageMat = Highgui.imread(oriImagePath);
		Mat queryImageMat = Highgui.imread(queryImagePath);
		
		int numOfPatchInOneRow = Tools.computeFitNumber(oriImageMat.cols(), step, overlap);
		int numOfPatchInOneCol = Tools.computeFitNumber(oriImageMat.rows(), step, overlap);
		int numOfPatchInOneImage = numOfPatchInOneCol * numOfPatchInOneRow;
		
		int fitWidth = Tools.computeFitSize(oriImageMat.cols(), step, overlap);
		int fitHeight = Tools.computeFitSize(oriImageMat.rows(), step, overlap);
		
		// E.g., test1.10.8114.txt -> test[No.].[sigma].[threshold].txt
		String queryImageName = queryImagePath.substring(queryImagePath.lastIndexOf("/") + 1);
		
		//String[] subStrs = queryImageName.split("\\.");
		//int sigma = Integer.parseInt(subStrs[1]);
		int threshold = (int)(1.126 * 1.126 * step * step * sigma * sigma);//Integer.parseInt(subStrs[2]);
		
		System.out.println("System parameters:\nsigma = " + sigma + "\nthreshold = " + threshold);
		
		// Start reading db and query patches
		Map<Integer, Patch> dbPatchesList = Tools.readPatches(dbPatchPath);
		ImageInPatch queryImageInPatch = Tools.readOneImageInPatches(queryPatchPath);
		
		assert(numOfPatchInOneImage == queryImageInPatch.getPatches().size());
		
		System.out.println("Now, searching similar patches...\n");
		
		long startTime = System.currentTimeMillis();
			
		RecoverImage recoverImage = Testgroundtruth.findSimilarPatchesAtCloud(queryImageInPatch, dbPatchesList, numOfPatchInOneImage, fitHeight, fitWidth, numOfPatchInOneRow, step, overlap, threshold);
		
		long stopTime1 = System.currentTimeMillis();

		System.out.println("Searching time is " + (stopTime1 - startTime) + " ms");
		
		System.out.println("Now, recovering image from patches...\n");
		
		Mat newImageMat = Tools.recoverImageFromPatches(recoverImage, step, threshold, sigma, k);
		
		// resize to original size
		Imgproc.resize(newImageMat, newImageMat, new Size(oriImageMat.cols(), oriImageMat.rows()));
		
		String outputFilePath = outputPath + "rec_" + queryImageName;
		
		File file = new File(outputFilePath);
		
		if (!file.exists()) {
			file.getParentFile().mkdirs();
		    try {
		        file.createNewFile();
		    } catch (IOException e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
		    }
		}

		Highgui.imwrite(outputFilePath, newImageMat);
		
		Imshow im1 = new Imshow("The original image");
		im1.showImage(oriImageMat);
		
		Imshow im2 = new Imshow("The query image");
		im2.showImage(queryImageMat);
		
		Imshow im3 = new Imshow("The recovered image");
		im3.showImage(newImageMat);
		
		double psnr1 = Tools.psnr(oriImageMat, newImageMat);
		
		double psnr2 = Tools.psnr(oriImageMat, queryImageMat);
		
		System.out.println("\nPSNR between original and new = " + psnr1);
		System.out.println("\nPSNR between original and query = " + psnr2);
		
		System.out.println("Done.\n");
			
	}

	private static RecoverImage findSimilarPatchesAtCloud(ImageInPatch image, Map<Integer, Patch> dbPatchesMap, int numOfPatchInOneImage, int fitHeight, int fitWidth, int patchWidth, int step, int overlap, int threshold) {
		
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
			
			System.out.println("Patch No. " + (i + 1) + " is done.");
		}
		
		return new RecoverImage(image.getName(), numOfPatchInOneImage, fitHeight, fitWidth, patchWidth, step, overlap, patches);
	}

}
