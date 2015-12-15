package test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import thread.MyCountDown;
import thread.QueryThread;
import util.ConfigParser;
import util.FileUtil;
import util.Tools;


public class BatchTestgroundtruthOpt {
	
	//public static final int NUM_OF_MAX_PATCH = 50;
	public static int topK;
	
	
	
	public static List<ImageInPatch> loadQuery(String queryFilePath, int step) {

		List<ImageInPatch> queryImages = new ArrayList<ImageInPatch>();

		List<File> queryFileList = FileUtil.loadFileList(queryFilePath);

		if (queryFileList.size() == 0) {

			System.out.println("\nNo query image is loaded. Please check the folder!");
		} else {

			System.out.println("\nLoading query images...");

			long startTime = System.currentTimeMillis();

			for (File qf : queryFileList) {

				ImageInPatch queryImage = Tools.readOneImageInPatches(queryFilePath + qf.getName());

				queryImages.add(queryImage);
			}

			long loadTime = System.currentTimeMillis() - startTime;

			// -------------------------------------------------------------------------------------------------------
			System.out.println("     ---> Done\n\nProcessed " + queryImages.size() + " images.\n\t\tLoad time : "
					+ loadTime + " ms\n");
		}

		return queryImages;

	}


	public static void main(String[] args) {
		
		if (args.length < 1) {

			System.out.println("Error: no argument provided!");
			
			return;
		}
		
		System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
		
		ConfigParser config = new ConfigParser(args[0]);
		int step = config.getInt("step");
		int overlap = config.getInt("overlap");
		int sigma = config.getInt("sigma");
		double k = Double.parseDouble(config.getString("k"));
		int numOfThread = config.getInt("numOfThread");
		
		topK = config.getInt("topK");
		
		String rootPath = config.getString("rootPath").replace("\\", "/");
		
		String dbPatchPath = rootPath + config.getString("dbPatchPath") + "patchDB-" + step + "-" + overlap + ".txt";
		
		boolean isShowImage = config.getBool("isShowImage");
		
		String outputPath = rootPath + config.getString("outputPath") + step + "-" + overlap + "/" + sigma + "/";
		
		String queryFilePath = rootPath + config.getString("queryPatchPath") + step + "-" + overlap + "/" + sigma + "/";
		
		// Start reading db and query patches
		Map<Integer, Patch> dbPatchesList = Tools.readPatches(dbPatchPath);	
		
		//ImageInPatch queryImageInPatch = Tools.readOneImageInPatches(queryPatchPath);
		
		List<ImageInPatch> queryImages = loadQuery(queryFilePath, step);
		
		for (int i = 1; i <= queryImages.size(); ++i) {
			
			ImageInPatch queryImageInPatch = queryImages.get(i-1);
			
			System.out.println("\nGroundtruth: Start processing image: " + queryImageInPatch.getName() + " -->> " + i + "/" + queryImages.size());
			
			String queryImageName = queryImageInPatch.getName();
			
			String queryImagePath = rootPath + config.getString("queryImagePath") + sigma + "/" + queryImageName;
			String oriImagePath = rootPath + config.getString("oriImagePath") + queryImageName.substring(0, queryImageName.indexOf('.')) + ".jpg";
			;
			
			Mat oriImageMat = Highgui.imread(oriImagePath);
			Mat queryImageMat = Highgui.imread(queryImagePath);
			
			int numOfPatchInOneRow = Tools.computeFitNumber(oriImageMat.cols(), step, overlap);
			int numOfPatchInOneCol = Tools.computeFitNumber(oriImageMat.rows(), step, overlap);
			int numOfPatchInOneImage = numOfPatchInOneCol * numOfPatchInOneRow;
			
			int fitWidth = Tools.computeFitSize(oriImageMat.cols(), step, overlap);
			int fitHeight = Tools.computeFitSize(oriImageMat.rows(), step, overlap);
			
			//String[] subStrs = queryImageName.split("\\.");
			//int sigma = Integer.parseInt(subStrs[1]);
			int threshold = (int)(1.126 * 1.126 * step * step * sigma * sigma);//Integer.parseInt(subStrs[2]);
			
			System.out.println("System parameters:\ntopK = " + topK + "\nsigma = " + sigma + "\nthreshold = " + threshold + "\nk = " + k);
			
			
			assert(numOfPatchInOneImage == queryImageInPatch.getPatches().size());
			
			System.out.println("Now, searching similar patches...\n");
			
			long startTime = System.currentTimeMillis();
				
			RecoverImage recoverImage = BatchTestgroundtruthOpt.findSimilarPatchesAtCloudOPT(queryImageInPatch, dbPatchesList, numOfPatchInOneImage, fitHeight, fitWidth, numOfPatchInOneRow, step, overlap, threshold, numOfThread);
			
			long stopTime1 = System.currentTimeMillis();

			System.out.println("Searching time is " + (stopTime1 - startTime) + " ms");
			
			System.out.println("Now, recovering image from patches...\n");
			
			Mat newImageMat = Tools.recoverImageFromPatches(recoverImage, step, threshold, sigma, k);
			
			// resize to original size
			Imgproc.resize(newImageMat, newImageMat, new Size(oriImageMat.cols(), oriImageMat.rows()));
			
			double psnr1 = Tools.psnr(oriImageMat, newImageMat);
			
			double psnr2 = Tools.psnr(oriImageMat, queryImageMat);
			
			//String outputFilePath = outputPath + "rec_" + queryImageName;
			
			String outputFilePath = outputPath + queryImageName.substring(0, queryImageName.lastIndexOf('.')) + "_Ori_" + String.format("%.4f", psnr2) + "_GT_" + String.format("%.4f", psnr1) + ".jpg";
			
			
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
			
			if (isShowImage) {
				
				Imshow im1 = new Imshow("The original image");
				im1.showImage(oriImageMat);

				Imshow im2 = new Imshow("The query image");
				im2.showImage(queryImageMat);

				Imshow im3 = new Imshow("The recovered image");
				im3.showImage(newImageMat);
			}
			
			System.out.println("System parameters:\ntopK = " + topK + "\nsigma = " + sigma + "\nthreshold = " + threshold + "\nk = " + k);
			
			System.out.println("\nPSNR between original and new = " + psnr1);
			System.out.println("\nPSNR between original and query = " + psnr2);
		}
		
		
		
		System.out.println("All done.\n");
			
	}

