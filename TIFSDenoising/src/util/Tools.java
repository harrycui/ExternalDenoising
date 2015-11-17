package util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;

import base.Image;
import base.Imshow;
import base.Patch;
import base.Pixel;
import base.RecoverImage;
import base.SimilarPatches;

public class Tools {

	public static Map<Integer, Patch> readPatches(String inputPath) {

		BufferedReader reader = null;

		Map<Integer, Patch> patches = new HashMap<Integer, Patch>(200000);

		try {

			reader = new BufferedReader(new FileReader(inputPath));

			int lineNumber = 0;

			String tempString = null;

			while ((tempString = reader.readLine()) != null) {

				++lineNumber;

				String[] subStrs = tempString.split(": ");

				String[] nameStr = subStrs[0].split("-");

				int pid = Integer.parseInt(nameStr[1]);

				String[] pixelStr = subStrs[1].split(" ");

				int dim = pixelStr.length;

				int[] pixels = new int[dim];

				for (int i = 0; i < dim; ++i) {

					pixels[i] = (Integer.parseInt(pixelStr[i]));
				}

				Patch onePatch = new Patch(pid, dim, pixels);

				patches.put(pid, onePatch);
			}

			System.out.println("Read DB patches sucessfully! Number of patches: " + lineNumber);

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

		return patches;
	}

	public static List<Image> readImages(String inputPath, int numOfImage, int numOfPatchesInOneImage) {

		BufferedReader reader = null;

		List<Image> imageList = new ArrayList<Image>();

		try {

			reader = new BufferedReader(new FileReader(inputPath));

			String tempStr = null;

			int pid;

			int iid;

			for (int ctr = 0; ctr < numOfImage; ++ctr) {

				iid = ctr;

				String imageName = null;

				List<Patch> patchList = new ArrayList<Patch>();

				for (int j = 0; j < numOfPatchesInOneImage; ++j) {

					tempStr = reader.readLine();

					// TODO: hardcode the format ": "

					String[] dataStr = tempStr.split(": ");

					String[] metaData = dataStr[0].split("-");

					imageName = metaData[0];

					String pidStr = metaData[1];

					pid = Integer.parseInt(pidStr);

					String[] pixelStr = dataStr[1].split(" ");

					int dim = pixelStr.length;

					int[] pixels = new int[dim];

					for (int t = 0; t < dim; ++t) {

						pixels[t] = (Integer.parseInt(pixelStr[t]));
					}

					Patch onePatch = new Patch(pid, dim, pixels);

					patchList.add(onePatch);

				}

				Image oneImage = new Image(imageName, iid, patchList);

				imageList.add(oneImage);

			}

			System.out.println("Read image patches sucessfully!");

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

		return imageList;

	}

	/**
	 * This function is used to compute the squared Euclidean distance.
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public static int computeEuclideanDist(int[] x, int[] y) {

		assert (x.length == y.length);

		int dist = 0;

		for (int i = 0; i < x.length; ++i) {

			dist += (x[i] - y[i]) * (x[i] - y[i]);

		}

		return dist;

	}

	public static int[] resizeImage(int width, int height, int step, int overlap) {

		double[] temp = new double[2];

		int[] imageSize = new int[2];

		temp[0] = Math.ceil((double) (width - overlap) / (double) (step - overlap));

		temp[1] = Math.ceil((double) (height - overlap) / (double) (step - overlap));

		imageSize[0] = ((int) (temp[0])) * (step - overlap) + overlap;

		imageSize[1] = (int) (temp[1]) * (step - overlap) + overlap;

		System.out.println("New width is " + imageSize[0]);

		System.out.println("New height is " + imageSize[1]);

		return imageSize;

	}

	public static void recoverImageFromPatches(String outputPath, RecoverImage ri, int threshold, int sigma) {

		// TODO: recover image
		List<Patch> finalPatches = new ArrayList<Patch>(ri.getNumOfPatches());

		for (SimilarPatches sp : ri.getPatches()) {

			Patch fp = Tools.nlmDenoise(sp.getQueryPatch(), sp.getPatches(), threshold, sigma);

			finalPatches.add(fp);
		}

		int[][] counter = new int[ri.getHeight()][ri.getWidth()];

		int x = 0;
		int y = 0;

		int pid = 0;
		for (Patch p : finalPatches) {

			Pixel firstPixel = Tools.computeLocation(pid, ri.getPatchWidth(), ri.getStep(), ri.getOverlap());
			
			for (int innerY = 0; innerY < ri.getStep(); ++innerY) {
				for (int innerX = 0; innerX < ri.getStep(); ++innerX) {

					x = firstPixel.getX() + innerX;
					y = firstPixel.getY() + innerY;
					ri.pixels[y][x] += p.getPixels()[innerY * ri.getStep() + innerX];
					counter[y][x] += 1;
				}
			}
			
			pid++;
		}
		
		assert(x == ri.getWidth() - 1 && y == ri.getHeight() - 1);
		
		System.out.println("heiht: " + ri.getHeight() + ", widht : " + ri.getWidth());
		
		Mat riMat = new Mat(ri.getHeight(), ri.getWidth(), CvType.CV_8UC1);
		
		for (int i = 0; i < ri.getHeight(); i++) {
			for (int j = 0; j < ri.getWidth(); j++) {
				ri.pixels[i][j] /= counter[i][j];
				riMat.put(i, j, ri.pixels[i][j]);
			}
		}
		
		Imshow im1 = new Imshow("Show the image");
		im1.showImage(riMat);
		
		Highgui.imwrite(outputPath + ri.getName(), riMat);
	}
	
	
	private static Pixel computeLocation(int pid, int patchWidth, int step, int overlap) {
		
		int patchX = pid % patchWidth;
		int patchY = pid / patchWidth;
		
		int x = patchX * (step - overlap);
		int y = patchY * (step - overlap);
		
		return new Pixel(x, y);
	}

	private static Patch nlmDenoise(Patch queryPatch, List<Patch> similarPatches, int threshold, int sigma) {

		List <Integer> indList = new ArrayList<Integer>();
		  
		  List <Double> weightList = new ArrayList<Double>();
		   
		  double h=2*sigma*sigma;
		  
		  double weightSum=0;
		  
		  // 1. Compute distances
		  
		  for (int i=0; i < similarPatches.size(); i++){
		   
		   double tempDist = computeEuclideanDist(queryPatch.getPixels(), similarPatches.get(i).getPixels());
		   
		   if(!(tempDist >threshold)){
		    
		    double weight =  Math.exp(-(tempDist/h));
		    
		    weightSum += weight;
		    
		    weightList.add(weight);
		    
		    indList.add(i);
		      
		   }
		      
		  }
		  
		  List <double[]> weightedPixelsList = new ArrayList<double []>();
		  
		  for (int j = 0; j < weightList.size(); j++){
		   
		   double tempWeight = weightList.get(j)/weightSum;
		   
		   double[] tempWeightedPixels = new double[64]; 
		   
		   for (int k = 0; k < 64; k++){
		    
		    tempWeightedPixels[k] = tempWeight * (similarPatches.get(indList.get(j)).getPixels()[k]); 
		    
		   }
		   
		   weightedPixelsList.add(tempWeightedPixels);
		   
		  }
		  
		  int[] pixels = new int[64];  
		   
		  for (int l = 0; l < 64; l++){
		   
		   for(int t = 0; t< weightedPixelsList.size(); t++){
		    
		    pixels[l] += (int) Math.floor(weightedPixelsList.get(t)[l]); 
		       
		   }
		   
		  }
		  
		  int pid = queryPatch.getPid();
		  
		  Patch denoisedPatch = new Patch(pid, 64, pixels);
		  
		  return denoisedPatch;
	}
	
	public static double psnr(int[][] ori, int[][] dst) {
	    
	    double psnr = 0.0;
	    
	    double mse =0.0;
	    
	    double maxValue = 255;
	    
	    int height = ori.length;
	    
	    int width = ori[0].length;
	    
	    for (int i = 0; i < height; i++){
	     
	     for (int j = 0; j< width; j++){
	      
	      mse += (ori[i][j]-dst[i][j])*(ori[i][j]-dst[i][j])/(height*width);
	      
	     }
	     
	    }
	    
	    psnr = 20*(Math.log10(maxValue/(Math.sqrt(mse))));
	     
	    return psnr;
	 }
}
