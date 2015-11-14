package base;

import java.util.List;

public class Image {
		
	private String name;
	
	private int iid;
	
	private List<PatchWithLSH> patches;
	
	public Image(String name, int iid, List<PatchWithLSH> patches) {
		
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

	public List<PatchWithLSH> getPatches() {
		return patches;
	}

	public void setPatches(List<PatchWithLSH> patches) {
		this.patches = patches;
	}

}
