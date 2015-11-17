package exp;

import base.*;
import index.CashIndex;
import tool.PRF;
import tool.TimeUtil;

import java.io.*;
import java.util.*;

/**
 *
 * This version is based on the TMC testing. I made this for the paper about
 * Image Denoising.
 *
 * Created by HarryC on 7-Oct-2015.
 */
public class DenoisingPhaseOneTest {

	public static final int PATCH_NUM_IN_ONE_IMAGE = 1887;
	
	public static final int IMAGE_HEIGHT = 360;
	
	public static final int IMAGE_WIDTH = 260;
	
	public static final int IMAGE_STEP = 8;
	
	public static final int IMAGE_OVERLAP = 1;

	public static boolean isQueryImagesLoaded = false;

	public static boolean isRawDataLoaded = false;

	public static CashIndex cashIndex;

	public static List<QueryImage> queryImages;

	public static Map<Integer, Patch> rawPatchMap;

	public static void initializeDB(String dbFilePath, short lshL, int limitNum, String keyV, String keyR) {

		cashIndex = new CashIndex(limitNum * lshL, lshL);

		// rawPatchMap = new HashMap<Integer, Patch>(limitNum);

		BufferedReader reader = null;
		File file = null;

		try {

			System.out.println("\nLoading db lsh file...");

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

				LSHVector lshVector = new LSHVector(pid, subStrs[1].replace("\n", ""), lshL);

				cashIndex.insert(lshVector, nameStr[0], pid, keyV, keyR);

				// rawPatchMap.put(pid, new Patch(pid, 64,
				// subStrs[2].replace("\n", "")));

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

	public static void initializeQuery(String queryFilePath, short lshL) {

		if (isQueryImagesLoaded) {

			System.out.println("\nWarning: the query images have been loaded!\n");

		} else {

			queryImages = new ArrayList<QueryImage>();

			BufferedReader reader = null;
			File file = null;

			try {

				System.out.println("\nLoading query images...");

				isQueryImagesLoaded = true;

				long startTime = System.currentTimeMillis();

				String tempString;

				int lineNumber = 0;

				int iid = 1;

				file = new File(queryFilePath);
				reader = new BufferedReader(new FileReader(file));

				List<PatchWithLSH> tempPatches = new ArrayList<PatchWithLSH>(PATCH_NUM_IN_ONE_IMAGE);

				while ((tempString = reader.readLine()) != null) {

					++lineNumber;
					String[] subStrs = tempString.split(":");

					String[] nameStr = subStrs[0].split("-");

					int pid = Integer.parseInt(nameStr[1]);

					tempPatches.add(new PatchWithLSH(pid, 64, subStrs[2], subStrs[1], lshL));

					if (lineNumber % PATCH_NUM_IN_ONE_IMAGE == 0) {

						QueryImage queryImage = new QueryImage(nameStr[0], iid++, tempPatches);

						queryImages.add(queryImage);

						tempPatches = null;

						tempPatches = new ArrayList<PatchWithLSH>(PATCH_NUM_IN_ONE_IMAGE);
					}
				}

				long insertTime = System.currentTimeMillis() - startTime;

				// -------------------------------------------------------------------------------------------------------
				System.out.print("     ---> Done\n\nProcessed " + (iid - 1) + " images with " + lineNumber
						+ " patches!!\n\t\tLoad time : " + insertTime + " ms\n");

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

	public static void loadRawPatches(String dbFilePath, int limitNum) {

		if (isRawDataLoaded) {

			System.out.println("\nWarning: the raw patches have been loaded!\n");

		} else {

			rawPatchMap = new HashMap<Integer, Patch>(limitNum);

			BufferedReader reader = null;
			File file = null;

			try {

				System.out.println("\nLoading raw patches...");

				isRawDataLoaded = true;

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

					rawPatchMap.put(pid, new Patch(pid, 64, subStrs[2].replace("\n", "")));

					// if (lineNumber % (limitNum / 100) == 0) {
					// System.out.println("Inserting " + lineNumber / (limitNum
					// / 100) + "%");
					// }

					if (lineNumber == limitNum) {
						break;
					}
				}

				long insertTime = System.currentTimeMillis() - startTime;

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

	public static void queryByOnePatch(BufferedReader br, String keyV, String keyR, short lshL) {

		System.out.println("\nModel: query by one patch.");

		if (!isQueryImagesLoaded) {

			System.out.println("Warning: Please load the query images first.");
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

			int numOfPatches = queryImages.size() * PATCH_NUM_IN_ONE_IMAGE;

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
					} else if (Integer.parseInt(queryStr) > numOfPatches || Integer.parseInt(queryStr) <= 0) {

						System.out.println("Warning: query index should be limited in [1, " + numOfPatches + "]");

						continue;
					} else {
						queryIndex = Integer.parseInt(queryStr);

						System.out.println("For query lsh vector index: " + queryIndex);
					}
				} catch (NumberFormatException e) {
					System.out.println("Warning: query index should be limited in [1, " + numOfPatches + "]");
					continue;
				}

				// this index starts from 0.
				int imageIndex = (queryIndex - 1) / PATCH_NUM_IN_ONE_IMAGE;

				// from 0
				int patchIndex = (queryIndex - 1) % PATCH_NUM_IN_ONE_IMAGE;

				PatchWithLSH queryPatch = queryImages.get(imageIndex).getPatchByPatchIndex(patchIndex);

				LSHVector lshVector = new LSHVector(1, queryPatch.getLshValues(), lshL);

				HashMap<Integer, Integer> searchResult = cashIndex.searchByOnePatch(lshVector, keyV, keyR);

				if (searchResult != null && searchResult.size() > 0) {

					if (isRawDataLoaded) {

						// TODO: is 100 too big?
						List<Integer> topKId = CashIndex.topKPatches(100, searchResult);

						System.out.println("\n\nThe search results are: top-" + topKId.size() + "\npid - occurrence - dist^2");

						int numOfGood = 0;
						int numOfBad = 0;

						for (Integer id : topKId) {

							int dist = computeEuclideanDist(rawPatchMap.get(id).getPixels(),	queryPatch.getPixels());
							if (dist <= threshold) {

								System.out.println(" " + id + " - " + searchResult.get(id) + " - " + dist);
								
								if (++numOfGood >= 50) {
									break;
								}
							} else {
								++numOfBad;
							}
						}
						
						System.out.println("To find " + numOfGood + " good patches, there are " + numOfBad + " bad patches.");
						
					} else {

						List<Integer> topKId = CashIndex.topKPatches(50, searchResult);

						System.out.println("\n\nThe search results are: top-" + topKId.size() + "\npid - occurrence");

						for (Integer id : topKId) {

							System.out.println(" " + id + " - " + searchResult.get(id));
						}
					}

					System.out.println("\nDone!");
				} else {

					System.out.println("No similar item!!!");
				}
			}
		}
	}

	public static void queryByOneImage(BufferedReader br, String keyV, String keyR, short lshL) {

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
								lshSimilarPatches.getPatches().add(rawPatchMap.get(id));
								numOfLSHPatches++;
							}

							int dist = computeEuclideanDist(rawPatchMap.get(id).getPixels(), patchWithLSH.getPixels());
							if (dist <= threshold) {

								if (++numOfGood >= 50) {
									break;
								}
								
								smcSimilarPatches.getPatches().add(rawPatchMap.get(id));
								
							} else {
								++numOfBad;
							}
						}
					}
					
					lshPatches.add(lshSimilarPatches);
					smcPatches.add(smcSimilarPatches);
				}
				
				//System.out.println(lshPatches.size() + " " + smcPatches.size());
				
				RecoverImage imageByLSH = new RecoverImage(DenoisingPhaseOneTest.PATCH_NUM_IN_ONE_IMAGE, DenoisingPhaseOneTest.IMAGE_HEIGHT, DenoisingPhaseOneTest.IMAGE_WIDTH, DenoisingPhaseOneTest.IMAGE_STEP, DenoisingPhaseOneTest.IMAGE_OVERLAP, lshPatches);
				RecoverImage imageBySMC = new RecoverImage(DenoisingPhaseOneTest.PATCH_NUM_IN_ONE_IMAGE, DenoisingPhaseOneTest.IMAGE_HEIGHT, DenoisingPhaseOneTest.IMAGE_WIDTH, DenoisingPhaseOneTest.IMAGE_STEP, DenoisingPhaseOneTest.IMAGE_OVERLAP, smcPatches);
				
				System.out.println("Two images are writen to files under " + outputPath);
			}
		}
	}

	
	public static void main(String args[]) {

		// initialization
		String dbFilePath = null;
		String queryFilePath = null;

		short lshL = 0;

		int limitNum = 1000000;

		if (args.length < 4) {
			System.err.println(
					"Error: arguments are not enough! Please follow the format:\n\t[db file path] [query file path] [lshL] [limit]");

			System.exit(Constant.ERROR_ARGUMENTS);
		} else {

			dbFilePath = args[0];
			queryFilePath = args[1];

			lshL = Short.parseShort(args[2]);
			limitNum = Integer.parseInt(args[3]);
			// threadNum = Integer.parseInt(args[4]);
		}

		String keyV = "harry";
		String keyR = "cityu";

		initializeDB(dbFilePath, lshL, limitNum, keyV, keyR);

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		boolean rootFlag = true;

		while (rootFlag) {
			System.out.print("\n\n----------------------- Root Menu -----------------------\n"
					+ "Please select an operation:\n" + "[1] load query images;\n" + "[2] load raw patches;\n"
					+ "[3] query test by one patch (thread num = L);\n"
					+ "[4] query test by one image (thread num = L);\n" +
					// "[5] query test (thread num = ?);\n" +
					"[QUIT] quit system.\n\n" + "--->");
			String inputStr;
			int operationType;
			try {
				inputStr = br.readLine();

				try {
					if (inputStr == null || inputStr.toLowerCase().equals("quit")
							|| inputStr.toLowerCase().equals("q")) {

						System.out.println("Quit!");

						break;
					} else if (Integer.parseInt(inputStr) > 4 || Integer.parseInt(inputStr) < 1) {

						System.out.println("Warning: operation type should be limited in [1, 4], please try again!");

						continue;
					} else {
						operationType = Integer.parseInt(inputStr);
					}
				} catch (NumberFormatException e) {
					System.out.println("Warning: operation type should be limited in [1, 4], please try again!");
					continue;
				}

			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}

			switch (operationType) {

			case Constant.OPERATION_LOAD_QUERY_IMAGE:
				initializeQuery(queryFilePath, lshL);
				break;
			case Constant.OPERATION_LOAD_RAW_PATCH:
				loadRawPatches(dbFilePath, limitNum);
				break;
			case Constant.OPERATION_QUERY_TEST_BY_PATCH:
				queryByOnePatch(br, keyV, keyR, lshL);
				break;
			case Constant.OPERATION_QUERY_TEST_BY_IMAGE:
				queryByOneImage(br, keyV, keyR, lshL);
				break;
			}
		}
		try {
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static int computeEuclideanDist(int[] v1, int[] v2) {

		assert (v1.length == v2.length);

		int dist = 0;

		for (int i = 0; i < v1.length; ++i) {
			dist += (v1[i] - v2[i]) * (v1[i] - v2[i]);
		}

		return dist;
	}
}
