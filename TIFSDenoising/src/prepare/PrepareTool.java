package prepare;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.highgui.Highgui;

public class PrepareTool {

	public static int extPatch(Mat image, int step, int overlap, String imageName, int startId, String filePath, String fileName) {
				
		assert(image.type() == CvType.CV_8UC1);
		
		String rawDataFileName = filePath + fileName;
        
        File file = new File(rawDataFileName);
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		
		BufferedWriter writer = null;
		
		try {

			writer = new BufferedWriter(new FileWriter(rawDataFileName, true));
			
			int width = image.cols();
			int height = image.rows();
			
			int endX = width - step;
			int endY = height - step;
			
			for(int curY = 0; curY <= endY; curY += (step - overlap)) {
				for(int curX = 0; curX <= endX; curX += (step - overlap)) {
					
					Rect roi = new Rect(curX, curY, step, step);
					
					image.submat(roi);
					
					Mat imageROI = image.submat(roi);
					
					/*Imshow im1 = new Imshow("Show the image");
					im1.showImage(imageROI);*/
					
					Highgui.imwrite(filePath + "rawPatch/" + imageName + "-" + startId + ".png", imageROI);
					
					//System.out.println(readPatch(imageROI, startId++));
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
		
		for(int y = 0; y < patch.rows(); ++y) {
			for(int x = 0; x < patch.cols(); ++x) {
				
				sb.append(" " + (int)patch.get(y, x)[0]);
			}
		}
		
		return sb.toString();
	}
}
