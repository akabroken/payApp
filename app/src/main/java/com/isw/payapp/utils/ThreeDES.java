package com.isw.payapp.utils;

import android.os.Build;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.HexFormat;

/**
 * Advanced Triple DES (3DES) encryption utility class
 * Supports ECB, CBC modes with proper padding and key management
 *
 * @version 2.0
 */
public class ThreeDES {

    // Constants
    private static final String ALGORITHM = "DESede";
    private static final String DEFAULT_MODE = "ECB";
    private static final String DEFAULT_PADDING = "PKCS5Padding";
    private static final String DEFAULT_TRANSFORMATION = ALGORITHM + "/" + DEFAULT_MODE + "/" + DEFAULT_PADDING;
    private static final byte[] ZERO_IV = new byte[8]; // For CBC mode
    private static final String ZERO_BLOCK = "0000000000000000";

    // Key types
    public enum KeyType {
        SINGLE_LENGTH(16),  // 16 hex chars = 8 bytes
        DOUBLE_LENGTH(32),  // 32 hex chars = 16 bytes
        TRIPLE_LENGTH(48);  // 48 hex chars = 24 bytes

        private final int hexLength;

        KeyType(int hexLength) {
            this.hexLength = hexLength;
        }

        public int getHexLength() {
            return hexLength;
        }
    }

    // Encryption modes
    public enum Mode {
        ECB("ECB"),
        CBC("CBC");

        private final String value;

        Mode(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    // Padding schemes
    public enum Padding {
        NO_PADDING("NoPadding"),
        PKCS5("PKCS5Padding");

        private final String value;

        Padding(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Performs Triple DES encryption/decryption with advanced options
     */
    public static String perform3DES(String keyHex, String dataHex, boolean encrypt,
                                     Mode mode, Padding padding) {
        try {
            validateHexInput(keyHex, "Key");
            validateHexInput(dataHex, "Data");

            byte[] keyBytes = hexStringToByteArray(keyHex);
            byte[] dataBytes = hexStringToByteArray(dataHex);

            // Adjust key to proper length
            byte[] adjustedKey = adjustKeyToProperLength(keyBytes);

            // Create transformation string
            String transformation = ALGORITHM + "/" + mode.getValue() + "/" + padding.getValue();

            // Generate secret key
            DESedeKeySpec keySpec = new DESedeKeySpec(adjustedKey);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM);
            SecretKey key = keyFactory.generateSecret(keySpec);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(transformation);

            if (mode == Mode.CBC) {
                // Use zero IV for CBC mode
                IvParameterSpec ivSpec = new IvParameterSpec(ZERO_IV);
                cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, key, ivSpec);
            } else {
                cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, key);
            }

            // Handle padding for NoPadding mode
            if (padding == Padding.NO_PADDING) {
                dataBytes = ensureBlockSize(dataBytes, 8);
            }

            byte[] result = cipher.doFinal(dataBytes);
            return byteArrayToHexString(result).toUpperCase();

        } catch (Exception e) {
            throw new RuntimeException("3DES operation failed: " + e.getMessage(), e);
        }
    }

    /**
     * ECB mode encryption with PKCS5 padding (most common)
     */
    public static String encryptECB(String keyHex, String dataHex) {
        return perform3DES(keyHex, dataHex, true, Mode.ECB, Padding.PKCS5);
    }

    /**
     * ECB mode decryption with PKCS5 padding
     */
    public static String decryptECB(String keyHex, String dataHex) {
        return perform3DES(keyHex, dataHex, false, Mode.ECB, Padding.PKCS5);
    }

    /**
     * ECB mode encryption with NoPadding (for precise block operations)
     */
    public static String encryptECBNoPadding(String keyHex, String dataHex) {
        return perform3DES(keyHex, dataHex, true, Mode.ECB, Padding.NO_PADDING);
    }

    /**
     * ECB mode decryption with NoPadding
     */
    public static String decryptECBNoPadding(String keyHex, String dataHex) {
        return perform3DES(keyHex, dataHex, false, Mode.ECB, Padding.NO_PADDING);
    }

    /**
     * CBC mode encryption
     */
    public static String encryptCBC(String keyHex, String dataHex, String ivHex) {
        try {
            validateHexInput(keyHex, "Key");
            validateHexInput(dataHex, "Data");
            validateHexInput(ivHex, "IV");

            byte[] keyBytes = hexStringToByteArray(keyHex);
            byte[] dataBytes = hexStringToByteArray(dataHex);
            byte[] ivBytes = hexStringToByteArray(ivHex);

            if (ivBytes.length != 8) {
                throw new IllegalArgumentException("IV must be 8 bytes (16 hex characters)");
            }

            byte[] adjustedKey = adjustKeyToProperLength(keyBytes);

            DESedeKeySpec keySpec = new DESedeKeySpec(adjustedKey);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM);
            SecretKey key = keyFactory.generateSecret(keySpec);

            Cipher cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);

