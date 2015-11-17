package base;

import java.util.ArrayList;
import java.util.List;

public class RecoverImage {

	private int numOfPatches;
	
	private int height;
	
	private int width;
	
	private int step;
	
	private int overlap;
	
	private List<SimilarPatches> patches;
	
	private int[][] pixels;

	public RecoverImage(int numOfPatches, int height, int width, int step, int overlap, List<SimilarPatches> patches) {
		super();
		this.numOfPatches = numOfPatches;
		this.height = height;
		this.width = width;
		this.step = step;
		this.overlap = overlap;
		this.patches = patches;
		
		this.pixels = new int[height][width];
	}
	
	public static void recoverImageFromPatches(String outputPath, RecoverImage ri) {
		
		// TODO: recover image
		List<Patch> finalPatches = new ArrayList<Patch>(ri.getNumOfPatches());
		
		for (SimilarPatches sp: ri.getPatches()) {
			
			Patch fp = RecoverImage.denoisingOnPatch(sp.getQueryPatch(), sp.getPatches());
			
			finalPatches.add(fp);
		}
		
		int[][] counter = new int[ri.height][ri.width];
		
		int x = 0;
		int y = 0;
		
		for (Patch p: finalPatches) {
			
			for (int innerY = 0; innerY < ri.step; ++innerY) {
				for (int innerX = 0; innerX < ri.step; ++innerX) {
					
					ri.pixels[y+innerY][x+innerX] += p.getPixels()[innerY * ri.step + innerX];
					counter[y+innerY][x+innerX] += 1;
				}
			}
		}
	}
	
	private static Patch denoisingOnPatch(Patch qp, List<Patch> patches) {
		
		
		
		return null;
	}

	public int getNumOfPatches() {
		return numOfPatches;
	}

	public void setNumOfPatches(int numOfPatches) {
		this.numOfPatches = numOfPatches;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public List<SimilarPatches> getPatches() {
		return patches;
	}

	public void setPatches(List<SimilarPatches> patches) {
		this.patches = patches;
	}
}
