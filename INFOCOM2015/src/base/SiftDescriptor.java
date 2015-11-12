package base;

/**
 * Created by HarryC on 1/11/15.
 */
public class SiftDescriptor {

    private short[] sift;

    public SiftDescriptor(String dataStr) {

        sift = new short[128];

        String[] values = dataStr.split(" ");

        for (int i = 0; i < 128; ++i) {
            sift[i] = Short.parseShort(values[i]);
        }
    }

    public short at(int index) {
        return sift[index];
    }

    public static double calculateDistance(SiftDescriptor a, SiftDescriptor b) {

        double distance = 0.0;

        for (int i = 0; i < 128; ++i) {
            distance += Math.pow(a.at(i) - b.at(i), 2);
        }

        return Math.sqrt(distance);
    }
}
