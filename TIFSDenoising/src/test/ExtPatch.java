package test;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;

import base.Imshow;
import util.PrepareTool;

public class ExtPatch {

	public static void main(String[] args) {

		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		if (args.length < 4) {

			System.out.println("Error: please provide the [imagePath] [outputPath] [step] [overlap]");
		}

		System.out.println("\nLoad one image.");

		String imagePath = args[0];

		String outputPath = args[1];

		int step = Integer.parseInt(args[2]);

		int overlap = Integer.parseInt(args[3]);
		
		boolean showImage = false;
		
		if (args.length > 4) {
			
			if (args[4].equals("true") || args[4].equals("1")) {
				showImage = true;
			}
		}

		System.out.print("Start reading image file...\n");
		
		String fileName = imagePath.substring(imagePath.lastIndexOf("/") + 1);

		Mat testImg = Highgui.imread(imagePath);

		if (showImage) {
			Imshow im1 = new Imshow("Show the image");
			im1.showImage(testImg);
		}

		/*
		 * SoftReference<Mat> mGrayMat = new SoftReference<Mat>(new
		 * Mat(testImg.rows(), testImg.cols(), CvType.CV_8UC1, new
		 * Scalar(0)));
		 * 
		 * Imgproc.cvtColor(testImg, mGrayMat.get(),
		 * Imgproc.COLOR_RGBA2GRAY);
		 */

		PrepareTool.extPatch(testImg, step, overlap, fileName, 1, outputPath);
	}
}
