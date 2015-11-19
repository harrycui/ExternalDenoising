package util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class PrepareTool {

	public static int extPatch(Mat image, int step, int overlap, String imageName, int startId, String filePath) {

		assert (image.type() == CvType.CV_8UC1);
		
		// this function is used to resize the image to be suitable for cutting
		image = resizeImage4Suitable(image, step, overlap);

		//String rawDataFileName = filePath + fileName;

		File file = new File(filePath);
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}

		BufferedWriter writer = null;

		try {

			writer = new BufferedWriter(new FileWriter(filePath, true));

			int width = image.cols();
			int height = image.rows();

			int endX = width - step;
			int endY = height - step;

			for (int curY = 0; curY <= endY; curY += (step - overlap)) {
				for (int curX = 0; curX <= endX; curX += (step - overlap)) {

					Rect roi = new Rect(curX, curY, step, step);

					image.submat(roi);

					Mat imageROI = image.submat(roi);


					//Highgui.imwrite(filePath + "rawPatch/" + imageName + "-" + startId + ".png", imageROI);

					// System.out.println(readPatch(imageROI, startId++));
					writer.write(readPatch(imageName, imageROI, startId) + "\n");

					System.out.println("Id : " + startId++ + " has been stored.");
				}
			}

			writer.flush();
			writer.close();

			System.out.println("Done!");

		} catch (IOException e) {

			e.printStackTrace();

		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}

		return startId;
	}

	public static String readPatch(String imageName, Mat patch, int id) {

		StringBuilder sb = new StringBuilder();

		sb.append(imageName + "-" + id + ":");

		for (int y = 0; y < patch.rows(); ++y) {
			for (int x = 0; x < patch.cols(); ++x) {

				sb.append(" " + (int) patch.get(y, x)[0]);
			}
		}

		return sb.toString();
	}

//	public static int[] resizeImage(int width, int height, int step, int overlap) {
//
//		double[] temp = new double[2];
//
//		int[] imageSize = new int[2];
//
//		temp[0] = Math.ceil((double) (width - overlap) / (double) (step - overlap));
//
//		temp[1] = Math.ceil((double) (height - overlap) / (double) (step - overlap));
//
//		imageSize[0] = ((int) (temp[0])) * (step - overlap) + overlap;
//
//		imageSize[1] = (int) (temp[1]) * (step - overlap) + overlap;
//
//		//System.out.println("New width is " + imageSize[0]);
//
//		//System.out.println("New height is " + imageSize[1]);
//
//		return imageSize;
//	}
	
	public static Mat resizeImage4Suitable(Mat ori, int step, int overlap) {
		
		double tempW = Math.ceil((double) (ori.cols() - overlap) / (double) (step - overlap));
		double tempH = Math.ceil((double) (ori.rows() - overlap) / (double) (step - overlap));
		
		int w = ((int)tempW) * (step - overlap) + overlap;
		int h = ((int)tempH) * (step - overlap) + overlap;
		
		if (w == ori.cols() && h == ori.rows()) {
			
			return ori;
		} else {
			
			Mat resizeimage = new Mat();
			Imgproc.resize(ori, resizeimage, new Size(w,h));
			ori.release();
			return resizeimage;
		}
	}
}
