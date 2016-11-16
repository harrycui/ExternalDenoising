package index;

public class IndexValue {

	private byte[] iv;
	
	private byte[] cipher;
	
	public IndexValue(byte[] iv, byte[] cipher) {
		
		this.iv = iv;
		this.cipher = cipher;
	}

	public byte[] getIv() {
		return iv;
	}

	public byte[] getCipher() {
		return cipher;
	}
}
