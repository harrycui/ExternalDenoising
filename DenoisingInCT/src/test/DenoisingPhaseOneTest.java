package test;

import base.*;
import index.CashIndex;
import util.ConfigParser;
import util.FileUtil;
import util.Tools;

import java.io.*;
import java.util.*;

import org.opencv.core.Core;

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

	public static void loadQuery(String queryFilePath, short lshL) {

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

							tempPatches.add(new PatchWithLSH(pid, 64, subStrs[2], subStrs[1], lshL));
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

	public static void loadRawDBPatches(String dbFilePath, int limitNum) {

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

					rawDBPatchMap.put(pid, new Patch(pid, 64, subStrs[2].replace("\n", "")));

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
					.println("First, please indicate the query image: (-1 means return to root menu)");

			int queryImageIndex = 0;
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

			while (true) {

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

			System.out
					.println("First, please indicate the query image: (-1 means return to root menu)");

			int queryImageIndex = 0;
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

			while (true) {

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

	public static void queryByOneImageWithoutSMC(BufferedReader br, String keyV, String keyR, short lshL, int step, int sigma, String outputPath) {

		System.out.println("\nModel: query by one image.");

		if (!isQueryImagesLoaded) {

			System.out.println("Warning: Please load the query images first.");
		} else {

			System.out.println("First, please indicate the top-k number: (-1 means return to root menu)");

			int topK = 0;
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

						break;
					} else if (Integer.parseInt(inputStr) > queryImages.size() + 1 || Integer.parseInt(inputStr) < 1) {

						System.out.println("Warning: the id should be limited in [1, " + queryImages.size()
								+ "], please try again!");

						continue;
					} else {
						topK = Integer.parseInt(inputStr);

						System.out.println("The top-k number is : " + topK);

						break;
					}
				} catch (NumberFormatException e) {
					System.out.println("Warning: please input again.");
					continue;
				}
			}

			int numOfImages = queryImages.size();

			while (true) {

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
					} else if (Integer.parseInt(queryStr) > numOfImages || Integer.parseInt(queryStr) <= 0) {

						System.out.println("Warning: query index should be limited in [1, " + numOfImages + "]");

						continue;
					} else {
						queryIndex = Integer.parseInt(queryStr);

						System.out.println("The query image index is : " + queryIndex);
					}
				} catch (NumberFormatException e) {
					System.out.println("Warning: query index should be limited in [1, " + numOfImages + "]");
					continue;
				}
				
				List<SimilarPatches> lshPatches = new ArrayList<SimilarPatches>(DenoisingPhaseOneTest.PATCH_NUM_IN_ONE_IMAGE);

				List<SimilarPatches> smcPatches = new ArrayList<SimilarPatches>(DenoisingPhaseOneTest.PATCH_NUM_IN_ONE_IMAGE);

				for (PatchWithLSH patchWithLSH: queryImages.get(queryIndex - 1).getPatches()) {
					
					LSHVector lshVector = new LSHVector(1, patchWithLSH.getLshValues(), lshL);
					
					SimilarPatches lshSimilarPatches = new SimilarPatches(patchWithLSH.getPid(), patchWithLSH.convert2Patch(), new ArrayList<Patch>());
					
					SimilarPatches smcSimilarPatches = new SimilarPatches(patchWithLSH.getPid(), patchWithLSH.convert2Patch(), new ArrayList<Patch>());

					HashMap<Integer, Integer> searchResult = cashIndex.searchByOnePatch(lshVector, keyV, keyR);

					if (searchResult != null && searchResult.size() > 0) {

						// TODO: is 100 too big?
						List<Integer> topKId = CashIndex.topKPatches(100, searchResult);

						int numOfGood = 0;
						int numOfBad = 0;
						int numOfLSHPatches = 0;

						for (Integer id : topKId) {
							
							if (numOfLSHPatches <= 50) {
								lshSimilarPatches.getPatches().add(rawDBPatchMap.get(id));
								numOfLSHPatches++;
							}

							int dist = Tools.computeEuclideanDist(rawDBPatchMap.get(id).getPixels(), patchWithLSH.getPixels());
							if (dist <= threshold) {

								if (++numOfGood >= 50) {
									break;
								}
								
								smcSimilarPatches.getPatches().add(rawDBPatchMap.get(id));
								
							} else {
								++numOfBad;
							}
						}
					}
					
					lshPatches.add(lshSimilarPatches);
					smcPatches.add(smcSimilarPatches);
				}
				
				//System.out.println(lshPatches.size() + " " + smcPatches.size());
				
				RecoverImage imageByLSH = new RecoverImage(queryImages.get(queryIndex - 1).getName(), DenoisingPhaseOneTest.PATCH_NUM_IN_ONE_IMAGE, DenoisingPhaseOneTest.IMAGE_HEIGHT, DenoisingPhaseOneTest.IMAGE_WIDTH, DenoisingPhaseOneTest.PATCH_WIDTH, DenoisingPhaseOneTest.IMAGE_STEP, DenoisingPhaseOneTest.IMAGE_OVERLAP, lshPatches);
				RecoverImage imageBySMC = new RecoverImage(queryImages.get(queryIndex - 1).getName(), DenoisingPhaseOneTest.PATCH_NUM_IN_ONE_IMAGE, DenoisingPhaseOneTest.IMAGE_HEIGHT, DenoisingPhaseOneTest.IMAGE_WIDTH, DenoisingPhaseOneTest.PATCH_WIDTH, DenoisingPhaseOneTest.IMAGE_STEP, DenoisingPhaseOneTest.IMAGE_OVERLAP, smcPatches);
				
				System.out.println("Two images are writen to files under " + outputPath);
			}
		}
	}

	public static void queryByOneImageWithSMC(BufferedReader br, String keyV, String keyR, short lshL) {

		System.out.println("\nModel: query by one image.");

		if (!isQueryImagesLoaded || !isRawDataLoaded) {

			System.out.println("Warning: Please load the query images or raw data first.");
		} else {

			System.out
					.println("First, please indicate the threshold (dist^2) of this patch: (-1 means return to root menu)");

			int threshold = 0;
			while (true) {
				
				String input = null;

				try {
					input = br.readLine();
				} catch (IOException e) {
					e.printStackTrace();
				}

				try {
					if (input == null || input.equals("-1")) {

						System.out.println("Return to root menu!");

						break;
					} else {
						threshold = Integer.parseInt(input);

						System.out.println("The threshold is indicated as : " + threshold);
						
						break;
					}
				} catch (NumberFormatException e) {
					System.out.println("Warning: please input again.");
					continue;
				}
			}
			
			System.out.println(
					"Second, please indicate the output path: (-1 means return to root menu)");

			String outputPath = null;
			while (true) {

				try {
					outputPath = br.readLine();
				} catch (IOException e) {
					e.printStackTrace();
				}

				try {
					if (outputPath == null || outputPath.equals("-1")) {

						System.out.println("Return to root menu!");

						break;
					} else {

						System.out.println("The output path is : " + outputPath);
						
						break;
					}
				} catch (NumberFormatException e) {
					System.out.println("Warning: please input again.");
					continue;
				}
			}

			int numOfImages = queryImages.size();

			while (true) {

				System.out.println("\nNow, you can search by input you query patch index range from [1, " + numOfImages
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
					} else if (Integer.parseInt(queryStr) > numOfImages || Integer.parseInt(queryStr) <= 0) {

						System.out.println("Warning: query index should be limited in [1, " + numOfImages + "]");

						continue;
					} else {
						queryIndex = Integer.parseInt(queryStr);

						System.out.println("For query lsh vector index: " + queryIndex);
					}
				} catch (NumberFormatException e) {
					System.out.println("Warning: query index should be limited in [1, " + numOfImages + "]");
					continue;
				}
				
				List<SimilarPatches> lshPatches = new ArrayList<SimilarPatches>(DenoisingPhaseOneTest.PATCH_NUM_IN_ONE_IMAGE);

				List<SimilarPatches> smcPatches = new ArrayList<SimilarPatches>(DenoisingPhaseOneTest.PATCH_NUM_IN_ONE_IMAGE);

				for (PatchWithLSH patchWithLSH: queryImages.get(queryIndex - 1).getPatches()) {
					
					LSHVector lshVector = new LSHVector(1, patchWithLSH.getLshValues(), lshL);
					
					SimilarPatches lshSimilarPatches = new SimilarPatches(patchWithLSH.getPid(), patchWithLSH.convert2Patch(), new ArrayList<Patch>());
					
					SimilarPatches smcSimilarPatches = new SimilarPatches(patchWithLSH.getPid(), patchWithLSH.convert2Patch(), new ArrayList<Patch>());

					HashMap<Integer, Integer> searchResult = cashIndex.searchByOnePatch(lshVector, keyV, keyR);

					if (searchResult != null && searchResult.size() > 0) {

						// TODO: is 100 too big?
						List<Integer> topKId = CashIndex.topKPatches(100, searchResult);

						int numOfGood = 0;
						int numOfBad = 0;
						int numOfLSHPatches = 0;

						for (Integer id : topKId) {
							
							if (numOfLSHPatches <= 50) {
								lshSimilarPatches.getPatches().add(rawDBPatchMap.get(id));
								numOfLSHPatches++;
							}

							int dist = Tools.computeEuclideanDist(rawDBPatchMap.get(id).getPixels(), patchWithLSH.getPixels());
							if (dist <= threshold) {

								if (++numOfGood >= 50) {
									break;
								}
								
								smcSimilarPatches.getPatches().add(rawDBPatchMap.get(id));
								
							} else {
								++numOfBad;
							}
						}
					}
					
					lshPatches.add(lshSimilarPatches);
					smcPatches.add(smcSimilarPatches);
				}
				
				//System.out.println(lshPatches.size() + " " + smcPatches.size());
				
				RecoverImage imageByLSH = new RecoverImage(queryImages.get(queryIndex - 1).getName(), DenoisingPhaseOneTest.PATCH_NUM_IN_ONE_IMAGE, DenoisingPhaseOneTest.IMAGE_HEIGHT, DenoisingPhaseOneTest.IMAGE_WIDTH, DenoisingPhaseOneTest.PATCH_WIDTH, DenoisingPhaseOneTest.IMAGE_STEP, DenoisingPhaseOneTest.IMAGE_OVERLAP, lshPatches);
				RecoverImage imageBySMC = new RecoverImage(queryImages.get(queryIndex - 1).getName(), DenoisingPhaseOneTest.PATCH_NUM_IN_ONE_IMAGE, DenoisingPhaseOneTest.IMAGE_HEIGHT, DenoisingPhaseOneTest.IMAGE_WIDTH, DenoisingPhaseOneTest.PATCH_WIDTH, DenoisingPhaseOneTest.IMAGE_STEP, DenoisingPhaseOneTest.IMAGE_OVERLAP, smcPatches);
				
				System.out.println("Two images are writen to files under " + outputPath);
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
		
		String dbFilePath = rootPath + config.getString("dbFilePath");
		
		String queryFilePath = rootPath + config.getString("queryFilePath");
		
		String outputPath = rootPath + config.getString("outputPath");
		
		short lshL = (short)config.getInt("lshL");
		
		// "-1" means unlimited
		int limitNum = config.getInt("limitNum");
		
		String keyV = config.getString("keyV");
		
		String keyR = config.getString("keyR");

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
					+ "[3] query test by one patch (thread num = L);\n"
					+ "[4] simulate query test by one image (thread num = L) without smc;\n" 
					+ "[5] simulate query test by one image (thread num = L) with smc;\n" 
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
					} else if (Integer.parseInt(inputStr) > 5 || Integer.parseInt(inputStr) < 1) {

						System.out.println("Warning: operation type should be limited in [1, 5], please try again!");

						continue;
					} else {
						operationType = Integer.parseInt(inputStr);
					}
				} catch (NumberFormatException e) {
					System.out.println("Warning: operation type should be limited in [1, 5], please try again!");
					continue;
				}

			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}

			// Step 3: operate as indicated
			
			switch (operationType) {

			case Constant.OPERATION_LOAD_QUERY_IMAGE:
				loadQuery(queryFilePath, lshL);
				break;
			case Constant.OPERATION_LOAD_RAW_PATCH:
				loadRawDBPatches(dbFilePath, limitNum);
				break;
			case Constant.OPERATION_QUERY_TEST_BY_PATCH:
				queryByOnePatch(br, keyV, keyR, lshL);
				break;
			case Constant.OPERATION_QUERY_TEST_BY_IMAGE_WITHOUT_SMC:
				queryByOneImageWithoutSMC(br, keyV, keyR, lshL);
				break;
			case Constant.OPERATION_QUERY_TEST_BY_IMAGE_WITH_SMC:
				//queryByOneImage(br, keyV, keyR, lshL);
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
