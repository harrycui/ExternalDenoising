package test;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import base.Imshow;

public class BatchConvertGray {

	public static void main(String[] args) {

		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		if (args.length < 2) {

			System.out.println("Error: please provide the [imagesFolderPath] [outputPath]");
		}

		String imageFolderPath = args[0];
		String outputFolderPath = args[1];
		
		boolean showImage = false;
		
		if (args.length > 2) {
			
			if (args[2].equals("true") || args[2].equals("1")) {
				showImage = true;
			}
		}

		try {
			List<Path> imagePathList = Files.walk(Paths.get(imageFolderPath)).filter(Files::isRegularFile)
					.collect(Collectors.toList());

			System.out.println("     ---> Done\n\nLoaded " + imagePathList.size() + " images!!\n");

			for (int i = 0; i < imagePathList.size(); ++i) {

				String tempPath = imagePathList.get(i).toString();

				Mat testImg = Highgui.imread(tempPath);

				//String filePath = tempPath.substring(0, tempPath.lastIndexOf("/") + 1);

				String fileName = tempPath.substring(tempPath.lastIndexOf("/") + 1);

				if (showImage) {
					Imshow im1 = new Imshow("Show the image");
					im1.showImage(testImg);
				}
				
				SoftReference<Mat> mGrayMat = new SoftReference<Mat>(
						new Mat(testImg.rows(), testImg.cols(), CvType.CV_8UC1, new Scalar(0)));

				Imgproc.cvtColor(testImg, mGrayMat.get(), Imgproc.COLOR_RGBA2GRAY);

				Highgui.imwrite(outputFolderPath + fileName, mGrayMat.get());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
