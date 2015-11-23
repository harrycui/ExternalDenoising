package util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

/**
 * Created by HarryC on 20/6/14.
 */
public class AESCoder {

    
    private static final String KEY_ALGORITHM = "AES";

    private static final String DEFAULT_CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";

    /**
     * åˆ�å§‹åŒ–å¯†é’¥
     *
     * @return byte[] å¯†é’¥
     * @throws Exception
     */
    public static byte[] initSecretKey() {
        //è¿”å›žç”Ÿæˆ�æŒ‡å®šç®—æ³•çš„ç§˜å¯†å¯†é’¥çš„ KeyGenerator å¯¹è±¡
        KeyGenerator kg = null;
        try {
            kg = KeyGenerator.getInstance(KEY_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return new byte[0];
        }
        //åˆ�å§‹åŒ–æ­¤å¯†é’¥ç”Ÿæˆ�å™¨ï¼Œä½¿å…¶å…·æœ‰ç¡®å®šçš„å¯†é’¥å¤§å°�
        //AES è¦�æ±‚å¯†é’¥é•¿åº¦ä¸º 128
        kg.init(128);
        //ç”Ÿæˆ�ä¸€ä¸ªå¯†é’¥
        SecretKey secretKey = kg.generateKey();
        return secretKey.getEncoded();
    }

    /**
     * è½¬æ�¢å¯†é’¥
     *
     * @param key äºŒè¿›åˆ¶å¯†é’¥
     * @return å¯†é’¥
     */
    public static Key toKey(byte[] key) {
        //ç”Ÿæˆ�å¯†é’¥
        return new SecretKeySpec(key, KEY_ALGORITHM);
    }

    /**
     * åŠ å¯†
     *
     * @param data å¾…åŠ å¯†æ•°æ�®
     * @param key  å¯†é’¥
     * @return byte[]   åŠ å¯†æ•°æ�®
     * @throws Exception
     */
    public static byte[] encrypt(byte[] data, Key key) throws Exception {
        return encrypt(data, key, DEFAULT_CIPHER_ALGORITHM);
    }

    /**
     * åŠ å¯†
     *
     * @param data å¾…åŠ å¯†æ•°æ�®
     * @param key  äºŒè¿›åˆ¶å¯†é’¥
     * @return byte[]   åŠ å¯†æ•°æ�®
     * @throws Exception
     */
    public static byte[] encrypt(byte[] data, byte[] key) throws Exception {
        return encrypt(data, key, DEFAULT_CIPHER_ALGORITHM);
    }


    /**
     * åŠ å¯†
     *
     * @param data            å¾…åŠ å¯†æ•°æ�®
     * @param key             äºŒè¿›åˆ¶å¯†é’¥
     * @param cipherAlgorithm åŠ å¯†ç®—æ³•/å·¥ä½œæ¨¡å¼�/å¡«å……æ–¹å¼�
     * @return byte[]   åŠ å¯†æ•°æ�®
     * @throws Exception
     */
    public static byte[] encrypt(byte[] data, byte[] key, String cipherAlgorithm) throws Exception {
        //è¿˜åŽŸå¯†é’¥
        Key k = toKey(key);
        return encrypt(data, k, cipherAlgorithm);
    }

    /**
     * åŠ å¯†
     *
     * @param data            å¾…åŠ å¯†æ•°æ�®
     * @param key             å¯†é’¥
     * @param cipherAlgorithm åŠ å¯†ç®—æ³•/å·¥ä½œæ¨¡å¼�/å¡«å……æ–¹å¼�
     * @return byte[]   åŠ å¯†æ•°æ�®
     * @throws Exception
     */
    public static byte[] encrypt(byte[] data, Key key, String cipherAlgorithm) throws Exception {
        //å®žä¾‹åŒ–
        Cipher cipher = Cipher.getInstance(cipherAlgorithm);
        //ä½¿ç”¨å¯†é’¥åˆ�å§‹åŒ–ï¼Œè®¾ç½®ä¸ºåŠ å¯†æ¨¡å¼�
        cipher.init(Cipher.ENCRYPT_MODE, key);
        //æ‰§è¡Œæ“�ä½œ
        return cipher.doFinal(data);
    }


    /**
     * è§£å¯†
     *
     * @param data å¾…è§£å¯†æ•°æ�®
     * @param key  äºŒè¿›åˆ¶å¯†é’¥
     * @return byte[]   è§£å¯†æ•°æ�®
     * @throws Exception
     */
    public static byte[] decrypt(byte[] data, byte[] key) throws Exception {
        return decrypt(data, key, DEFAULT_CIPHER_ALGORITHM);
    }

    /**
     * è§£å¯†
     *
     * @param data å¾…è§£å¯†æ•°æ�®
     * @param key  å¯†é’¥
     * @return byte[]   è§£å¯†æ•°æ�®
     * @throws Exception
     */
    public static byte[] decrypt(byte[] data, Key key) throws Exception {
        return decrypt(data, key, DEFAULT_CIPHER_ALGORITHM);
    }

    /**
     * è§£å¯†
     *
     * @param data            å¾…è§£å¯†æ•°æ�®
     * @param key             äºŒè¿›åˆ¶å¯†é’¥
     * @param cipherAlgorithm åŠ å¯†ç®—æ³•/å·¥ä½œæ¨¡å¼�/å¡«å……æ–¹å¼�
     * @return byte[]   è§£å¯†æ•°æ�®
     * @throws Exception
     */
    public static byte[] decrypt(byte[] data, byte[] key, String cipherAlgorithm) throws Exception {
        //è¿˜åŽŸå¯†é’¥
        Key k = toKey(key);
        return decrypt(data, k, cipherAlgorithm);
    }

    /**
     * è§£å¯†
     *
     * @param data            å¾…è§£å¯†æ•°æ�®
     * @param key             å¯†é’¥
     * @param cipherAlgorithm åŠ å¯†ç®—æ³•/å·¥ä½œæ¨¡å¼�/å¡«å……æ–¹å¼�
     * @return byte[]   è§£å¯†æ•°æ�®
     * @throws Exception
     */
    public static byte[] decrypt(byte[] data, Key key, String cipherAlgorithm) throws Exception {
        //å®žä¾‹åŒ–
        Cipher cipher = Cipher.getInstance(cipherAlgorithm);
        //ä½¿ç”¨å¯†é’¥åˆ�å§‹åŒ–ï¼Œè®¾ç½®ä¸ºè§£å¯†æ¨¡å¼�
        cipher.init(Cipher.DECRYPT_MODE, key);
        //æ‰§è¡Œæ“�ä½œ
        return cipher.doFinal(data);
    }

    @SuppressWarnings("unused")
	private static String showByteArray(byte[] data) {
        if (null == data) {
            return null;
        }
        StringBuilder sb = new StringBuilder("{");
        for (byte b : data) {
            sb.append(b).append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("}");
        return sb.toString();
    }

    /*public static void main(String[] args) throws Exception {
        byte[] key = initSecretKey();
        System.out.println("keyï¼š" + showByteArray(key));

        Key k = toKey(key);

        String data = "AESæ•°æ�®";
        System.out.println("åŠ å¯†å‰�æ•°æ�®: string:" + data);
        System.out.println("åŠ å¯†å‰�æ•°æ�®: byte[]:" + showByteArray(data.getBytes()));
        System.out.println();
        byte[] encryptData = encrypt(data.getBytes(), k);
        System.out.println("åŠ å¯†å�Žæ•°æ�®: byte[]:" + showByteArray(encryptData));
        System.out.println("åŠ å¯†å�Žæ•°æ�®: hexStr:" + Hex.encodeHexStr(encryptData));
        System.out.println();
        byte[] decryptData = decrypt(encryptData, k);
        System.out.println("è§£å¯†å�Žæ•°æ�®: byte[]:" + showByteArray(decryptData));
        System.out.println("è§£å¯†å�Žæ•°æ�®: string:" + new String(decryptData));

    }*/
}
