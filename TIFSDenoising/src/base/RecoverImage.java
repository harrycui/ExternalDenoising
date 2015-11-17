package base;

import java.util.List;

public class RecoverImage {
	
	private String name;

	private int numOfPatches;
	
	private int height;
	
	private int width;
	
	private int step;
	
	private int overlap;
	
	private int patchWidth;
	
	private List<SimilarPatches> patches;
	
	public int[][] pixels;

	public RecoverImage(String name, int numOfPatches, int height, int width, int step, int overlap, int patchWidth, List<SimilarPatches> patches) {
		super();
		this.name = name;
		this.numOfPatches = numOfPatches;
		this.height = height;
		this.width = width;
		this.step = step;
		this.overlap = overlap;
		this.patchWidth = patchWidth;
		this.patches = patches;
		
		this.pixels = new int[height][width];
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	public int getStep() {
		return step;
	}

	public void setStep(int step) {
		this.step = step;
	}

	public int getOverlap() {
		return overlap;
	}

	public void setOverlap(int overlap) {
		this.overlap = overlap;
	}

	public int getPatchWidth() {
		return patchWidth;
	}

	public void setPatchWidth(int patchWidth) {
		this.patchWidth = patchWidth;
	}

	public int[][] getPixels() {
		return pixels;
	}

	public void setPixels(int[][] pixels) {
		this.pixels = pixels;
	}
}
