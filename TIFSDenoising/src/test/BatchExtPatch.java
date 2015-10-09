package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import base.Imshow;
import prepare.PrepareTool;

public class BatchExtPatch {

	public static void main(String[] args) {
		
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		if(args.length < 3) {
			
			System.out.println("Error: please provide the [imageFileListPath] [step] [overlap]");
		}
		
		List<String> imagePathList = new ArrayList<String>();
		
		System.out.println("\nLoad image file list.");

        File imageFileListPath = new File(args[0]);
        
        int step = Integer.parseInt(args[1]);
        
        int overlap = Integer.parseInt(args[2]);
        
        BufferedReader br = null;

        try {
            System.out.print("Start reading image file list...\n");

            br = new BufferedReader(new FileReader(imageFileListPath));

            String tempString;

            int limageFileNum = 0;

            // read until null
            while ((tempString = br.readLine()) != null) {

            	imagePathList.add(tempString.replace("\n", ""));

                ++limageFileNum;
            }

            br.close();

            System.out.println("     ---> Done\n\nLoaded " + limageFileNum + " images!!\n");

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
        
        int startId = 1;
        
        for(int i = 0; i < imagePathList.size(); ++i) {
        	
        	String tempPath = imagePathList.get(i);
        	
        	String filePath = tempPath.substring(0, tempPath.lastIndexOf("/")+1);
        	
        	Mat testImg = Highgui.imread(tempPath);
    		
    		Imshow im1 = new Imshow("Show the image");
    		im1.showImage(testImg);
    		
    		/*SoftReference<Mat> mGrayMat = new SoftReference<Mat>(new Mat(testImg.rows(), testImg.cols(), CvType.CV_8UC1, new Scalar(0)));

    		Imgproc.cvtColor(testImg, mGrayMat.get(), Imgproc.COLOR_RGBA2GRAY);*/
    		
    		startId = PrepareTool.extPatch(testImg, step, overlap, startId, filePath, "patch.txt");
        }
	}
}
