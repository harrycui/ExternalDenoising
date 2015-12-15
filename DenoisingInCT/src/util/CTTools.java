package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import base.Imshow;
import base.LSHVector;
import base.Patch;
import base.PatchWithLSH;
import base.QueryImage;
import base.RecoverImage;
import base.SimilarPatches;
import test.DenoisingPhaseOneTest;
import thread.MyCountDown;
import thread.OneImageQueryWithSMCThread;
import thread.OneImageQueryWithoutSMCThread;

public class CTTools {
	public static void queryByOnePatchWithoutSMC(BufferedReader br, String keyV, String keyR, short lshL, int step, int sigma) {

		System.out.println("\nModel: query by one patch.");

		if (!DenoisingPhaseOneTest.isQueryImagesLoaded) {

			System.out.println("Warning: Please load the query images first.");
		} else {

			System.out
					.println("First, please indicate the query image range from [1, " + DenoisingPhaseOneTest.queryImages.size()
						+ "]: (-1 means return to root menu): (-1 means return to root menu)");

			int queryImageIndex = 0;
			
			boolean returnToRoot = false;
			
			while (true) {
				
				String inputStr = null;

				try {
					inputStr = br.readLine();
				} catch (IOException e) {
					e.printStackTrace();
				}

				try {
					if (inputStr == null || inputStr.equals("-1")) {

						System.out.println("Return to root menu!");
						returnToRoot = true;
						break;
					} else if (Integer.parseInt(inputStr) > DenoisingPhaseOneTest.queryImages.size() + 1 || Integer.parseInt(inputStr) < 1) {

						System.out.println("Warning: the id should be limited in [1, " + DenoisingPhaseOneTest.queryImages.size() + "], please try again!");

						continue;
					}  else {
						queryImageIndex = Integer.parseInt(inputStr);

						System.out.println("The query image id : " + queryImageIndex);
						
						break;
					}
				} catch (NumberFormatException e) {
					System.out.println("Warning: please input again.");
					continue;
				}
			}

			QueryImage qi = DenoisingPhaseOneTest.queryImages.get(queryImageIndex - 1);
			
			//int threshold = (int)(1.126 * 1.126 * step * step * sigma * sigma);
			
			int numOfPatches = qi.getPatches().size();

			while (!returnToRoot) {

				System.out.println("\nNow, you can search by input you query patch index range from [1, " + numOfPatches
						+ "]: (-1 means return to root menu)");

				String queryStr = null;
				int queryIndex;

				try {
					queryStr = br.readLine();
				} catch (IOException e) {
					e.printStackTrace();
				}

				try {
					if (queryStr == null || queryStr.equals("-1")) {

						System.out.println("Return to root menu!");

						break;
					} else if (Integer.parseInt(queryStr) > numOfPatches || Integer.parseInt(queryStr) < 1) {

						System.out.println("Warning: query patch index should be limited in [1, " + numOfPatches + "]");

						continue;
					} else {
						queryIndex = Integer.parseInt(queryStr);

						System.out.println("For query patch index: " + queryIndex);
					}
				} catch (NumberFormatException e) {
					System.out.println("Warning: query patch index should be limited in [1, " + numOfPatches + "]");
					continue;
				}

				PatchWithLSH queryPatch = qi.getPatchByPatchIndex(queryIndex - 1);

				LSHVector lshVector = new LSHVector(1, queryPatch.getLshValues(), lshL);

				TreeMap<Integer, List<Integer>> searchResult = DenoisingPhaseOneTest.cashIndex.searchByOnePatch(lshVector, keyV, keyR);

				if (searchResult != null && searchResult.size() > 0) {
					
					System.out.println("\n\nThe search results are: top-" + DenoisingPhaseOneTest.TOP_K_PATCH + "\npid - occurrence");
					
					Iterator<Map.Entry<Integer, List<Integer>>> entries = searchResult.entrySet().iterator();

					List<Patch> similarPatchesForOnePatch = new ArrayList<Patch>(DenoisingPhaseOneTest.TOP_K_PATCH);
					boolean isFullForOnePatch = false;
					while (entries.hasNext()) {

					    Map.Entry<Integer, List<Integer>> entry = entries.next();
					    
					    for (int j = 0; j < entry.getValue().size(); j++) {
					    	
					    	int pid = entry.getValue().get(j);
					    	
					    	System.out.println(" " + pid + " - " + entry.getKey());
					    	
							similarPatchesForOnePatch.add(DenoisingPhaseOneTest.rawDBPatchMap.get(pid));
							
							if (similarPatchesForOnePatch.size() >= DenoisingPhaseOneTest.TOP_K_PATCH) {
								isFullForOnePatch = true;
								break;
							}
						}
					    
					    if (isFullForOnePatch) {
							break;
						}
					}

					// TODO: hardcode
					//List<Integer> topKId = CashIndex.topKPatches(DenoisingPhaseOneTest.TOP_K_PATCH, searchResult);

					//System.out.println("\n\nThe search results are: top-" + DenoisingPhaseOneTest.TOP_K_PATCH + "\npid - occurrence");

					/*for (Integer id : topKId) {

						System.out.println(" " + id + " - " + searchResult.get(id));
					}*/
					
					System.out.println("\nDone!");
				} else {

					System.out.println("No similar item!!!");
				}
			}
		}
	}
	

