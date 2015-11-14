package base;

public class PatchWithLSH extends Patch {
	
	private long[] lshValues;

	public PatchWithLSH(int pid, int dim, int[] pixels, long[] lshValues) {

		super(pid, dim, pixels);
		this.lshValues = lshValues;
	}
	
	public PatchWithLSH(int pid, int dim, String valueStr, String lshStr, short lshL) {
		
		super(pid, dim, valueStr);
		
		this.lshValues = new long[lshL];
		
		String[] lshValues = lshStr.split(" ");
		
		for (int i = 0; i < lshValues.length; i++) {
			
			this.lshValues[i] = Long.parseLong(lshValues[i]);
		}
	}

	public long[] getLshValues() {
		return lshValues;
	}

	public void setLshValues(long[] lshValues) {
		this.lshValues = lshValues;
	}
}
