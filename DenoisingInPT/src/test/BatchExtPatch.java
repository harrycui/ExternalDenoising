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
import util.ConfigParser;
import util.FileUtil;
import util.PrepareTool;

public class BatchExtPatch {

	public static void main(String[] args) {

		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		if (args.length < 1) {

			System.out.println("Error: no argument provided!");
			
			return;
		}

		System.out.println("\nLoad image file list.");
		
		ConfigParser config = new ConfigParser(args[0]);

		
		int step = config.getInt("step");
		int overlap = config.getInt("overlap");
		int sigma = config.getInt("sigma");
		String type = config.getString("type");
		boolean isShowImage = config.getBool("showImage");
		String inputPath = config.getString("inputPath").replace("\\", "/");
		
		if (type.equals("query")) {
			inputPath +=  sigma + "/";
		}
		
		String outputPath = config.getString("outputPath").replace("\\", "/");

		System.out.print("Start reading image file list...\n");
		
		
		
		if (type.equals("database")) {
			
			FileUtil.deleteFile(outputPath + "patchDB-" + step + "-" + overlap + ".txt");
		} else {
			
			FileUtil.deleteDirectory(outputPath + step + "-" + overlap + "/" + sigma + "/");
		}

		try {
			List<Path> imagePathList = Files.walk(Paths.get(inputPath)).filter(Files::isRegularFile)
					.collect(Collectors.toList());

			System.out.println("     ---> Done\n\nLoaded " + imagePathList.size() + " images!!\n");

			int startId = 1;

			for (int i = 0; i < imagePathList.size(); ++i) {

				String tempPath = imagePathList.get(i).toString().replace("\\", "/");
				
				String outputFilePath = null;
				
				if (type.equals("database")) {
					outputFilePath = outputPath + "patchDB-" + step + "-" + overlap + ".txt";
				} else {
					
					String fileName = tempPath.substring(tempPath.lastIndexOf("/") + 1);
					
					outputFilePath = outputPath + step + "-" + overlap + "/" + sigma + "/" + fileName + ".txt";
					
					startId = 1;
				}
				
				String fileName = tempPath.substring(tempPath.lastIndexOf("/") + 1);

				Mat testImg = Highgui.imread(tempPath);

				if (isShowImage) {
					Imshow im1 = new Imshow("Show the image");
					im1.showImage(testImg);
				}

				startId = PrepareTool.extPatch(testImg, step, overlap, fileName, startId, outputFilePath);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