	public static void queryByOnePatchWithSMC(BufferedReader br, String keyV, String keyR, short lshL, int step, int sigma) {

		System.out.println("\nModel: query by one patch.");

		if (!DenoisingPhaseOneTest.isQueryImagesLoaded || !DenoisingPhaseOneTest.isRawDataLoaded) {

			System.out.println("Warning: Please load the query images or raw data first.");
		} else {

			System.out.println("First, please indicate the query image range from [1, " + DenoisingPhaseOneTest.queryImages.size()
				+ "]: (-1 means return to root menu): (-1 means return to root menu)");

			int queryImageIndex = 0;
			
			boolean returnToRoot = false;
			
			while (true) {
				
				String inputStr = null;

				try {
					inputStr = br.readLine();
				} catch (IOException e) {
					e.printStackTrace();
				}

				try {
					if (inputStr == null || inputStr.equals("-1")) {

						System.out.println("Return to root menu!");
						returnToRoot = true;
						break;
					} else if (Integer.parseInt(inputStr) > DenoisingPhaseOneTest.queryImages.size() + 1 || Integer.parseInt(inputStr) < 1) {

						System.out.println("Warning: the id should be limited in [1, " + DenoisingPhaseOneTest.queryImages.size() + "], please try again!");

						continue;
					}  else {
						queryImageIndex = Integer.parseInt(inputStr);

						System.out.println("The query image id : " + queryImageIndex);
						
						break;
					}
				} catch (NumberFormatException e) {
					System.out.println("Warning: please input again.");
					continue;
				}
			}

			QueryImage qi = DenoisingPhaseOneTest.queryImages.get(queryImageIndex - 1);
			
			int threshold = (int)(1.126 * 1.126 * step * step * sigma * sigma);
			
			int numOfPatches = qi.getPatches().size();

			while (!returnToRoot) {

				System.out.println("\nNow, you can search by input you query patch index range from [1, " + numOfPatches
						+ "]: (-1 means return to root menu)");

				String queryStr = null;
				int queryIndex;

				try {
					queryStr = br.readLine();
				} catch (IOException e) {
					e.printStackTrace();
				}

				try {
					if (queryStr == null || queryStr.equals("-1")) {

						System.out.println("Return to root menu!");

						break;
					} else if (Integer.parseInt(queryStr) > numOfPatches || Integer.parseInt(queryStr) < 1) {

						System.out.println("Warning: query patch index should be limited in [1, " + numOfPatches + "]");

						continue;
					} else {
						queryIndex = Integer.parseInt(queryStr);

						System.out.println("For query patch index: " + queryIndex);
					}
				} catch (NumberFormatException e) {
					System.out.println("Warning: query patch index should be limited in [1, " + numOfPatches + "]");
					continue;
				}

				// from 0
				int patchIndex = (queryIndex - 1);

				PatchWithLSH queryPatch = qi.getPatchByPatchIndex(patchIndex);

				LSHVector lshVector = new LSHVector(1, queryPatch.getLshValues(), lshL);

				TreeMap<Integer, List<Integer>> searchResult = DenoisingPhaseOneTest.cashIndex.searchByOnePatch(lshVector, keyV, keyR);

				if (searchResult != null && searchResult.size() > 0) {
					
					System.out.println("\n\nThe search results are: top-" + DenoisingPhaseOneTest.TOP_K_PATCH + "\npid - occurrence");
					
					Iterator<Map.Entry<Integer, List<Integer>>> entries = searchResult.entrySet().iterator();

					List<Patch> similarPatchesForOnePatch = new ArrayList<Patch>(DenoisingPhaseOneTest.TOP_K_PATCH);
					
					boolean isFullForOnePatch = false;
					
					int numOfGood = 0;
					int numOfBad = 0;
					
					while (entries.hasNext()) {

					    Map.Entry<Integer, List<Integer>> entry = entries.next();
					    
					    for (int j = 0; j < entry.getValue().size(); j++) {
					    	
					    	int pid = entry.getValue().get(j);
					    	
					    	int dist = Tools.computeEuclideanDist(DenoisingPhaseOneTest.rawDBPatchMap.get(pid).getPixels(),	queryPatch.getPixels());
							
							if (dist <= threshold) {

								System.out.println(" " + pid + " - " + entry.getKey() + " - " + dist);
								
								similarPatchesForOnePatch.add(DenoisingPhaseOneTest.rawDBPatchMap.get(pid));
								
								++numOfGood;
								
								if (similarPatchesForOnePatch.size() >= DenoisingPhaseOneTest.TOP_K_PATCH) {
									isFullForOnePatch = true;
									break;
								}
							} else {
								++numOfBad;
							}
						}
					    
					    if (isFullForOnePatch) {
							break;
						}
					}
					
					System.out.println("To find " + numOfGood + " good patches, there are " + numOfBad + " bad patches.");

					System.out.println("\nDone!");
				} else {

					System.out.println("No similar item!!!");
				}
			}
		}
	}

