package base;

/**
 * Created by Helei on 10/3/2015.
 */
public class AddDigest {

    private String imageId;

    private int fid;

    private long key;

    private int value;

    public AddDigest(String imageId, int fid, long key, int value) {
        this.imageId = imageId;
        this.fid = fid;
        this.key = key;
        this.value = value;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public int getFid() {
        return fid;
    }

    public void setFid(int fid) {
        this.fid = fid;
    }

    public long getKey() {
        return key;
    }

    public void setKey(long key) {
        this.key = key;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