            byte[] result = cipher.doFinal(dataBytes);
            return byteArrayToHexString(result).toUpperCase();

        } catch (Exception e) {
            throw new RuntimeException("CBC encryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * CBC mode decryption
     */
    public static String decryptCBC(String keyHex, String dataHex, String ivHex) {
        try {
            validateHexInput(keyHex, "Key");
            validateHexInput(dataHex, "Data");
            validateHexInput(ivHex, "IV");

            byte[] keyBytes = hexStringToByteArray(keyHex);
            byte[] dataBytes = hexStringToByteArray(dataHex);
            byte[] ivBytes = hexStringToByteArray(ivHex);

            if (ivBytes.length != 8) {
                throw new IllegalArgumentException("IV must be 8 bytes (16 hex characters)");
            }

            byte[] adjustedKey = adjustKeyToProperLength(keyBytes);

            DESedeKeySpec keySpec = new DESedeKeySpec(adjustedKey);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM);
            SecretKey key = keyFactory.generateSecret(keySpec);

            Cipher cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);

            byte[] result = cipher.doFinal(dataBytes);
            return byteArrayToHexString(result).toUpperCase();

        } catch (Exception e) {
            throw new RuntimeException("CBC decryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Advanced key check value generation with multiple standards
     */
    public static String generateKeyCheckValue(String keyHex, KcvMethod method) {
        validateHexInput(keyHex, "Key");

        String testData;
        switch (method) {
            case ANSI:
                testData = ZERO_BLOCK; // 8 bytes of zeros
                break;
            case VISA:
                testData = "FFFFFFFFFFFFFFFF"; // 8 bytes of FF
                break;
            case MASTERCARD:
                testData = "0123456789ABCDEF"; // Specific test pattern
                break;
            default:
                testData = ZERO_BLOCK;
        }

        String encrypted = encryptECBNoPadding(keyHex, testData);

        // Extract KCV based on method
        switch (method) {
            case ANSI:
            case VISA:
                return encrypted.substring(0, 6); // First 3 bytes
            case MASTERCARD:
                return encrypted.substring(0, 8); // First 4 bytes
            default:
                return encrypted.substring(0, 6);
        }
    }

    /**
     * KCV generation methods
     */
    public enum KcvMethod {
        ANSI, VISA, MASTERCARD
    }

    /**
     * Validates if a key is weak or semi-weak (security check)
     */
    public static boolean isWeakKey(String keyHex) {
        try {
            byte[] keyBytes = hexStringToByteArray(keyHex);
            byte[] adjustedKey = adjustKeyToProperLength(keyBytes);

            // Check for weak keys (all zeros, all ones, etc.)
            if (isUniformKey(adjustedKey)) {
                return true;
            }

            // Check for semi-weak keys (specific patterns)
            return isSemiWeakKey(adjustedKey);

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Key adjustment with proper validation
     */
    private static byte[] adjustKeyToProperLength(byte[] key) {
        if (key.length == 24) {
            return key; // Already triple length
        } else if (key.length == 16) {
            // Double length key: K1 + K2 -> K1 + K2 + K1
            byte[] newKey = new byte[24];
            System.arraycopy(key, 0, newKey, 0, 16);
            System.arraycopy(key, 0, newKey, 16, 8);
            return newKey;
        } else if (key.length == 8) {
            // Single length key: K1 -> K1 + K1 + K1
            byte[] newKey = new byte[24];
            System.arraycopy(key, 0, newKey, 0, 8);
            System.arraycopy(key, 0, newKey, 8, 8);
            System.arraycopy(key, 0, newKey, 16, 8);
            return newKey;
        } else {
            throw new IllegalArgumentException(
                    "Invalid key length: " + key.length + " bytes. " +
                            "Supported lengths: 8, 16, or 24 bytes");
        }
    }

    /**
     * Ensures data is proper block size for NoPadding mode
     */
    private static byte[] ensureBlockSize(byte[] data, int blockSize) {
        if (data.length % blockSize == 0) {
            return data;
        }

        int paddedLength = ((data.length / blockSize) + 1) * blockSize;
        byte[] paddedData = new byte[paddedLength];
        System.arraycopy(data, 0, paddedData, 0, data.length);
        // Remainder bytes remain zeros
        return paddedData;
    }

    /**
     * Validates hex input
     */
    private static void validateHexInput(String hex, String fieldName) {
        if (hex == null || hex.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }

        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException(fieldName + " must have even number of hex characters");
        }

        if (!hex.matches("[0-9A-Fa-f]+")) {
            throw new IllegalArgumentException(fieldName + " contains invalid hex characters");
        }
    }

    /**
     * Checks for uniform keys (weak keys)
     */
    private static boolean isUniformKey(byte[] key) {
        byte first = key[0];
        for (byte b : key) {
            if (b != first) return false;
        }
        return true;
    }

    /**
     * Checks for semi-weak keys
     */
    private static boolean isSemiWeakKey(byte[] key) {
        // Common semi-weak key patterns
        byte[][] weakPatterns = {
                {(byte)0x01, (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x01},
                {(byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE},
                {0x1F, 0x1F, 0x1F, 0x1F, 0x0E, 0x0E, 0x0E, 0x0E}
        };

        for (byte[] pattern : weakPatterns) {
            if (Arrays.equals(Arrays.copyOf(key, 8), pattern)) {
                return true;
            }
        }
        return false;
    }

    // Hex conversion utilities (optimized)
    public static byte[] hexStringToByteArray(String hex) {
        if (hex == null || hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Invalid hex string");
        }

        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    public static String byteArrayToHexString(byte[] bytes) {
        if (bytes == null) return "";

        StringBuilder hexString = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    // Modern Java 17+ HexFormat implementation
    public static String byteArrayToHexStringModern(byte[] bytes) {
        HexFormat hexFormat = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            hexFormat = HexFormat.of().withUpperCase();
            return hexFormat.formatHex(bytes);
        }
       return null;
    }

    public static byte[] hexStringToByteArrayModern(String hex) {
        HexFormat hexFormat = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            hexFormat = HexFormat.of();
            return hexFormat.parseHex(hex);
        }
        return null;
    }

    /**
     * Utility method to determine key type
     */
    public static KeyType getKeyType(String keyHex) {
        validateHexInput(keyHex, "Key");

        int length = keyHex.length();
        for (KeyType type : KeyType.values()) {
            if (length == type.getHexLength()) {
                return type;
            }
        }

        throw new IllegalArgumentException("Invalid key length: " + length + " hex characters");
    }
}