	public static void queryByOneImageWithoutSMC(BufferedReader br, String keyV, String keyR, short lshL, int step, Integer overlap, int sigma, double k, String queryImagePath, String oriImagePath, String outputPath, int numOfThread, boolean isShowImage) {

		System.out.println("\nModel: query by one image without smc.");

		if (!DenoisingPhaseOneTest.isQueryImagesLoaded || !DenoisingPhaseOneTest.isRawDataLoaded) {

			System.out.println("Warning: Please load the query images or raw data first.");
		} else {

			System.out
					.println("First, please indicate the top-k number: (-1 means return to root menu)");

			int topK = 0;
			
			boolean returnToRoot = false;
			
			while (true) {
				
				String inputStr = null;

				try {
					inputStr = br.readLine();
				} catch (IOException e) {
					e.printStackTrace();
				}

				try {
					if (inputStr == null || inputStr.equals("-1")) {

						System.out.println("Return to root menu!");
						returnToRoot = true;
						break;
					} else {
						
						topK = Integer.parseInt(inputStr);

						System.out.println("The top-k is : " + topK);
						
						break;
					}
				} catch (NumberFormatException e) {
					System.out.println("Warning: please input again.");
					continue;
				}
			}
			
			int numOfImages = DenoisingPhaseOneTest.queryImages.size();

			while (!returnToRoot) {

				System.out.println("\nModel: query by one image without smc.");
				
				System.out.println("\nNow, you can search by input you query image index range from [1, " + numOfImages
						+ "]: (-1 means return to root menu)");

				String queryStr = null;
				int queryIndex;

				try {
					queryStr = br.readLine();
				} catch (IOException e) {
					e.printStackTrace();
				}

				try {
					if (queryStr == null || queryStr.equals("-1")) {

						System.out.println("Return to root menu!");

						break;
					} else if (Integer.parseInt(queryStr) > numOfImages || Integer.parseInt(queryStr) < 1) {

						System.out.println("Warning: query patch index should be limited in [1, " + numOfImages + "]");

						continue;
					} else {
						queryIndex = Integer.parseInt(queryStr);

						System.out.println("For query image index: " + queryIndex);
					}
				} catch (NumberFormatException e) {
					System.out.println("Warning: query patch index should be limited in [1, " + numOfImages + "]");
					continue;
				}
				
				long startTime = System.currentTimeMillis();
				
				QueryImage qi = DenoisingPhaseOneTest.queryImages.get(queryIndex - 1);
				
				List<List<SimilarPatches>> result = new ArrayList<List<SimilarPatches>>(numOfThread);
				
				MyCountDown threadCounter = new MyCountDown(numOfThread);
				
				int numOfTimesInOneThread = qi.getPatches().size() / numOfThread;
				
				for (int i = 0; i < numOfThread; ++i) {
					
					List<PatchWithLSH> queryPatchesInThread = new ArrayList<PatchWithLSH>();
					
					int startIndex = i * numOfTimesInOneThread;
					
					int endIndex = (i + 1) * numOfTimesInOneThread - 1;
					
					if (i == numOfThread - 1) {
						
						endIndex = qi.getPatches().size() - 1;
					}
					
					List<SimilarPatches> resultInOneThread = new ArrayList<SimilarPatches>(endIndex - startIndex + 1);
					
					result.add(resultInOneThread);
					
					queryPatchesInThread.addAll(qi.getPatches().subList(startIndex, endIndex + 1));
					
					OneImageQueryWithoutSMCThread t = new OneImageQueryWithoutSMCThread("Thread-" + (i + 1), threadCounter, lshL, keyV, keyR, topK, queryPatchesInThread, DenoisingPhaseOneTest.cashIndex, DenoisingPhaseOneTest.rawDBPatchMap, resultInOneThread);
					
					t.start();
					
					System.out.println("Thread-" + (i + 1) + " is running... -> [" + startIndex + ", " + endIndex + "]");
				}
				
				while (true) {
					if (!threadCounter.hasNext())
						break;
				}
				
				List<SimilarPatches> patches = new ArrayList<SimilarPatches>(qi.getPatches().size());
				
				for (int i = 0; i < numOfThread; ++i) {
					
					patches.addAll(result.get(i));
				}
				
				
				/*
				for (int i = 0; i < qi.getPatches().size(); ++i) {
					
					PatchWithLSH qp = qi.getPatchByPatchIndex(i);

					LSHVector lshVector = new LSHVector(1, qp.getLshValues(), lshL);

					HashMap<Integer, Integer> searchResult = cashIndex.searchByOnePatch(lshVector, keyV, keyR);
					
					List<Patch> similarPatchesForOnePatch = new ArrayList<Patch>(topK);
					
					if (searchResult != null && searchResult.size() > 0) {
						
						List<Integer> topKId = CashIndex.topKPatches(topK, searchResult);

						for (Integer id : topKId) {

							//System.out.println(" " + id + " - " + searchResult.get(id));
							similarPatchesForOnePatch.add(rawDBPatchMap.get(id));
						}
						
					}
					
					SimilarPatches sp = new SimilarPatches(i, qp, similarPatchesForOnePatch);
					
					patches.add(sp);

					System.out.println("Patch No. " + (i + 1) + " is done.");
				}
				*/
				
				long stopTime1 = System.currentTimeMillis();

				System.out.println("\n\nSearching time is " + (stopTime1 - startTime) + " ms");
				
				prepareToRecoverImage(qi.getName(), patches, topK, step, overlap, sigma, k, queryImagePath, oriImagePath, outputPath+"/withoutSMC_", isShowImage);
				
			}
		}
	}
	
