package base;

import java.math.BigInteger;

import util.PRF;

/**
 * Created by HarryC on 1/9/15.
 */
public class CashDigest {

    private BigInteger k1;

    private BigInteger k2;

    public CashDigest(long lshValue, int l, String keyV, String keyR) {

        //this.k1 = PRF.HMACSHA1ToUnsignedInt("1xx" + lshValue + "xx" + l, keyV);

        //this.k2 = PRF.HMACSHA1ToUnsignedInt("2xx" + lshValue + "xx" + l, keyR);
        
        this.k1 = PRF.HMACSHA1ToBigInteger("1xx" + lshValue + "xx" + l, keyV);
		this.k2 = PRF.HMACSHA1ToBigInteger("2xx" + lshValue + "xx" + l, keyR);
    }

    public BigInteger getK2() {
        return k2;
    }

    public BigInteger getK1() {
        return k1;
    }
}
