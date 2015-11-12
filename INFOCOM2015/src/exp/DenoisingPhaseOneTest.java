package exp;

import base.*;
import index.CashIndex;
import tool.PRF;
import tool.TimeUtil;

import java.io.*;
import java.util.*;

/**
 *
 * This version is based on the TMC testing. I made this for the paper about Image Denoising.
 *
 * Created by HarryC on 7-Oct-2015.
 */
public class DenoisingPhaseOneTest {

    public static void main(String args[]) {

    	// initialization
        String lshFileName = null;
        String queryFileName = null;

        short lshL = 0;

        int limit = 1000000;

        if (args.length < 4) {
            System.err.println("Error: arguments are not enough! Please follow the format:\n\t[lsh file path] [query file path] [lshL] [limit]");

            System.exit(Constant.ERROR_ARGUMENTS);
        } else {

            lshFileName = args[0];
            queryFileName = args[1];


            lshL = Short.parseShort(args[2]);
            limit = Integer.parseInt(args[3]);
            //threadNum = Integer.parseInt(args[4]);
        }
        
        String keyV = "harry";
        String keyR = "cityu";
        
        int initialMaxId = 0; // this is used to indicate the number of initial data (for further dynamic operation)

        CashIndex cashIndex3 = new CashIndex(limit * lshL, lshL);

        List<String> lshFileList = new ArrayList<String>();

        System.out.println("\nLoad lsh file list.");

        File lshFileListPath = new File(lshFileName);

        BufferedReader br = null;

        try {
            System.out.print("Start reading lsh file list...\n");

            br = new BufferedReader(new FileReader(lshFileListPath));

            String tempString;

            int lshFileNum = 0;

            // read until null
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

            // reserve the last file for "add" testing
            for (int i = 0; i < lshFileList.size()-1; ++i) {

                file = new File(lshFileList.get(i));
                reader = new BufferedReader(new FileReader(file));

                // read util null
                while ((tempString = reader.readLine()) != null) {

                    ++lineNumber;

                    String[] lshLine = tempString.split(":");

                    LSHVector lshVector = new LSHVector(lineNumber, lshLine[1].replace("\n", ""), lshL);

                    cashIndex3.insert(lshVector, lshLine[0].replace(".feature", ""), lineNumber, keyV, keyR);


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

            CashIndex3.featureRankArray = new int[lineNumber + 1];

            initialMaxId = lineNumber;

            writer = new BufferedWriter(new FileWriter("./insertCashIndexResult.txt", true));


            writer.write("-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.\n\nDSSSE Experiment - Building Secure Index:" +
                    "\n\n\tData set: " + lshFileName);

            writer.write("\n\n\t\tInsert time: " + insertTime + " ms" +
                    "\n\n\tSetting:\n\t\tL = " + lshL +
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
                    //"[5]  query test with distance (thread num = L);\n" +
                    "[6] dynamic testing for \"add\"\n" +
                    "[7] dynamic testing for \"delete\"\n" +
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
                    } else if (Integer.parseInt(inputStr) > 7 || Integer.parseInt(inputStr) < 1) {

                        System.out.println("Warning: operation type should be limited in [1, 7], please try again!");

                        continue;
                    } else {
                        operationType = Integer.parseInt(inputStr);
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Warning: operation type should be limited in [1, 7], please try again!");
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

                        List<LSHVector> qf = loadQueryLshVectors(queryFileList.get(queryIndex - 1), lshL);

                        HashMap<String, Integer> searchResult = cashIndex3.search(qf, keyV, keyR);

                        if (searchResult != null && searchResult.size() > 0) {

                            System.out.println("\n\nThe search results are: ");

                            // TODO
                            List<String> topKId = CashIndex3.topK(20, searchResult);

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

                        List<LSHVector> qf = loadQueryLshVectors(queryFileList.get(queryIndex - 1), lshL);

                        HashMap<String, Integer> searchResult = cashIndex3.searchByUserDefinedThread(qf, keyV, keyR, threadNum);

                        if (searchResult != null && searchResult.size() > 0) {

                            System.out.println("\n\nThe search results are: ");

                            // TODO
                            List<String> topKId = CashIndex3.topK(20, searchResult);

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
            } else if (operationType == Constant.OPERATION_DYNAMIC_ADD) {

                System.out.println("\nModel: dynamically add files.");

                String addLshFilePath = lshFileList.get(lshFileList.size()-1);

                BufferedReader readerForDynAdd = null;
                File addLshFile = null;

                List<AddDigest> addDigestList = new ArrayList<AddDigest>();
                HashMap<Long, Long> localMaxC = new HashMap<Long, Long>();

                try {
                    System.out.print("Start reading files by line...\n");

                    String tempString;

                    int lineNumber = 0;

                    long startTime = System.currentTimeMillis();

                    addLshFile = new File(addLshFilePath);
                    readerForDynAdd = new BufferedReader(new FileReader(addLshFile));

                    while ((tempString = readerForDynAdd.readLine()) != null) {

                        ++lineNumber;

                        String[] lshLine = tempString.split(":");

                        int fid = initialMaxId+lineNumber;

                        LSHVector lshVector = new LSHVector(fid, lshLine[1].replace("\n", ""), lshL);

                        String imageId = lshLine[0].replace(".feature", "");

                        for (int i = 0; i < lshVector.getDimension(); ++i) {

                            Long lshValue = lshVector.getLSHValueByIndex(i);

                            long c = 0; // start from 0

                            // TODO: double check the connection method
                            long k1 = PRF.HMACSHA1ToUnsignedInt("1xx" + lshValue + "xx" + i, keyV);
                            //String k1 = 1 + "xx" + lshValue + "xx" + i;
                            //long k2 = Long.parseLong(2 + "00" + lshValue + "00" + i);

                            if (localMaxC.containsKey(k1)) {

                                c = localMaxC.get(k1) + 1;
                                localMaxC.put(k1, c);
                            } else {
                                localMaxC.put(k1, 1L);
                            }

                            long a = (int) (PRF.HMACSHA1ToUnsignedInt(String.valueOf(c), Long.toString(k1)));

                            addDigestList.add(new AddDigest(imageId, fid, a, fid));

                            cashIndex3.idMap.put(fid, imageId);

                            localMaxC.put(k1, c);

                            ++c;
                        }

                        if (lineNumber % (limit / 100) == 0) {
                            System.out.println("Transforming " + lineNumber / (limit / 100) + "%");
                        }

                        if (lineNumber == limit) {
                            break;
                        }
                    }

                    long transformTime = System.currentTimeMillis() - startTime;
                    int conflictNum = 0;
                    startTime = System.currentTimeMillis();

                    for (int i = 0; i < addDigestList.size(); ++i) {

                        if (cashIndex3.dynamicAdd(addDigestList.get(i).getKey(), addDigestList.get(i).getValue()) == false) {

                            conflictNum++;
                        }
                    }

                    readerForDynAdd.close();
                    file = null;
                    readerForDynAdd = null;

                    long insertTime = System.currentTimeMillis() - startTime;

                    // -------------------------------------------------------------------------------------------------------
                    //System.out.print("     ---> Done\n\nProcessed " + lineNumber + " records!! Total cost " + (insertTime + encryptTime) + " ms\n\t\t\tInsert time " + insertTime + " ms\n\t\t\tEncryption time " + encryptTime + " ms\n\nWriting result into file...");
                    System.out.print("     ---> Done\n\nProcessed " + lineNumber + " records!!\n\t\t\tThere are " + conflictNum + "conflicts!\n" +
                            "\t\t\tTransform time " + transformTime + "ms\n\t\t\tInsert time " + insertTime + " ms\n\n");

                    CashIndex3.featureRankArray = new int[CashIndex3.rawIndex.size() + CashIndex3.dynamicIndex.size() + 1];

                    System.out.println("        ---> Done");

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (readerForDynAdd != null) {
                        try {
                            readerForDynAdd.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                }

            } else if (operationType == Constant.OPERATION_DYNAMIC_DELETE) {

                try {
                    System.out.println("Please indicate the number of feature you want to delete:");

                    Scanner scan = new Scanner(System.in);
                    int delItemNum = scan.nextInt();

                    System.out.println("The number of deleted features are : " + delItemNum);

                    CashIndex3.revidSet.clear();

                    String delLshFilePath = lshFileList.get(lshFileList.size()-1);

                    BufferedReader readerForDynDel = null;
                    File delLshFile = null;

                    List<Long> delDigestList = new ArrayList<Long>();

                    try {
                        System.out.print("Start reading files by line...\n");

                        String tempString;

                        int lineNumber = 0;

                        long startTime = System.currentTimeMillis();

                        delLshFile = new File(delLshFilePath);
                        readerForDynDel = new BufferedReader(new FileReader(delLshFile));

                        while ((tempString = readerForDynDel.readLine()) != null) {

                            ++lineNumber;

                            String[] lshLine = tempString.split(":");

                            int fid = initialMaxId+lineNumber;

                            LSHVector lshVector = new LSHVector(fid, lshLine[1].replace("\n", ""), lshL);

                            String imageId = lshLine[0].replace(".feature", "");

                            for (int i = 0; i < lshVector.getDimension(); ++i) {

                                Long lshValue = lshVector.getLSHValueByIndex(i);

                                long k1 = PRF.HMACSHA1ToUnsignedInt("1xx" + lshValue + "xx" + i, keyV);
                                //String k1 = 1 + "xx" + lshValue + "xx" + i;
                                //long k2 = Long.parseLong(2 + "00" + lshValue + "00" + i);


                                long revid = (int) (PRF.HMACSHA1ToUnsignedInt(String.valueOf(fid), Long.toString(k1)));

                                delDigestList.add(revid);
                            }

                            if (lineNumber % (delItemNum / 100) == 0) {
                                System.out.println("Transforming " + lineNumber / (delItemNum / 100) + "%");
                            }

                            if (lineNumber == delItemNum) {
                                break;
                            }
                        }

                        long transformTime = System.currentTimeMillis() - startTime;
                        startTime = System.currentTimeMillis();

                        for (int i = 0; i < delDigestList.size(); ++i) {

                            cashIndex3.dynamicDel(delDigestList.get(i));
                        }

                        readerForDynDel.close();
                        file = null;
                        readerForDynDel = null;

                        long deleteTime = System.currentTimeMillis() - startTime;

                        // -------------------------------------------------------------------------------------------------------
                        //System.out.print("     ---> Done\n\nProcessed " + lineNumber + " records!! Total cost " + (insertTime + encryptTime) + " ms\n\t\t\tInsert time " + insertTime + " ms\n\t\t\tEncryption time " + encryptTime + " ms\n\nWriting result into file...");
                        System.out.print("     ---> Done\n\nProcessed " + lineNumber + " records!!\n\t\t\tTransform time " + transformTime + "ms\n\t\t\tDelete time " + deleteTime + " ms\n\n");

                        CashIndex3.featureRankArray = new int[CashIndex3.rawIndex.size() + CashIndex3.dynamicIndex.size() + 1];

                        System.out.println("        ---> Done");

                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (readerForDynDel != null) {
                            try {
                                readerForDynDel.close();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }


                } catch (InputMismatchException ime) {
                    //ime.printStackTrace();
                    System.out.println("Error: please input a integer value!");
                }
            }
        }
        try {
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<LSHVector> loadQueryLshVectors(String queryPath, short L) {

        List<LSHVector> queryLshVectors = new ArrayList<LSHVector>();
        //List<SiftDescriptor> sifts = new ArrayList<SiftDescriptor>();

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

        return queryLshVectors;
    }
}