	public static void prepareToRecoverImage(String queryImageName, List<SimilarPatches> patches, int topK, int step, int overlap, int sigma, double k, String queryImagePath, String oriImagePath, String outputPath, boolean isShowImage) {
		
		System.out.println("Start simulating client side recovering...");
		
		int threshold = (int)(1.126 * 1.126 * step * step * sigma * sigma);
		
		oriImagePath = oriImagePath + queryImageName.substring(0, queryImageName.indexOf(".")) + ".jpg";
		
		queryImagePath = queryImagePath + queryImageName;
		
		Mat oriImageMat = Highgui.imread(oriImagePath);
		Mat queryImageMat = Highgui.imread(queryImagePath);
		
		assert(oriImageMat != null && queryImageMat != null);
		
		int numOfPatchInOneRow = Tools.computeFitNumber(oriImageMat.cols(), step, overlap);
		int numOfPatchInOneCol = Tools.computeFitNumber(oriImageMat.rows(), step, overlap);
		int numOfPatchInOneImage = numOfPatchInOneCol * numOfPatchInOneRow;
		
		int fitWidth = Tools.computeFitSize(oriImageMat.cols(), step, overlap);
		int fitHeight = Tools.computeFitSize(oriImageMat.rows(), step, overlap);
		
		RecoverImage ri = new RecoverImage(queryImageName, numOfPatchInOneImage, fitHeight, fitWidth, numOfPatchInOneRow, step, overlap, patches);
		
		Mat newImageMat = Tools.recoverImageFromPatches(ri, step, threshold, sigma, k);
		
		// resize to original size
		Imgproc.resize(newImageMat, newImageMat, new Size(oriImageMat.cols(), oriImageMat.rows()));
		
		double psnr1 = Tools.psnr(oriImageMat, newImageMat);
		
		double psnr2 = Tools.psnr(oriImageMat, queryImageMat);
		
		String outputFilePath = outputPath + queryImageName.substring(0, queryImageName.lastIndexOf('.')) + "_Ori_" + String.format("%.4f", psnr2) + "_Rec_" + String.format("%.4f", psnr1) + ".jpg";
		
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
		
		System.out.println("\nFor the query image " + queryImageName);
		
		System.out.println("\nPSNR between original and new = " + psnr1);
		System.out.println("\nPSNR between original and query = " + psnr2);
		
		System.out.println("Done.\n");
	}

