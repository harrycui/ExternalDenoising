package base;

import java.util.List;

/**
 * Created by HarryC on 1/11/15.
 */
public class QueryFile {

    private List<LSHVector> lshVectors;

    private List<SiftDescriptor> sifts;

    public QueryFile(List<LSHVector> lshVectors, List<SiftDescriptor> sifts) {

        this.lshVectors = lshVectors;
        this.sifts = sifts;
    }

    public List<LSHVector> getLshVectors() {
        return lshVectors;
    }

    public void setLshVectors(List<LSHVector> lshVectors) {
        this.lshVectors = lshVectors;
    }

    public List<SiftDescriptor> getSifts() {
        return sifts;
    }

    public void setSifts(List<SiftDescriptor> sifts) {
        this.sifts = sifts;
    }
}
