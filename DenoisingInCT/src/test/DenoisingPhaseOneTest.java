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

import base.Constant;
import base.LSHVector;
import base.Patch;
import base.PatchWithLSH;
import base.QueryImage;
import index.SecureCashIndex;
import util.CTTools;
import util.ConfigParser;
import util.FileUtil;

/**
 *
 * This version is based on the INFOCOM-2015 testing. I made this for the paper about Image External Denoising.
 *
 * Created by HarryC on 7-Oct-2015.
 */
public class DenoisingPhaseOneTest {
	
	public static final int TOP_K_PATCH = 60;

	public static boolean isQueryImagesLoaded = false;

	public static boolean isRawDataLoaded = false;

	public static SecureCashIndex cashIndex;

	public static List<QueryImage> queryImages;

	// used for simulated evaluation
	public static Map<Integer, Patch> rawDBPatchMap;

	public static void buildIndex(String dbFilePath, short lshL, int limitNum, String keyV, String keyR) {

		cashIndex = new SecureCashIndex(limitNum * lshL, lshL);

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
		
		boolean isShowTime = config.getBool("isShowTime");

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
					+ "[7] batch simulate query test by one image (thread num = L) without smc;\n" 
					+ "[8] batch simulate query test by one image (thread num = L) with smc;\n"
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
					} else if (Integer.parseInt(inputStr) > 8 || Integer.parseInt(inputStr) < 1) {

						System.out.println("Warning: operation type should be limited in [1, 8], please try again!");

						continue;
					} else {
						operationType = Integer.parseInt(inputStr);
					}
				} catch (NumberFormatException e) {
					System.out.println("Warning: operation type should be limited in [1, 8], please try again!");
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
				CTTools.queryByOnePatchWithoutSMC(br, keyV, keyR, lshL, step, sigma, isShowTime);
				break;
			case Constant.OPERATION_QUERY_TEST_BY_PATCH_WITH_SMC:
				CTTools.queryByOnePatchWithSMC(br, keyV, keyR, lshL, step, sigma, isShowTime);
				break;
			case Constant.OPERATION_QUERY_TEST_BY_IMAGE_WITHOUT_SMC:
				CTTools.queryByOneImageWithoutSMC(br, keyV, keyR, lshL, step, overlap, sigma, k, queryImagePath, oriImagePath, outputPath, numOfThread, isShowImage, isShowTime);
				break;
			case Constant.OPERATION_QUERY_TEST_BY_IMAGE_WITH_SMC:
				CTTools.queryByOneImageWithSMC(br, keyV, keyR, lshL, step, overlap, sigma, k, queryImagePath, oriImagePath, outputPath, numOfThread, isShowImage, isShowTime);
				break;
			case Constant.OPERATION_BATCH_QUERY_TEST_BY_IMAGE_WITHOUT_SMC:
				CTTools.queryByOneImageWithoutSMCBatch(br, keyV, keyR, lshL, step, overlap, sigma, k, queryImagePath, oriImagePath, outputPath, numOfThread, isShowImage, isShowTime);
				break;
			case Constant.OPERATION_BATCH_QUERY_TEST_BY_IMAGE_WITH_SMC:
				CTTools.queryByOneImageWithSMCBatch(br, keyV, keyR, lshL, step, overlap, sigma, k, queryImagePath, oriImagePath, outputPath, numOfThread, isShowImage, isShowTime);
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