	public static void queryByOneImageWithSMC(BufferedReader br, String keyV, String keyR, short lshL, int step, Integer overlap, int sigma, double k, String queryImagePath, String oriImagePath, String outputPath, int numOfThread, boolean isShowImage) {

		System.out.println("\nModel: query by one image with smc.");

		if (!DenoisingPhaseOneTest.isQueryImagesLoaded || !DenoisingPhaseOneTest.isRawDataLoaded) {

			System.out.println("Warning: Please load the query images or raw data first.");
		} else {

			System.out
					.println("First, please indicate the top-k number: (-1 means return to root menu)");

			int topK = 0;
			
			boolean returnToRoot = false;
			
			while (true) {
				
				String inputStr = null;

				try {
					inputStr = br.readLine();
				} catch (IOException e) {
					e.printStackTrace();
				}

				try {
					if (inputStr == null || inputStr.equals("-1")) {

						System.out.println("Return to root menu!");
						returnToRoot = true;
						break;
					} else {
						
						topK = Integer.parseInt(inputStr);

						System.out.println("The top-k is : " + topK);
						
						break;
					}
				} catch (NumberFormatException e) {
					System.out.println("Warning: please input again.");
					continue;
				}
			}
			
			int numOfImages = DenoisingPhaseOneTest.queryImages.size();

			while (!returnToRoot) {

				System.out.println("\nModel: query by one image with smc.");
				
				System.out.println("\nNow, you can search by input you query image index range from [1, " + numOfImages
						+ "]: (-1 means return to root menu)");

				String queryStr = null;
				int queryIndex;

				try {
					queryStr = br.readLine();
				} catch (IOException e) {
					e.printStackTrace();
				}

				try {
					if (queryStr == null || queryStr.equals("-1")) {

						System.out.println("Return to root menu!");

						break;
					} else if (Integer.parseInt(queryStr) > numOfImages || Integer.parseInt(queryStr) < 1) {

						System.out.println("Warning: query patch index should be limited in [1, " + numOfImages + "]");

						continue;
					} else {
						queryIndex = Integer.parseInt(queryStr);

						System.out.println("For query image index: " + queryIndex);
					}
				} catch (NumberFormatException e) {
					System.out.println("Warning: query patch index should be limited in [1, " + numOfImages + "]");
					continue;
				}
				
				long startTime = System.currentTimeMillis();
				
				QueryImage qi = DenoisingPhaseOneTest.queryImages.get(queryIndex - 1);
				
				int threshold = (int)(1.126 * 1.126 * step * step * sigma * sigma);
				
				List<List<SimilarPatches>> result = new ArrayList<List<SimilarPatches>>(numOfThread);
				
				MyCountDown threadCounter = new MyCountDown(numOfThread);
				
				int numOfTimesInOneThread = qi.getPatches().size() / numOfThread;
				
				for (int i = 0; i < numOfThread; ++i) {
					
					List<PatchWithLSH> queryPatchesInThread = new ArrayList<PatchWithLSH>();
					
					int startIndex = i * numOfTimesInOneThread;
					
					int endIndex = (i + 1) * numOfTimesInOneThread - 1;
					
					if (i == numOfThread - 1) {
						
						endIndex = qi.getPatches().size() - 1;
					}
					
					System.out.println("[" + startIndex + ", " + endIndex + "]");
					
					List<SimilarPatches> resultInOneThread = new ArrayList<SimilarPatches>(endIndex - startIndex + 1);
					
					result.add(resultInOneThread);
					
					queryPatchesInThread.addAll(qi.getPatches().subList(startIndex, endIndex + 1));
					
					OneImageQueryWithSMCThread t = new OneImageQueryWithSMCThread("Thread-" + (i + 1), threadCounter, lshL, keyV, keyR, topK, threshold, queryPatchesInThread, DenoisingPhaseOneTest.cashIndex, DenoisingPhaseOneTest.rawDBPatchMap, resultInOneThread);
					
					t.start();
				}
				
				while (true) {
					if (!threadCounter.hasNext())
						break;
				}
				
				List<SimilarPatches> patches = new ArrayList<SimilarPatches>(qi.getPatches().size());
				
				for (int i = 0; i < numOfThread; ++i) {
					
					patches.addAll(result.get(i));
				}

				/*long startTime = System.currentTimeMillis();
				
				QueryImage qi = queryImages.get(queryIndex - 1);
				
				List<SimilarPatches> patches = new ArrayList<SimilarPatches>(qi.getPatches().size());
				
				int threshold = (int)(1.126 * 1.126 * step * step * sigma * sigma);
				
				for (int i = 0; i < qi.getPatches().size(); ++i) {
					
					PatchWithLSH qp = qi.getPatchByPatchIndex(i);

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
				}*/
				
				long stopTime1 = System.currentTimeMillis();

				System.out.println("\n\nSearching time is " + (stopTime1 - startTime) + " ms");
				
				prepareToRecoverImage(qi.getName(), patches, topK, step, overlap, sigma, k, queryImagePath, oriImagePath, outputPath+"/withSMC_", isShowImage);
				
			}
		}
	}

