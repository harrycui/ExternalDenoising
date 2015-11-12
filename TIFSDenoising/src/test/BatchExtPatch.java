package test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;

import base.Imshow;
import prepare.PrepareTool;

public class BatchExtPatch {

	public static void main(String[] args) {

		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		if (args.length < 4) {

			System.out.println("Error: please provide the [imagesFolderPath] [outputPath] [step] [overlap]");
		}

		System.out.println("\nLoad image file list.");

		String imagesFolderPath = args[0];

		String outputPath = args[1];

		int step = Integer.parseInt(args[2]);

		int overlap = Integer.parseInt(args[3]);
		
		boolean showImage = false;
		
		if (args.length > 4) {
			
			if (args[4].equals("true") || args[4].equals("1")) {
				showImage = true;
			}
		}

		System.out.print("Start reading image file list...\n");

		try {
			List<Path> imagePathList = Files.walk(Paths.get(imagesFolderPath)).filter(Files::isRegularFile)
					.collect(Collectors.toList());

			System.out.println("     ---> Done\n\nLoaded " + imagePathList.size() + " images!!\n");

			int startId = 1;

			for (int i = 0; i < imagePathList.size(); ++i) {

				String tempPath = imagePathList.get(i).toString();
				
				String fileName = tempPath.substring(tempPath.lastIndexOf("/") + 1);

				Mat testImg = Highgui.imread(tempPath);

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

				startId = PrepareTool.extPatch(testImg, step, overlap, fileName, startId, outputPath, "patch.txt");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
