package exp;

import base.*;
import tool.TimeUtil;

import java.io.*;
import java.util.*;

/**
 *
 * Created by HarryC on 1/7/15.
 */
public class InfocomRawTest {

    public static void main(String args[]) {

        String lshFileName = null;
        String queryFileName = null;

        short L = 0;

        int limit = 1000000;

        String keyV = "harry";
        String keyR = "cityu";

        //int threadNum = 0;

        //String sampleIndexFileName = "./sampleIndex.txt";

        if (args.length < 4) {
            System.err.println("Error: arguments are not enough! Please follow the format:\n\t[lsh file path] [query file path] [L] [limit]");

            System.exit(Constant.ERROR_ARGUMENTS);
        } else {

            lshFileName = args[0];
            queryFileName = args[1];


            L = Short.parseShort(args[2]);
            limit = Integer.parseInt(args[3]);
            //threadNum = Integer.parseInt(args[4]);
        }

        CashIndex2 rawIndex = new CashIndex2(limit * L, L);

        List<String> lshFileList = new ArrayList<String>();

        System.out.println("\nLoad lsh file list.");

        File lshFileListPath = new File(lshFileName);

        BufferedReader br = null;

        try {
            System.out.print("Start reading lsh file list...\n");

            br = new BufferedReader(new FileReader(lshFileListPath));

            String tempString;

            int lshFileNum = 0;

            // read util null
            while ((tempString = br.readLine()) != null) {

                lshFileList.add(tempString.replace("\n", ""));

                ++lshFileNum;
            }

            br.close();

            System.out.println("     ---> Done\n\nLoaded " + lshFileNum + " lsh files!!\n");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }

        System.out.println("Initialize Secure Index           ---> Done");


        BufferedReader reader = null;
        BufferedWriter writer = null;
        File file = null;

        try {
            System.out.print("Start reading files by line...\n");

            long startTime = System.currentTimeMillis();

            String tempString;

            int lineNumber = 0;

            for (int i = 0; i < lshFileList.size(); ++i) {

                file = new File(lshFileList.get(i));
                reader = new BufferedReader(new FileReader(file));

                // read util null
                while ((tempString = reader.readLine()) != null) {

                    ++lineNumber;

                    String[] lshLine = tempString.split(":");

                    LSHVector lshVector = new LSHVector(lineNumber, lshLine[1].replace("\n", ""), L);

                    rawIndex.insert(lshVector, lshLine[0].replace(".feature", ""), lineNumber, keyV, keyR, lshLine[2]);


                    if (lineNumber % (limit / 100) == 0) {
                        System.out.println("Inserting " + lineNumber / (limit / 100) + "%");
                    }

                    if (lineNumber == limit) {
                        break;
                    }
                }

                reader.close();
                file = null;
                reader = null;

                if (lineNumber == limit) {
                    break;
                }
            }

            long insertTime = System.currentTimeMillis() - startTime;

            // -------------------------------------------------------------------------------------------------------
            //System.out.print("     ---> Done\n\nProcessed " + lineNumber + " records!! Total cost " + (insertTime + encryptTime) + " ms\n\t\t\tInsert time " + insertTime + " ms\n\t\t\tEncryption time " + encryptTime + " ms\n\nWriting result into file...");
            System.out.print("     ---> Done\n\nProcessed " + lineNumber + " records!!\n\t\t\tInsert time " + insertTime + " ms\n\nWriting result into file...");

            CashIndex2.featureRankArray = new int[lineNumber + 1];

            writer = new BufferedWriter(new FileWriter("./insertCashIndexResult.txt", true));


            writer.write("-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.\n\nDSSSE Experiment - Building Secure Index:" +
                    "\n\n\tData set: " + lshFileName);

            writer.write("\n\n\t\tInsert time: " + insertTime + " ms" +
                    "\n\n\tSetting:\n\t\tL = " + L +
                    "\n\t\tTotal size: " + limit);


            writer.write("\n\nInsert testing finished at " + TimeUtil.timeToString(Calendar.getInstance(), TimeUtil.TIME_FORMAT_YMD_HMS) + ".\n\n");

            writer.close();

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
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }


        boolean queryFileListLoaded = false;
        int queryLimit = 0;
        List<String> queryFileList = new ArrayList<String>();

        System.out.println("\nModel: load query file list.");

        File queryFile = new File(queryFileName);
        BufferedReader reader2 = null;

        try {
            System.out.print("Start reading query file list by line...\n");

            reader2 = new BufferedReader(new FileReader(queryFile));

            String tempString;

            int lineNumber = 0;

            long startTime = System.currentTimeMillis();
            // read util null
            while ((tempString = reader2.readLine()) != null) {

                queryFileList.add(tempString.replace("\n", ""));

                ++lineNumber;
            }

            long readTime = System.currentTimeMillis() - startTime;

            reader2.close();

            queryFileListLoaded = true;
            queryLimit = queryFileList.size();

            System.out.print("     ---> Done\n\nProcessed " + lineNumber + " records!!\n\t\t\tRead time " + readTime + " ms.");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader2 != null) {
                try {
                    reader2.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }

        br = new BufferedReader(new InputStreamReader(System.in));
        boolean rootFlag = true;

        while (rootFlag) {
            System.out.print("\n\n----------------------- Root Menu -----------------------\n" +
                    "Please select an operation:\n" +
                    "[1]  ...\n" +
                    //"[2]  load query file list;\n" +
                    "[3]  query test (thread num = L);\n" +
                    "[4]  query test (thread num = ?);\n" +
                    "[5]  query test with distance (thread num = L);\n" +
                    "[QUIT] quit system.\n\n" +
                    "--->");
            String inputStr;
            int operationType;
            try {
                inputStr = br.readLine();

                try {
                    if (inputStr == null || inputStr.toLowerCase().equals("quit") || inputStr.toLowerCase().equals("q")) {

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

            if (operationType == Constant.OPERATION_QUERY) {

                System.out.println("\nModel: query point.");

                if (!queryFileListLoaded) {

                    System.out.println("Please load the query file list first");
                } else {

                    while (true) {

                        System.out.println("Now, you can search by input you query index range from [1, " + queryLimit + "]: (-1 means return to root menu)");

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
                            } else if (Integer.parseInt(queryStr) > queryLimit || Integer.parseInt(queryStr) <= 0) {

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

                        QueryFile qf = loadQueryLshVectors(queryFileList.get(queryIndex - 1), L);

                        HashMap<String, Integer> searchResult = rawIndex.search(qf.getLshVectors(), keyV, keyR);

                        if (searchResult != null && searchResult.size() > 0) {

                            System.out.println("\n\nThe search results are: ");

                            // TODO
                            List<String> topKId = CashIndex2.topK(20, searchResult);

                            for (String id : topKId) {

                                System.out.println(" " + id + "-" + searchResult.get(id));
                            }

                            System.out.println("\nDone!");
                        } else {
                            System.out.println("No similar item!!!");
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

                        System.out.println("\nNow, you can search by input you query index range from [1, " + queryLimit + "]: (-1 means return to root menu)");

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
                            } else if (Integer.parseInt(queryStr) > queryLimit || Integer.parseInt(queryStr) <= 0) {

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

                        QueryFile qf = loadQueryLshVectors(queryFileList.get(queryIndex - 1), L);

                        HashMap<String, Integer> searchResult = rawIndex.searchByUserDefinedThread(qf.getLshVectors(), keyV, keyR, threadNum);

                        if (searchResult != null && searchResult.size() > 0) {

                            System.out.println("\n\nThe search results are: ");

                            // TODO
                            List<String> topKId = CashIndex2.topK(20, searchResult);

                            for (String id : topKId) {

                                System.out.println(" " + id + "-" + searchResult.get(id));
                            }

                            System.out.println("\nDone!");
                        } else {
                            System.out.println("No similar item!!!");
                        }
                    }


                } catch (InputMismatchException ime) {
                    //ime.printStackTrace();
                    System.out.println("Error: please input a float value!");
                }
            } else if (operationType == Constant.OPERATION_QUERY_TEST_DIST) {

                System.out.println("\nModel: query test with distance.");

                if (!queryFileListLoaded) {

                    System.out.println("Please load the query file list first");
                } else {

                    while (true) {

                        System.out.println("Now, you can search by input you query index range from [1, " + queryLimit + "]: (-1 means return to root menu)");

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
                            } else if (Integer.parseInt(queryStr) > queryLimit || Integer.parseInt(queryStr) <= 0) {

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

                        QueryFile qf = loadQueryLshVectors(queryFileList.get(queryIndex - 1), L);

                        HashMap<String, Integer> searchResult = rawIndex.searchWithDist(qf, keyV, keyR);

                        if (searchResult != null && searchResult.size() > 0) {

                            System.out.println("\n\nThe search results are: ");

                            // TODO
                            List<String> topKId = CashIndex2.topK(20, searchResult);

                            for (String id : topKId) {

                                System.out.println(" " + id + "-" + searchResult.get(id));
                            }

                            System.out.println("\nDone!");
                        } else {
                            System.out.println("No similar item!!!");
                        }
                    }
                }
            }
        }
        try {
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static QueryFile loadQueryLshVectors(String queryPath, short L) {

        List<LSHVector> queryLshVectors = new ArrayList<LSHVector>();
        List<SiftDescriptor> sifts = new ArrayList<SiftDescriptor>();

        File file = new File(queryPath);
        BufferedReader reader = null;

        try {
            System.out.print("Start reading query LSH vectors by line...\n");

            reader = new BufferedReader(new FileReader(file));

            String tempString;

            int lineNumber = 1;

            long startTime = System.currentTimeMillis();
            // read util null
            while ((tempString = reader.readLine()) != null) {

                String[] lshLine = tempString.split(":");

                LSHVector lshVector = new LSHVector(lineNumber, lshLine[1].replace("\n", ""), L);

                queryLshVectors.add(lshVector);
                sifts.add(new SiftDescriptor(lshLine[2]));

                ++lineNumber;
            }

            long readTime = System.currentTimeMillis() - startTime;

            reader.close();

            System.out.println("     ---> Done\n\nProcessed " + (lineNumber - 1) + " records!!\n\t\t\tRead time " + readTime + " ms.");

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

        return new QueryFile(queryLshVectors, sifts);
    }
}
