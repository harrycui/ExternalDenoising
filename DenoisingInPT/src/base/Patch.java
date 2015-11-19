package base;

public class Patch {
	
	private int pid;
	
	private int dim;
	
	private int[] pixels;
	
	public Patch(Patch p) {
		
		this.pid = p.getPid();
		this.dim = p.getDim();
		this.pixels = new int[dim];
		
		for (int i = 0; i < dim; i++) {
			
			this.pixels[i] = p.getPixels()[i];
		}
	}

	public Patch(int pid, int dim, int[] pixels) {

		this.pid = pid;
		this.dim = dim;
		this.pixels = pixels;
	}
	
	public Patch(int pid, int dim, String valueStr) {
		
		this.pid = pid;
		this.dim = dim;
		this.pixels = new int[dim];
		
		String[] values = valueStr.split(" ");
		
		assert(values.length == dim);
		
		for (int i = 0; i < values.length; i++) {
			
			this.pixels[i] = Integer.parseInt(values[i]);
		}
	}

	public int getPid() {
		return pid;
	}

	public void setPid(int pid) {
		this.pid = pid;
	}

	public int getDim() {
		return dim;
	}

	public void setDim(int dim) {
		this.dim = dim;
	}

	public int[] getPixels() {
		return pixels;
	}

	public void setPixels(int[] pixels) {
		this.pixels = pixels;
	}
}
