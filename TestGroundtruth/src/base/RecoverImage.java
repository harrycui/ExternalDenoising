package base;

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
	
	public void recoverImageFromPatches(String outputPath) {
		
		// TODO: recover image
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
