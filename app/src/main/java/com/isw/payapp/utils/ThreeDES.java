package com.isw.payapp.utils;

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
 */
public class ThreeDES {

    private final byte[] keybyte = "0123456789123456".getBytes();
    private static final String Algorithm = "DESede";
    private SecretKey deskey;

    public ThreeDES() {
        deskey = new SecretKeySpec(keybyte, Algorithm);
    }

    // Encryption
    public byte[] encrypt(byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, deskey);
            return cipher.doFinal(data);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    // Decryption
    public byte[] decrypt(byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, deskey);
            return cipher.doFinal(data);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Performs Triple DES encryption/decryption in ECB mode
     */
    public static String tdesECB(String keyHex, String dataHex, boolean encrypt) {
        try {
            byte[] keyBytes = hexStringToByteArray(keyHex);
            byte[] dataBytes = hexStringToByteArray(dataHex);
            byte[] adjustedKey = adjustKeyLength(keyBytes);

            DESedeKeySpec keySpec = new DESedeKeySpec(adjustedKey);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DESede");
            SecretKey key = keyFactory.generateSecret(keySpec);

            // Fixed: Use ECB mode instead of CBC (consistent with method name)
            Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding");
            cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, key);

            byte[] result = cipher.doFinal(dataBytes);
            return byteArrayToHexString(result);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("TDES operation failed", e);
        }
    }

    public String tdesECBEncrypt(String keyHex, String dataHex) {
        return tdesECB(keyHex, dataHex, true);
    }

    public static String tdesECBDecrypt(String keyHex, String dataHex) {
        return tdesECB(keyHex, dataHex, false);
    }

    /**
     * Extracts 16 hex digit (8-byte) check value from encrypted data
     */
    public String extractCheckValue(String encryptedHex) {
        if (encryptedHex.length() < 16) {
            throw new IllegalArgumentException("Encrypted data too short for 16-digit check value");
        }
        return encryptedHex.substring(0, 16).toUpperCase();
    }

    /**
     * Generates a 16 hex digit check value by encrypting a zero block
     * Common method for generating key check values
     */
    public String generateCheckValue(String keyHex) {
        // Encrypt 8 bytes of zeros (16 hex digits) to generate check value
        String zeroBlock = "0000000000000000";
        String encrypted = tdesECBEncrypt(keyHex, zeroBlock);
        return extractCheckValue(encrypted);
    }

    /**
     * Alternative: Generate check value using specific test data
     */
    public String generateCheckValue(String keyHex, String testData) {
        if (testData.length() != 16) {
            throw new IllegalArgumentException("Test data must be 16 hex digits for 8-byte encryption");
        }
        String encrypted = tdesECBEncrypt(keyHex, testData);
        return extractCheckValue(encrypted);
    }

    /**
     * Backward compatibility - keep original KCV method but deprecate it
     */
    @Deprecated
    public String extractKcv(String encryptedHex) {
        if (encryptedHex.length() < 6) {
            throw new IllegalArgumentException("Encrypted data too short for KCV");
        }
        return encryptedHex.substring(0, 6).toUpperCase();
    }

    // Key adjustment methods
    private static byte[] adjustKeyLength(byte[] key) {
       // key = expand10ByteKeyTo24(key);
        if (key.length == 24) {
            return key;
        } else if (key.length == 16) {
            byte[] newKey = new byte[24];
            System.arraycopy(key, 0, newKey, 0, 16);
            System.arraycopy(key, 0, newKey, 16, 8);
            return newKey;
        } else {
            throw new IllegalArgumentException("Key must be 16 or 24 bytes for Triple DES");
        }
    }

    private static byte[] expand10ByteKeyTo24(byte[] key) {
        if (key.length == 10) {
            byte[] key24 = new byte[24];
            for (int i = 0; i < 24; i++) {
                key24[i] = key[i % 10];
            }
            return key24;
        }
        return key; // Return original if not 10 bytes
    }

    // Hex conversion utilities
    //
    public static byte[] hexStringToByteArray(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }

    public static String byteArrayToHexString(byte[] bytes) {
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

    // Using Java 8+ HexFormat (alternative implementation)
//    private static String byteArrayToHexStringModern(byte[] bytes) {
//        HexFormat hexFormat = HexFormat.of().withUpperCase();
//        return hexFormat.formatHex(bytes);
//    }
//
//    private static byte[] hexStringToByteArrayModern(String hex) {
//        HexFormat hexFormat = HexFormat.of();
//        return hexFormat.parseHex(hex);
//    }
}