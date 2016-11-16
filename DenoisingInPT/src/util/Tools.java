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

import base.ImageInPatch;
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

	public static ImageInPatch readOneImageInPatches(String inputPath) {

		BufferedReader reader = null;

		ImageInPatch imageInPatch = null;

		try {

			reader = new BufferedReader(new FileReader(inputPath));

			List<Patch> patchList = new ArrayList<Patch>();

			int lineNumber = 0;

			String tempString = null;
			
			String imageName = inputPath.substring(inputPath.lastIndexOf('/')+1, inputPath.lastIndexOf('.'));

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

				patchList.add(onePatch);
			}

			imageInPatch = new ImageInPatch(imageName, 1, patchList);

			System.out.println("Read query image patches sucessfully! Number of patches: " + lineNumber);

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

		return imageInPatch;
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

	public static Mat recoverImageFromPatches(RecoverImage ri, int step, int threshold, int sigma, double k) {

		// TODO: recover image
		List<Patch> finalPatches = new ArrayList<Patch>(ri.getNumOfPatches());

		for (SimilarPatches sp : ri.getPatches()) {

			Patch fp = Tools.nlmDenoise(sp.getQueryPatch(), sp.getPatches(), step, threshold, sigma, k);

			finalPatches.add(fp);
		}

		int[][] counter = new int[ri.getHeight()][ri.getWidth()];

		int x = 0;
		int y = 0;

		int patchIndex = 0;
		for (Patch p : finalPatches) {

			Pixel firstPixel = Tools.computeLocation(patchIndex, ri.getPatchWidth(), ri.getStep(), ri.getOverlap());

			for (int innerY = 0; innerY < ri.getStep(); ++innerY) {
				for (int innerX = 0; innerX < ri.getStep(); ++innerX) {

					x = firstPixel.getX() + innerX;
					y = firstPixel.getY() + innerY;
					ri.pixels[y][x] += p.getPixels()[innerY * ri.getStep() + innerX];
					counter[y][x] += 1;
				}
			}

			patchIndex++;
		}

		assert (x == ri.getWidth() - 1 && y == ri.getHeight() - 1);

		System.out.println("heiht: " + ri.getHeight() + ", widht : " + ri.getWidth());

		Mat newImageMat = new Mat(ri.getHeight(), ri.getWidth(), CvType.CV_8UC1);

		for (int i = 0; i < ri.getHeight(); i++) {
			for (int j = 0; j < ri.getWidth(); j++) {
				ri.pixels[i][j] /= counter[i][j];
				newImageMat.put(i, j, ri.pixels[i][j]);
			}
		}

		return newImageMat;
	}

	private static Pixel computeLocation(int pid, int patchWidth, int step, int overlap) {

		int patchX = pid % patchWidth;
		int patchY = pid / patchWidth;

		int x = patchX * (step - overlap);
		int y = patchY * (step - overlap);

		return new Pixel(x, y);
	}

	private static Patch nlmDenoise(Patch queryPatch, List<Patch> similarPatches, int step, int threshold, int sigma, double k) {

		if (similarPatches.size() == 0) {

//			System.out.println(queryPatch.getPixels());
//			
//			for (int i = 0; i < queryPatch.getPixels().length; ++i) {
//				System.out.print(queryPatch.getPixels()[i] + " ");
//			}
			
			Patch denoisedPatch = new Patch(queryPatch);

			return denoisedPatch;
		} else {

			List<Integer> indList = new ArrayList<Integer>();

			List<Double> weightList = new ArrayList<Double>();

			double h = k * k * sigma * sigma;

			double weightSum = 0;

			// 1. Compute distances

			for (int i = 0; i < similarPatches.size(); i++) {

				double tempDist = computeEuclideanDist(queryPatch.getPixels(), similarPatches.get(i).getPixels());

				if (tempDist <= threshold) {

					double weight = Math.exp(-1 * (tempDist / h));

					weightSum += weight;

					weightList.add(weight);

					indList.add(i);

				}
			}

			if (weightSum == 0) {

				Patch denoisedPatch = new Patch(queryPatch);

				return denoisedPatch;
			} else {

				List<double[]> weightedPixelsList = new ArrayList<double[]>();

				for (int j = 0; j < weightList.size(); j++) {

					double tempWeight = weightList.get(j) / weightSum;

					double[] tempWeightedPixels = new double[step * step];

					for (int l = 0; l < step * step; l++) {

						tempWeightedPixels[l] = tempWeight * (similarPatches.get(indList.get(j)).getPixels()[l]);

					}

					weightedPixelsList.add(tempWeightedPixels);

				}

				int[] pixels = new int[step * step];

				for (int l = 0; l < step * step; l++) {

					for (int t = 0; t < weightedPixelsList.size(); t++) {

						pixels[l] += (int) Math.floor(weightedPixelsList.get(t)[l]);

					}

				}

				int pid = queryPatch.getPid();

				Patch denoisedPatch = new Patch(pid, step * step, pixels);

				return denoisedPatch;
			}
		}
	}

	public static double psnr(int[][] ori, int[][] dst) {

		double psnr = 0.0;

		double mse = 0.0;

		double maxValue = 255;

		int height = ori.length;

		int width = ori[0].length;

		for (int i = 0; i < height; i++) {

			for (int j = 0; j < width; j++) {

				mse += (ori[i][j] - dst[i][j]) * (ori[i][j] - dst[i][j]) / (height * width);

			}

		}

		psnr = 20 * (Math.log10(maxValue / (Math.sqrt(mse))));

		return psnr;
	}
	
	public static double psnr(Mat ori, Mat dst) {
		
		assert(ori.type() == dst.type() && ori.cols() == dst.cols() && ori.rows() == dst.rows());

		double psnr = 0.0;

		double mse = 0.0;

		double maxValue = 255;

		int height = ori.rows();

		int width = ori.cols();

		for (int i = 0; i < height; i++) {

			for (int j = 0; j < width; j++) {

				mse += (ori.get(i, j)[0] - dst.get(i, j)[0]) * (ori.get(i, j)[0] - dst.get(i, j)[0]) / (height * width);

			}

		}

		psnr = 20 * (Math.log10(maxValue / (Math.sqrt(mse))));

		return psnr;
	}
	
	public static int computeFitNumber(int length, int step, int overlap) {
		
		return (int) Math.ceil((double) (length - overlap) / (double) (step - overlap));
	}
	
	public static int computeFitSize(int length, int step, int overlap) {
		
		int fitNum = Tools.computeFitNumber(length, step, overlap);
		
		return fitNum * (step - overlap) + overlap;
	}
}
