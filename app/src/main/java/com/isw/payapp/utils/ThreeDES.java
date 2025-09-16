package com.isw.payapp.utils;

/**
 * Created by yemiekai on 2016/12/20 0020.
 */

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import java.util.HexFormat;


/**
 * 项目名称：****
 * 类名称：ThreeDES
 * 类描述： 3des 加密工具类
 * @version  1.0
 *
 */
public class ThreeDES {

    //key 根据实际情况对应的修改
    private final byte[] keybyte="0123456789123456".getBytes(); //keybyte为加密密钥，长度为16字节
    private static final String Algorithm = "DESede"; //定义 加密算法,可用 DES,DESede,Blowfish
    private SecretKey deskey;
    ///生成密钥
    public ThreeDES(){
        deskey = new SecretKeySpec(keybyte, Algorithm);
    }
    //加密
    public byte[] encrypt(byte[] data){
        try {
            Cipher cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, deskey);
            return cipher.doFinal(data);
        } catch (Exception ex) {
            //加密失败，打日志
            ex.printStackTrace();
        }
        return null;
    }
    //解密
    public byte[] decrypt(byte[] data){
        try {
            Cipher cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE,deskey);
            return cipher.doFinal(data);
        } catch (Exception ex) {
            //解密失败，打日志
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Performs Triple DES encryption/decryption in ECB mode
     * @param keyHex Hex string of the key (16, 24, or 32 bytes for DESede)
     * @param dataHex Hex string of the data to encrypt/decrypt
     * @param encrypt true for encryption, false for decryption
     * @return Hex string of the result
     */
    public static String tdesECB(String keyHex, String dataHex, boolean encrypt) {
        try {
            // Convert hex strings to byte arrays
            byte[] keyBytes = hexStringToByteArray(keyHex);
            byte[] dataBytes = hexStringToByteArray(dataHex);

            // Ensure key is proper length for Triple DES (24 bytes)
            byte[] adjustedKey = adjustKeyLength(keyBytes);

            // Create DESede key specification
            DESedeKeySpec keySpec = new DESedeKeySpec(adjustedKey);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DESede");
            SecretKey key = keyFactory.generateSecret(keySpec);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding");
            cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, key);

            // Perform encryption/decryption
            byte[] result = cipher.doFinal(dataBytes);

            // Convert result to hex string
            return byteArrayToHexString(result);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("TDES operation failed", e);
        }
    }

    /**
     * Triple DES encryption in ECB mode
     * @param keyHex Hex string of the key
     * @param dataHex Hex string of the data to encrypt
     * @return Hex string of the encrypted data
     */
    public  String tdesECBEncrypt(String keyHex, String dataHex) {
        return tdesECB(keyHex, dataHex, true);
    }

    /**
     * Triple DES decryption in ECB mode
     * @param keyHex Hex string of the key
     * @param dataHex Hex string of the data to decrypt
     * @return Hex string of the decrypted data
     */
    public static String tdesECBDecrypt(String keyHex, String dataHex) {
        return tdesECB(keyHex, dataHex, false);
    }

    /**
     * Adjusts key length for Triple DES
     * Triple DES requires 24-byte key. If key is shorter, it's padded/expanded.
     */
    private static byte[] adjustKeyLength(byte[] key) {
        key = expand10ByteKeyTo24(key);
        if (key.length == 24) {
            return key;
        } else if (key.length == 16) {
            // For 16-byte key, copy first 8 bytes to end to make 24 bytes
            byte[] newKey = new byte[24];
            System.arraycopy(key, 0, newKey, 0, 16);
            System.arraycopy(key, 0, newKey, 16, 8);
            return newKey;
        } else {
            throw new IllegalArgumentException("Key must be 16 or 24 bytes for Triple DES");
        }
    }

    /**
     * Converts hex string to byte array
     */
    private static byte[] hexStringToByteArray(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }

    /**
     * Converts byte array to hex string
     */
    private static String byteArrayToHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString().toUpperCase();
    }

    /**
     * Extracts KCV (Key Check Value) from encrypted data (first 3 bytes)
     */
    public  String extractKcv(String encryptedHex) {
        if (encryptedHex.length() < 6) {
            throw new IllegalArgumentException("Encrypted data too short for KCV");
        }
        return encryptedHex.substring(0, 6).toUpperCase();
    }

    /**
     * Expand 10-byte key to 24 bytes for Triple DES
     * Strategy: Repeat the key pattern to fill 24 bytes
     */
    private static byte[] expand10ByteKeyTo24(byte[] key10) {
//        if (key10.length != 10) {
//            throw new IllegalArgumentException("Expected 10-byte key, got: " + key10.length + " bytes");
//        }

        byte[] key24 = new byte[24];

        // Fill the 24-byte array by repeating the 10-byte pattern
        // This is a common approach for short keys in payment systems
        for (int i = 0; i < 24; i++) {
            key24[i] = key10[i % 10];
        }

        return key24;
    }

    /**
     * Alternative expansion method - pad with specific values if needed
     */
    private static byte[] expand10ByteKeyTo24Alternative(byte[] key10) {
        if (key10.length != 10) {
            throw new IllegalArgumentException("Expected 10-byte key, got: " + key10.length + " bytes");
        }

        byte[] key24 = new byte[24];

        // Copy the original 10 bytes
        System.arraycopy(key10, 0, key24, 0, 10);

        // Pad the remaining 14 bytes with a specific pattern
        // Common patterns: zeros, repeat the key, or specific padding bytes
        for (int i = 10; i < 24; i++) {
            key24[i] = (byte) 0x00; // Pad with zeros
            // OR: key24[i] = key10[i % 10]; // Repeat the key pattern
            // OR: key24[i] = (byte) 0xFF; // Pad with 0xFF
        }

        return key24;
    }

}