	public static void queryByOneImageWithoutSMCBatch(BufferedReader br, String keyV, String keyR, short lshL, int step, Integer overlap, int sigma, double k, String queryImagePath, String oriImagePath, String outputPath, int numOfThread, boolean isShowImage) {

		System.out.println("\nModel: batch query by images without smc.");

		if (!DenoisingPhaseOneTest.isQueryImagesLoaded || !DenoisingPhaseOneTest.isRawDataLoaded) {

			System.out.println("Warning: Please load the query images or raw data first.");
		} else {

			System.out
					.println("First, please indicate the top-k number: (-1 means return to root menu)");

			int topK = 0;
			
			boolean returnToRoot = false;
			
			while (true) {
				
				String inputStr = null;

				try {
					inputStr = br.readLine();
				} catch (IOException e) {
					e.printStackTrace();
				}

				try {
					if (inputStr == null || inputStr.equals("-1")) {

						System.out.println("Return to root menu!");
						returnToRoot = true;
						break;
					} else {
						
						topK = Integer.parseInt(inputStr);

						System.out.println("The top-k is : " + topK);
						
						break;
					}
				} catch (NumberFormatException e) {
					System.out.println("Warning: please input again.");
					continue;
				}
			}
			
			if (!returnToRoot) {

				int numOfImages = DenoisingPhaseOneTest.queryImages.size();

				for (int queryIndex = 1; queryIndex <= numOfImages; ++queryIndex) {

					QueryImage qi = DenoisingPhaseOneTest.queryImages.get(queryIndex - 1);

					System.out.println("\nWithout SMC: Start processing image: " + qi.getName() + " -->> " + queryIndex + "/" + numOfImages);

					List<List<SimilarPatches>> result = new ArrayList<List<SimilarPatches>>(numOfThread);

					MyCountDown threadCounter = new MyCountDown(numOfThread);

					int numOfTimesInOneThread = qi.getPatches().size() / numOfThread;

					for (int i = 0; i < numOfThread; ++i) {

						List<PatchWithLSH> queryPatchesInThread = new ArrayList<PatchWithLSH>();

						int startIndex = i * numOfTimesInOneThread;

						int endIndex = (i + 1) * numOfTimesInOneThread - 1;

						if (i == numOfThread - 1) {

							endIndex = qi.getPatches().size() - 1;
						}

						List<SimilarPatches> resultInOneThread = new ArrayList<SimilarPatches>(
								endIndex - startIndex + 1);

						result.add(resultInOneThread);

						queryPatchesInThread.addAll(qi.getPatches().subList(startIndex, endIndex + 1));

						OneImageQueryWithoutSMCThread t = new OneImageQueryWithoutSMCThread("Thread-" + (i + 1),
								threadCounter, lshL, keyV, keyR, topK, queryPatchesInThread,
								DenoisingPhaseOneTest.cashIndex, DenoisingPhaseOneTest.rawDBPatchMap,
								resultInOneThread);

						t.start();

						System.out.println(
								"Thread-" + (i + 1) + " is running... -> [" + startIndex + ", " + endIndex + "]");
					}

					while (true) {
						if (!threadCounter.hasNext())
							break;
					}

					List<SimilarPatches> patches = new ArrayList<SimilarPatches>(qi.getPatches().size());

					for (int i = 0; i < numOfThread; ++i) {

						patches.addAll(result.get(i));
					}

					prepareToRecoverImage(qi.getName(), patches, topK, step, overlap, sigma, k, queryImagePath,
							oriImagePath, outputPath+"/withoutSMC_", isShowImage);
				}

				System.out.println("\nAll done.");
			}
		}
	}
	
