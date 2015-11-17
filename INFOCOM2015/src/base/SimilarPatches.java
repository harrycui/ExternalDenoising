package base;

import java.util.List;

public class SimilarPatches {
	
	private int pid;
	
	private Patch queryPatch;
	
	private List<Patch> patches;

	public SimilarPatches(int pid, Patch queryPatch, List<Patch> patches) {
		super();
		this.pid = pid;
		this.queryPatch = queryPatch;
		this.patches = patches;
	}

	public int getPid() {
		return pid;
	}

	public void setPid(int pid) {
		this.pid = pid;
	}

	public List<Patch> getPatches() {
		return patches;
	}

	public void setPatches(List<Patch> patches) {
		this.patches = patches;
	}

	public Patch getQueryPatch() {
		return queryPatch;
	}

	public void setQueryPatch(Patch queryPatch) {
		this.queryPatch = queryPatch;
	}
}
