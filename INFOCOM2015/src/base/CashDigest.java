package base;

import tool.PRF;

/**
 * Created by HarryC on 1/9/15.
 */
public class CashDigest {

    private long k1;

    private long k2;

    public CashDigest(long lshValue, int l, String keyV, String keyR) {

        this.k1 = PRF.HMACSHA1ToUnsignedInt("1xx" + lshValue + "xx" + l, keyV);

        this.k2 = PRF.HMACSHA1ToUnsignedInt("2xx" + lshValue + "xx" + l, keyR);
    }

    public long getK2() {
        return k2;
    }

    public long getK1() {
        return k1;
    }
}