	public static void queryByOneImageWithSMCBatch(BufferedReader br, String keyV, String keyR, short lshL, int step, Integer overlap, int sigma, double k, String queryImagePath, String oriImagePath, String outputPath, int numOfThread, boolean isShowImage) {

		System.out.println("\nModel: query by one image with smc.");

		if (!DenoisingPhaseOneTest.isQueryImagesLoaded || !DenoisingPhaseOneTest.isRawDataLoaded) {

			System.out.println("Warning: Please load the query images or raw data first.");
		} else {

			System.out
					.println("First, please indicate the top-k number: (-1 means return to root menu)");

			int topK = 0;
			
			boolean returnToRoot = false;
			
			while (true) {
				
				String inputStr = null;

				try {
					inputStr = br.readLine();
				} catch (IOException e) {
					e.printStackTrace();
				}

				try {
					if (inputStr == null || inputStr.equals("-1")) {

						System.out.println("Return to root menu!");
						returnToRoot = true;
						break;
					} else {
						
						topK = Integer.parseInt(inputStr);

						System.out.println("The top-k is : " + topK);
						
						break;
					}
				} catch (NumberFormatException e) {
					System.out.println("Warning: please input again.");
					continue;
				}
			}
			
			if (!returnToRoot) {

				int numOfImages = DenoisingPhaseOneTest.queryImages.size();

				for (int queryIndex = 1; queryIndex <= numOfImages; ++queryIndex) {

					QueryImage qi = DenoisingPhaseOneTest.queryImages.get(queryIndex - 1);

					System.out.println("\nWith SMC: Start processing image: " + qi.getName() + " -->> " + queryIndex + "/" + numOfImages);

					int threshold = (int) (1.126 * 1.126 * step * step * sigma * sigma);

					List<List<SimilarPatches>> result = new ArrayList<List<SimilarPatches>>(numOfThread);

					MyCountDown threadCounter = new MyCountDown(numOfThread);

					int numOfTimesInOneThread = qi.getPatches().size() / numOfThread;

					for (int i = 0; i < numOfThread; ++i) {

						List<PatchWithLSH> queryPatchesInThread = new ArrayList<PatchWithLSH>();

						int startIndex = i * numOfTimesInOneThread;

						int endIndex = (i + 1) * numOfTimesInOneThread - 1;

						if (i == numOfThread - 1) {

							endIndex = qi.getPatches().size() - 1;
						}

						System.out.println("[" + startIndex + ", " + endIndex + "]");

						List<SimilarPatches> resultInOneThread = new ArrayList<SimilarPatches>(
								endIndex - startIndex + 1);

						result.add(resultInOneThread);

						queryPatchesInThread.addAll(qi.getPatches().subList(startIndex, endIndex + 1));

						OneImageQueryWithSMCThread t = new OneImageQueryWithSMCThread("Thread-" + (i + 1),
								threadCounter, lshL, keyV, keyR, topK, threshold, queryPatchesInThread,
								DenoisingPhaseOneTest.cashIndex, DenoisingPhaseOneTest.rawDBPatchMap,
								resultInOneThread);

						t.start();
					}

					while (true) {
						if (!threadCounter.hasNext())
							break;
					}

					List<SimilarPatches> patches = new ArrayList<SimilarPatches>(qi.getPatches().size());

					for (int i = 0; i < numOfThread; ++i) {

						patches.addAll(result.get(i));
					}

					prepareToRecoverImage(qi.getName(), patches, topK, step, overlap, sigma, k, queryImagePath,
							oriImagePath, outputPath+"/withSMC_", isShowImage);

				}
			}
		}
	}
}
