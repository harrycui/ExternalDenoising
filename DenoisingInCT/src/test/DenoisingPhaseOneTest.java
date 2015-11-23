package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import base.Constant;
import base.Imshow;
import base.LSHVector;
import base.Patch;
import base.PatchWithLSH;
import base.QueryImage;
import base.RecoverImage;
import base.SimilarPatches;
import index.CashIndex;
import thread.MyCountDown;
import thread.OneImageQueryWithSMCThread;
import thread.OneImageQueryWithoutSMCThread;
import util.ConfigParser;
import util.FileUtil;
import util.Tools;

/**
 *
 * This version is based on the INFOCOM-2015 testing. I made this for the paper about Image External Denoising.
 *
 * Created by HarryC on 7-Oct-2015.
 */
public class DenoisingPhaseOneTest {
	
	public static final int TOP_K_PATCH = 50;
	
	public static final int NUM_OF_PRE_LOADED_PATCH = 100;

	public static boolean isQueryImagesLoaded = false;

	public static boolean isRawDataLoaded = false;

	public static CashIndex cashIndex;

	public static List<QueryImage> queryImages;

	// used for simulated evaluation
	public static Map<Integer, Patch> rawDBPatchMap;

	public static void buildIndex(String dbFilePath, short lshL, int limitNum, String keyV, String keyR) {

		cashIndex = new CashIndex(limitNum * lshL, lshL);

		BufferedReader reader = null;

		try {

			System.out.println("\nLoading db lsh file...");

			long startTime = System.currentTimeMillis();

			String tempString;

			int lineNumber = 0;
			
			reader = new BufferedReader(new FileReader(new File(dbFilePath)));

			while ((tempString = reader.readLine()) != null) {

				++lineNumber;
				
				String[] subStrs = tempString.split(":");

				String[] nameStr = subStrs[0].split("-");

				int pid = Integer.parseInt(nameStr[1]);

				LSHVector lshVector = new LSHVector(pid, subStrs[1].replace("\n", ""), lshL);

				cashIndex.insert(lshVector, nameStr[0], pid, keyV, keyR);

				if (lineNumber % (limitNum / 100) == 0) {
					System.out.println("Inserting " + lineNumber / (limitNum / 100) + "%");
				}

				if (lineNumber == limitNum) {
					break;
				}
			}

			long insertTime = System.currentTimeMillis() - startTime;

			// -------------------------------------------------------------------------------------------------------
			System.out.print("     ---> Done\n\nProcessed " + lineNumber + " patches!!\n\t\tInsert time : " + insertTime
					+ " ms\n");

			System.out.println("        ---> Done");

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	public static void loadQuery(String queryFilePath, short lshL, int step) {

		if (isQueryImagesLoaded) {

			System.out.println("\nWarning: the query images have been loaded!\n");

		} else {

			queryImages = new ArrayList<QueryImage>();
			
			List<File> queryFileList = FileUtil.loadFileList(queryFilePath);
			
			if (queryFileList.size() == 0) {
				
				System.out.println("\nNo query image is loaded. Please check the folder!");
			} else {
				
				System.out.println("\nLoading query images...");
				
				BufferedReader reader = null;

				try {

					long startTime = System.currentTimeMillis();
					
					int iid = 1;
					
					for (File qf : queryFileList) {
						
						reader = new BufferedReader(new FileReader(qf));
						
						List<PatchWithLSH> tempPatches = new ArrayList<PatchWithLSH>();
						
						String tempString;
						
						String imageName = "";
						
						while ((tempString = reader.readLine()) != null) {

							String[] subStrs = tempString.split(":");

							String[] nameStr = subStrs[0].split("-");
							
							imageName = nameStr[0];

							int pid = Integer.parseInt(nameStr[1]);

							tempPatches.add(new PatchWithLSH(pid, step * step, subStrs[2], subStrs[1], lshL));
						}
						
						QueryImage queryImage = new QueryImage(imageName, iid++, tempPatches);

						queryImages.add(queryImage);
					}
					
					if (queryImages.size() > 0) {
						
						isQueryImagesLoaded = true;
					}

					long loadTime = System.currentTimeMillis() - startTime;

					// -------------------------------------------------------------------------------------------------------
					System.out.println("     ---> Done\n\nProcessed " + queryImages.size() + " images.\n\t\tLoad time : " + loadTime + " ms\n");

					System.out.println("     ---> Done");

				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					if (reader != null) {
						try {
							reader.close();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}
			}
		}
	}

	public static void loadRawDBPatches(String dbFilePath, int limitNum, int step) {

		if (isRawDataLoaded) {

			System.out.println("\nWarning: the raw patches have been loaded!\n");

		} else {

			rawDBPatchMap = new HashMap<Integer, Patch>(limitNum);

			BufferedReader reader = null;
			File file = null;

			try {

				System.out.println("\nLoading raw patches...");

				long startTime = System.currentTimeMillis();

				String tempString;

				int lineNumber = 0;

				file = new File(dbFilePath);
				reader = new BufferedReader(new FileReader(file));

				while ((tempString = reader.readLine()) != null) {

					++lineNumber;
					String[] subStrs = tempString.split(":");

					String[] nameStr = subStrs[0].split("-");

					int pid = Integer.parseInt(nameStr[1]);

					rawDBPatchMap.put(pid, new Patch(pid, step * step, subStrs[2].replace("\n", "")));

					// if (lineNumber % (limitNum / 100) == 0) {
					// System.out.println("Inserting " + lineNumber / (limitNum
					// / 100) + "%");
					// }

					if (lineNumber == limitNum) {
						break;
					}
				}

				long insertTime = System.currentTimeMillis() - startTime;

				isRawDataLoaded = true;
				
				// -------------------------------------------------------------------------------------------------------
				System.out.print("     ---> Done\n\nProcessed " + lineNumber + " patches!!\n\t\tLoad time : "
						+ insertTime + " ms\n");

				System.out.println("        ---> Done");

			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		}
	}

	public static void queryByOnePatchWithoutSMC(BufferedReader br, String keyV, String keyR, short lshL, int step, int sigma) {

		System.out.println("\nModel: query by one patch.");

		if (!isQueryImagesLoaded) {

			System.out.println("Warning: Please load the query images first.");
		} else {

			System.out
					.println("First, please indicate the query image range from [1, " + queryImages.size()
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
					} else if (Integer.parseInt(inputStr) > queryImages.size() + 1 || Integer.parseInt(inputStr) < 1) {

						System.out.println("Warning: the id should be limited in [1, " + queryImages.size() + "], please try again!");

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

			QueryImage qi = queryImages.get(queryImageIndex - 1);
			
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

				HashMap<Integer, Integer> searchResult = cashIndex.searchByOnePatch(lshVector, keyV, keyR);

				if (searchResult != null && searchResult.size() > 0) {

					// TODO: the 50 is hardcode
					List<Integer> topKId = CashIndex.topKPatches(DenoisingPhaseOneTest.TOP_K_PATCH, searchResult);

					System.out.println("\n\nThe search results are: top-" + topKId.size() + "\npid - occurrence");

					for (Integer id : topKId) {

						System.out.println(" " + id + " - " + searchResult.get(id));
					}
					
					System.out.println("\nDone!");
				} else {

					System.out.println("No similar item!!!");
				}
			}
		}
	}
	

	public static void queryByOnePatchWithSMC(BufferedReader br, String keyV, String keyR, short lshL, int step, int sigma) {

		System.out.println("\nModel: query by one patch.");

		if (!isQueryImagesLoaded || !isRawDataLoaded) {

			System.out.println("Warning: Please load the query images or raw data first.");
		} else {

			System.out.println("First, please indicate the query image range from [1, " + queryImages.size()
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
					} else if (Integer.parseInt(inputStr) > queryImages.size() + 1 || Integer.parseInt(inputStr) < 1) {

						System.out.println("Warning: the id should be limited in [1, " + queryImages.size() + "], please try again!");

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

			QueryImage qi = queryImages.get(queryImageIndex - 1);
			
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

				HashMap<Integer, Integer> searchResult = cashIndex.searchByOnePatch(lshVector, keyV, keyR);

				if (searchResult != null && searchResult.size() > 0) {
					
					// TODO: is 100 too big?
					List<Integer> topKId = CashIndex.topKPatches(DenoisingPhaseOneTest.NUM_OF_PRE_LOADED_PATCH, searchResult);

					System.out.println("\n\nThe search results are: top-" + topKId.size() + "\npid - occurrence - dist^2");

					int numOfGood = 0;
					int numOfBad = 0;

					for (Integer id : topKId) {

						int dist = Tools.computeEuclideanDist(rawDBPatchMap.get(id).getPixels(),	queryPatch.getPixels());
						
						if (dist <= threshold) {

							System.out.println(" " + id + " - " + searchResult.get(id) + " - " + dist);
							
							
							if (++numOfGood >= DenoisingPhaseOneTest.TOP_K_PATCH) {
								break;
							}
						} else {
							++numOfBad;
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

		if (!isQueryImagesLoaded || !isRawDataLoaded) {

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
			
			int numOfImages = queryImages.size();

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
				
				QueryImage qi = queryImages.get(queryIndex - 1);
				
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
					
					OneImageQueryWithoutSMCThread t = new OneImageQueryWithoutSMCThread("Thread-" + (i + 1), threadCounter, lshL, keyV, keyR, topK, queryPatchesInThread, cashIndex, rawDBPatchMap, resultInOneThread);
					
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
				
				prepareToRecoverImage(qi.getName(), patches, topK, step, overlap, sigma, k, queryImagePath, oriImagePath, outputPath, isShowImage);
				
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
		
		if (isShowImage) {
			Imshow im1 = new Imshow("The original image");
			im1.showImage(oriImageMat);
			
			Imshow im2 = new Imshow("The query image");
			im2.showImage(queryImageMat);
			
			Imshow im3 = new Imshow("The recovered image");
			im3.showImage(newImageMat);
		}
		
		double psnr1 = Tools.psnr(oriImageMat, newImageMat);
		
		double psnr2 = Tools.psnr(oriImageMat, queryImageMat);
		
		System.out.println("System parameters:\ntopK = " + topK + "\nsigma = " + sigma + "\nthreshold = " + threshold + "\nk = " + k);
		
		System.out.println("\nFor the query image " + queryImageName);
		
		System.out.println("\nPSNR between original and new = " + psnr1);
		System.out.println("\nPSNR between original and query = " + psnr2);
		
		System.out.println("Done.\n");
	}

	public static void queryByOneImageWithSMC(BufferedReader br, String keyV, String keyR, short lshL, int step, Integer overlap, int sigma, double k, String queryImagePath, String oriImagePath, String outputPath, int numOfThread, boolean isShowImage) {

		System.out.println("\nModel: query by one image with smc.");

		if (!isQueryImagesLoaded || !isRawDataLoaded) {

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
			
			int numOfImages = queryImages.size();

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
				
				QueryImage qi = queryImages.get(queryIndex - 1);
				
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
					
					OneImageQueryWithSMCThread t = new OneImageQueryWithSMCThread("Thread-" + (i + 1), threadCounter, lshL, keyV, keyR, topK, threshold, queryPatchesInThread, cashIndex, rawDBPatchMap, resultInOneThread);
					
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
				
				prepareToRecoverImage(qi.getName(), patches, topK, step, overlap, sigma, k, queryImagePath, oriImagePath, outputPath, isShowImage);
				
			}
		}
	}

	public static void main(String args[]) {
		
		if (args.length < 1) {
			
			System.err.println("Error: no argument provided!");
			
			System.exit(Constant.ERROR_ARGUMENTS);
		}

		// load OpenCV library
		System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
		
		ConfigParser config = new ConfigParser(args[0]);
		
		// initialization
		String rootPath = config.getString("rootPath").replace("\\", "/");
		
		short lshL = (short)config.getInt("lshL");
		
		int step = config.getInt("step");
		
		int overlap = config.getInt("overlap");
		
		int sigma = config.getInt("sigma");
		
		int numOfThread = config.getInt("numOfThread");
		
		double k = Double.parseDouble(config.getString("k"));
		
		String dbFilePath = rootPath + config.getString("dbFilePath") + "patchDB-" + step + "-" + overlap + ".txt";
		
		String queryFilePath = rootPath + config.getString("queryFilePath") + step + "-" + overlap + "/" + sigma + "/";
		
		String queryImagePath = rootPath + config.getString("queryImagePath") + sigma + "/";
		
		String oriImagePath = rootPath + config.getString("oriImagePath");
		
		String outputPath = rootPath + config.getString("outputPath") + step + "-" + overlap + "/" + sigma + "/";
		
		int limitNum = config.getInt("limitNum");
		
		String keyV = config.getString("keyV");
		
		String keyR = config.getString("keyR");
		
		boolean isShowImage = config.getBool("isShowImage");

		// Step 1: start building secure index
		buildIndex(dbFilePath, lshL, limitNum, keyV, keyR);

		// Step 2: show a menu
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		boolean rootFlag = true;

		while (rootFlag) {
			
			System.out.print("\n\n----------------------- Root Menu -----------------------\n"
					+ "Please select an operation:\n" 
					+ "[1] load query images;\n" 
					+ "[2] load raw patches;\n"
					+ "[3] query test by one patch (thread num = L) without smc;\n"
					+ "[4] query test by one patch (thread num = L) with smc;\n"
					+ "[5] simulate query test by one image (thread num = L) without smc;\n" 
					+ "[6] simulate query test by one image (thread num = L) with smc;\n" 
					+ "[QUIT] quit system.\n\n" + "--->");
			
			String inputStr;
			int operationType;
			try {
				inputStr = br.readLine();

				try {
					if (inputStr == null || inputStr.toLowerCase().equals("quit")
							|| inputStr.toLowerCase().equals("q")) {

						System.out.println("Quit!");

						break;
					} else if (Integer.parseInt(inputStr) > 6 || Integer.parseInt(inputStr) < 1) {

						System.out.println("Warning: operation type should be limited in [1, 6], please try again!");

						continue;
					} else {
						operationType = Integer.parseInt(inputStr);
					}
				} catch (NumberFormatException e) {
					System.out.println("Warning: operation type should be limited in [1, 6], please try again!");
					continue;
				}

			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}

			// Step 3: operate as indicated
			
			switch (operationType) {

			case Constant.OPERATION_LOAD_QUERY_IMAGE:
				loadQuery(queryFilePath, lshL, step);
				break;
			case Constant.OPERATION_LOAD_RAW_PATCH:
				loadRawDBPatches(dbFilePath, limitNum, step);
				break;
			case Constant.OPERATION_QUERY_TEST_BY_PATCH_WITHOUT_SMC:
				queryByOnePatchWithoutSMC(br, keyV, keyR, lshL, step, sigma);
				break;
			case Constant.OPERATION_QUERY_TEST_BY_PATCH_WITH_SMC:
				queryByOnePatchWithSMC(br, keyV, keyR, lshL, step, sigma);
				break;
			case Constant.OPERATION_QUERY_TEST_BY_IMAGE_WITHOUT_SMC:
				queryByOneImageWithoutSMC(br, keyV, keyR, lshL, step, overlap, sigma, k, queryImagePath, oriImagePath, outputPath, numOfThread, isShowImage);
				break;
			case Constant.OPERATION_QUERY_TEST_BY_IMAGE_WITH_SMC:
				queryByOneImageWithSMC(br, keyV, keyR, lshL, step, overlap, sigma, k, queryImagePath, oriImagePath, outputPath, numOfThread, isShowImage);
				break;
			}
		}
		try {
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
