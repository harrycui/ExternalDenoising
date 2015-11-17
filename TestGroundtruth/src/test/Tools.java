package test;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.List;

import base.Image;
import base.Patch;
import java.util.TreeMap;
import java.util.Map;
import java.util.Comparator; 

public class Tools {


	public static List<Image> readImages(String inputPath, int dim, int numOfImage, int numOfPatchesInOneImage){
			
		BufferedReader reader = null;
		
		List<Image> imageList = new ArrayList<Image> ();
			
		try {

			reader = new BufferedReader(new FileReader(inputPath));

			String tempStr = null;
										
			int pid;
						
			int iid;
			
			for (int ctr=0; ctr < numOfImage; ++ctr){
				
				iid=ctr;
				
				String imageName=null;
				
				List<Patch> patchList = new ArrayList<Patch>();
				
				for(int j = 0 ; j < numOfPatchesInOneImage; ++j){
										
					tempStr=reader.readLine();
					
					// TODO: hardcode the format ": "
					
					String [] dataStr = tempStr.split(": ");
					
					String [] metaData = dataStr[0].split("-");
					
					imageName=metaData[0];
					
					String pidStr = metaData[1];
					
					pid=Integer.parseInt(pidStr);					
					
					String [] pixelStr = dataStr[1].split(" ");
					
					dim = pixelStr.length;
					
					int [] pixels=new int [dim];
										
					for (int t=0; t<dim; ++t){
						
						pixels[t]=(Integer.parseInt(pixelStr[t]));					
					}
				
					Patch onePatch=new Patch(pid, dim, pixels);
					
					patchList.add(onePatch);
					
				}
				
				Image oneImage=new Image(imageName, iid, patchList);
				
				imageList.add(oneImage);
				
			}
			
			System.out.println("Read image patches sucessfully!");
				
			}catch (IOException e) {

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
	public static int computeEuclideanDist(int [] x, int [] y) {
		
		assert(x.length == y.length);
		
		int dist = 0;
		
		for (int i = 0; i < x.length; ++i) {
			
			dist += (x[i] - y[i]) * (x[i] - y[i]);
			
		}
			
		return dist;
		
	}
	
	public static int[] resizeImage(int width, int height, int step, int overlap){
		
		double[] temp = new double[2];
		
		int[] imageSize = new int[2];
				
		//end point 
		
	// 1 + (n-1)*(step-overlap)+step-1=(n-1)*(step-overlap)+step=n*(step-overlap)+overlap
		
		
		temp[0] = Math.ceil((double)(width-overlap)/(double)(step-overlap));
		
		temp[1] = Math.ceil((double)(height-overlap)/(double)(step-overlap));
		
		imageSize[0] = ((int) (temp[0]))*(step-overlap)+overlap;
		
		imageSize[1] = (int) (temp[1])*(step-overlap)+overlap;

		System.out.println("New width is "+imageSize[0]);
		
		System.out.println("New height is "+imageSize[1]);
		
		return imageSize;
		
	}
	
	
}
