package base;

import java.util.List;

public class SimilarPatches {
	
	private int pid;
	
	private List<Patch> patches;

	public SimilarPatches(int pid, List<Patch> patches) {
		super();
		this.pid = pid;
		this.patches = patches;
	}

	public int getPid() {
		return pid;
	}

	public void setPid(int pid) {
		this.pid = pid;
	}

	public List<Patch> getSimilarPatches() {
		return patches;
	}

	public void setSimilarPatches(List<Patch> patches) {
		this.patches = patches;
	}
}
