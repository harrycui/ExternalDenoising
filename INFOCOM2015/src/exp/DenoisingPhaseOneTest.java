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

	public static boolean loadQueryImages = false;
	
	public static boolean loadRawData = false;

	public static CashIndex cashIndex;
	
	public static List<Image> queryImages;

	public static Map<Integer, Patch> rawPatchMap;

	public static void initializeDB(String dbFilePath, short lshL, int limitNum, String keyV,
			String keyR) {

		cashIndex = new CashIndex(limitNum * lshL, lshL);

		//rawPatchMap = new HashMap<Integer, Patch>(limitNum);

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

				//rawPatchMap.put(pid, new Patch(pid, 64, subStrs[2].replace("\n", "")));

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

		if (loadQueryImages) {
		
			System.out.println("\nWarning: the query images have been loaded!\n");
			
		} else {

			queryImages = new ArrayList<Image>();
			
			// TODO: hardcode the number of patches in each image 1887
			final int numOfPatchesInOneImage = 1887;

			BufferedReader reader = null;
			File file = null;

			try {

				System.out.println("\nLoading query images...");
				
				loadQueryImages = true;

				long startTime = System.currentTimeMillis();

				String tempString;

				int lineNumber = 0;
				
				int iid = 1;

				file = new File(queryFilePath);
				reader = new BufferedReader(new FileReader(file));
				
				List<PatchWithLSH> tempPatches = new ArrayList<PatchWithLSH>(numOfPatchesInOneImage);

				while ((tempString = reader.readLine()) != null) {

					++lineNumber;
					String[] subStrs = tempString.split(":");

					String[] nameStr = subStrs[0].split("-");

					int pid = Integer.parseInt(nameStr[1]);
					
					tempPatches.add(new PatchWithLSH(pid, 64, subStrs[2], subStrs[1], lshL));

					if (lineNumber % numOfPatchesInOneImage == 0) {
						
						Image queryImage = new Image(nameStr[0], iid++, tempPatches);
						
						queryImages.add(queryImage);
						
						tempPatches = null;
						
						tempPatches = new ArrayList<PatchWithLSH>(numOfPatchesInOneImage);
					}
				}

				long insertTime = System.currentTimeMillis() - startTime;

				// -------------------------------------------------------------------------------------------------------
				System.out.print("     ---> Done\n\nProcessed " + (iid - 1) + " images with " + lineNumber + " patches!!\n\t\tLoad time : " + insertTime
						+ " ms\n");

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

		if (loadRawData) {
			
			System.out.println("\nWarning: the raw patches have been loaded!\n");
			
		} else {

			rawPatchMap = new HashMap<Integer, Patch>(limitNum);

			BufferedReader reader = null;
			File file = null;

			try {

				System.out.println("\nLoading raw patches...");

				loadRawData = true;

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
					+ "Please select an operation:\n" + 
					"[1] load query images;\n" +
					"[2] load raw patches;\n" +
					"[3] query test by one patch (thread num = L);\n" +
					"[4] query test by one image (thread num = L);\n" +
					//"[5] query test (thread num = ?);\n" +
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
			}

			/*if (operationType == Constant.OPERATION_QUERY) {

				System.out.println("\nModel: query point.");

				if (!loadQueryImages) {

					System.out.println("Please load the query file list first");
				} else {

					while (true) {

						System.out.println("Now, you can search by input you query index range from [1, " + queryImages.size()
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
							} else if (Integer.parseInt(queryStr) > queryImages.size() || Integer.parseInt(queryStr) <= 0) {

								System.out.println("Warning: query index should be limited in [1, queryLimit]");

								continue;
							} else {
								queryIndex = Integer.parseInt(queryStr);

								System.out.println("For query lsh vector index: " + queryIndex);
							}
						} catch (NumberFormatException e) {
							System.out.println("Warning: query index should be limited in [1, queryLimit]");
							continue;
						}
					}
				}
			} else if (operationType == Constant.OPERATION_QUERY_USER_DEFINED) {

				System.out.println("Model: test query by user defined thread number.");
				try {
					System.out.println("Please indicate the number of thread:");

					Scanner scan = new Scanner(System.in);
					int threadNum = scan.nextInt();

					System.out.println("The number of thread is : " + threadNum);

					while (true) {

						System.out.println("\nNow, you can search by input you query index range from [1, " + queryImages.size()
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
							} else if (Integer.parseInt(queryStr) > queryImages.size() || Integer.parseInt(queryStr) <= 0) {

								System.out.println("Warning: query index should be limited in [1, queryLimit]");

								continue;
							} else {
								queryIndex = Integer.parseInt(queryStr);

								System.out.println("For query lsh vector index: " + queryIndex);
							}
						} catch (NumberFormatException e) {
							System.out.println("Warning: query index should be limited in [1, queryLimit]");
							continue;
						}
					}

				} catch (InputMismatchException ime) {
					// ime.printStackTrace();
					System.out.println("Error: please input a float value!");
				}
			} else if (operationType == Constant.OPERATION_DYNAMIC_ADD) {

			} else if (operationType == Constant.OPERATION_DYNAMIC_DELETE) {

			}*/
		}
		try {
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