	private static RecoverImage findSimilarPatchesAtCloudOPT(ImageInPatch image, Map<Integer, Patch> dbPatchesMap, int numOfPatchInOneImage, int fitHeight, int fitWidth, int patchWidth, int step, int overlap, int threshold, int numOfThread) {
		
		List<List<SimilarPatches>> result = new ArrayList<List<SimilarPatches>>(numOfThread);
		
		MyCountDown threadCounter = new MyCountDown(numOfThread);
		
		int numOfTimesInOneThread = image.getPatches().size() / numOfThread;
		
		for (int i = 0; i < numOfThread; ++i) {
			
			List<Patch> queryPatchesInThread = new ArrayList<Patch>();
			
			int startIndex = i * numOfTimesInOneThread;
			
			int endIndex = (i + 1) * numOfTimesInOneThread - 1;
			
			if (i == numOfThread - 1) {
				
				endIndex = image.getPatches().size() - 1;
			}
			
			System.out.println("[" + startIndex + ", " + endIndex + "]");
			
			List<SimilarPatches> resultInOneThread = new ArrayList<SimilarPatches>(endIndex - startIndex + 1);
			
			result.add(resultInOneThread);
			
			queryPatchesInThread.addAll(image.getPatches().subList(startIndex, endIndex + 1));
			
			QueryThread t = new QueryThread("Thread-" + (i + 1), threadCounter, threshold, queryPatchesInThread, dbPatchesMap, resultInOneThread);
			
			t.start();
		}
		
		while (true) {
			if (!threadCounter.hasNext())
				break;
		}
		
		List<SimilarPatches> patches = new ArrayList<SimilarPatches>(image.getPatches().size());
		
		for (int i = 0; i < numOfThread; ++i) {
			
			patches.addAll(result.get(i));
		}
		
		return new RecoverImage(image.getName(), numOfPatchInOneImage, fitHeight, fitWidth, patchWidth, step, overlap, patches);
	}
}
