package base;

import java.util.List;

public class ImageInPatch {
		
	private String name;
	
	private int iid;
	
	private List<Patch> patches;
	
	public ImageInPatch(String name, int iid, List<Patch> patches) {
		
		this.name = name;
		
		this.iid = iid;
		
		this.patches = patches;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getIid() {
		return iid;
	}

	public void setIid(int iid) {
		this.iid = iid;
	}

	public List<Patch> getPatches() {
		return patches;
	}

	public void setPatches(List<Patch> patches) {
		this.patches = patches;
	}

}
