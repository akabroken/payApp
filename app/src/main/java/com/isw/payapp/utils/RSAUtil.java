package com.isw.payapp.utils;

import android.util.Base64;

import com.dspread.print.util.TRACE;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * RSA Utility class for asymmetric encryption and decryption
 * Provides methods for key generation, encryption, decryption, and key management
 */
public class RSAUtil {

    private static final String RSA_ALGORITHM = "RSA";
    private static final String RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding";
    private static final int DEFAULT_KEY_SIZE = 2048;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;
    private KeyPair keyPair;

    public RSAUtil() {
        // Initialize with default key pair
        generateKeyPair(DEFAULT_KEY_SIZE);
    }

    public RSAUtil(int keySize) {
        generateKeyPair(keySize);
    }

    public RSAUtil(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    /**
     * Generate RSA key pair
     * @param keySize Key size in bits (recommended: 2048 or higher)
     */
    public void generateKeyPair(int keySize) {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
            keyPairGenerator.initialize(keySize, SECURE_RANDOM);
            this.keyPair = keyPairGenerator.generateKeyPair();
            this.publicKey = (RSAPublicKey) keyPair.getPublic();
            this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate RSA key pair", e);
        }
    }

    /**
     * Encrypt data using the stored public key
     * @param plaintext Text to encrypt
     * @return Base64 encoded encrypted data
     */
    public String encrypt(String plaintext) throws Exception {
        if (publicKey == null) {
            throw new IllegalStateException("Public key not initialized");
        }
        return encrypt(plaintext, publicKey);
    }

    /**
     * Encrypt data using provided public key
     * @param plaintext Text to encrypt
     * @param publicKey Public key to use for encryption
     * @return Base64 encoded encrypted data
     */
    public String encrypt(String plaintext, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP);
    }

    /**
     * Decrypt data using the stored private key
     * @param ciphertext Base64 encoded encrypted data
     * @return Decrypted plaintext
     */
    public String decrypt(String ciphertext) throws Exception {
        if (privateKey == null) {
            throw new IllegalStateException("Private key not initialized");
        }
        return decrypt(ciphertext, privateKey);
    }

    /**
     * Decrypt data using provided private key
     * @param ciphertext Base64 encoded encrypted data
     * @param privateKey Private key to use for decryption
     * @return Decrypted plaintext
     */
    public String decrypt(String ciphertext, PrivateKey privateKey) throws Exception {
        byte[] ciphertextBytes = Base64.decode(ciphertext, Base64.NO_WRAP);
        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = cipher.doFinal(ciphertextBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * Get public key modulus and exponent as Base64 strings
     * @return Map containing modulus and exponent
     */
    public Map<String, String> getPublicKeyPrivateComponents() {
        if (publicKey == null) {
            throw new IllegalStateException("Public key not initialized");
        }

        Map<String, String> components = new HashMap<>();
        components.put("modulus", Base64.encodeToString(publicKey.getModulus().toByteArray(), Base64.NO_WRAP));
        components.put("exponent", Base64.encodeToString(publicKey.getPublicExponent().toByteArray(), Base64.NO_WRAP));
        components.put("priModulus",Base64.encodeToString(privateKey.getModulus().toByteArray(),Base64.NO_WRAP));
        components.put("priExponent", Base64.encodeToString(privateKey.getPrivateExponent().toByteArray(),Base64.NO_WRAP));
        return components;
    }

    /**
     * Create public key from modulus and exponent
     * @param modulusBase64 Base64 encoded modulus
     * @param exponentBase64 Base64 encoded exponent
     * @return RSAPublicKey instance
     */
    public static RSAPublicKey createPublicKeyFromComponents(String modulusBase64, String exponentBase64) throws Exception {
        byte[] modBytes = Base64.decode(modulusBase64, Base64.NO_WRAP);
        byte[] expBytes = Base64.decode(exponentBase64, Base64.NO_WRAP);

        BigInteger modulus = new BigInteger(1, modBytes);
        BigInteger exponent = new BigInteger(1, expBytes);

        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        return (RSAPublicKey) keyFactory.generatePublic(keySpec);
    }

    /**
     * Export public key as Base64 string
     * @return Base64 encoded public key
     */
    public String exportPublicKey() {
        if (publicKey == null) {
            throw new IllegalStateException("Public key not initialized");
        }
        return Base64.encodeToString(publicKey.getEncoded(), Base64.NO_WRAP);
    }

    /**
     * Import public key from Base64 string
     * @param publicKeyBase64 Base64 encoded public key
     * @return RSAPublicKey instance
     */
    public static RSAPublicKey importPublicKey(String publicKeyBase64) throws Exception {
        byte[] keyBytes = Base64.decode(publicKeyBase64, Base64.NO_WRAP);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        return (RSAPublicKey) keyFactory.generatePublic(keySpec);
    }

    /**
     * Export private key as Base64 string
     * @return Base64 encoded private key
     */
    public String exportPrivateKey() {
        if (privateKey == null) {
            throw new IllegalStateException("Private key not initialized");
        }
        return Base64.encodeToString(privateKey.getEncoded(), Base64.NO_WRAP);
    }

    /**
     * Export private key as byte array
     */
    /**
     * Export private key as Base64 string
     * @return Base64 encoded private key
     */
//    public RSAPublicKey exportPrivateKey_() {
//        if (privateKey == null) {
//            throw new IllegalStateException("Private key not initialized");
//        }
//        return Base64.decode(privateKey.getEncoded(), Base64.NO_WRAP);
//    }

    /**
     * Import private key from Base64 string
     * @param privateKeyBase64 Base64 encoded private key
     * @return RSAPrivateKey instance
     */
    public static RSAPrivateKey importPrivateKey(String privateKeyBase64) throws Exception {
        byte[] keyBytes = Base64.decode(privateKeyBase64, Base64.NO_WRAP);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    }

    /**
     * Export private key as Base64 string
     */
//    public String exportPrivateKeyBase64() {
//        return Base64.encodeToString(exportPrivateKey(), Base64.NO_WRAP);
//    }

    /**
     * Export public key as RSAPublicKey object
     */

    /**
     * Corrected decryption method for hex-encoded ciphertext
     */
    public static String decryptHexCiphertext(String o,String hexCiphertext, RSAPrivateKey privateKey) {
        try {
            // Convert hex string to byte array
            byte[] ciphertextBytes = hexStringToByteArray(hexCiphertext);

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            // Decrypt the ciphertext
            byte[] decryptedBytes = cipher.doFinal(ciphertextBytes);

            // Convert to string
            String te = bytesToHex(decryptedBytes);
            return te; //new String(decryptedBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            System.err.println("Decryption error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Corrected decryption method for hex-encoded ciphertext
     */
//    public static String decryptHexCiphertext(String hexCiphertext, RSAPrivateKey privateKey) {
//        try {
//            // Convert hex string to byte array
//            byte[] ciphertextBytes = hexStringToByteArray(hexCiphertext);
//
//            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
//            cipher.init(Cipher.DECRYPT_MODE, privateKey);
//
//            // Decrypt the ciphertext
//            byte[] decryptedBytes = cipher.doFinal(ciphertextBytes);
//
//            // Return the actual decrypted string, not hex
//            //String new_ = new String(decryptedBytes, StandardCharsets.UTF_8);
//            //return hex2ascii(decryptedBytes);//bytesToHex(new_.getBytes(StandardCharsets.UTF_8));
//            return bytesToHex(decryptedBytes,"");
//
//        } catch (Exception e) {
//            System.err.println("Decryption error: " + e.getMessage());
//            e.printStackTrace();
//            return null;
//        }
//    }

    /**
     * Corrected decryption method for hex-encoded ciphertext
     */
    public static String decryptHexCiphertext(String hexCiphertext, RSAPrivateKey privateKey) {
        try {
            // Validate inputs
            if (hexCiphertext == null || hexCiphertext.trim().isEmpty()) {
                throw new IllegalArgumentException("Hex ciphertext cannot be null or empty");
            }
            if (privateKey == null) {
                throw new IllegalArgumentException("Private key cannot be null");
            }

            TRACE.d("Hex ciphertext length: " + hexCiphertext.length());
            TRACE.d("Hex ciphertext (first 100 chars): " +
                    (hexCiphertext.length() > 100 ? hexCiphertext.substring(0, 100) + "..." : hexCiphertext));

            // Convert hex string to byte array
            byte[] ciphertextBytes = hexStringToByteArray(hexCiphertext);
            TRACE.d("Ciphertext byte length: " + ciphertextBytes.length);

            // Check if ciphertext length matches key size
            int keySizeBytes = privateKey.getModulus().bitLength() / 8;
            TRACE.d("Expected ciphertext size for " + privateKey.getModulus().bitLength() +
                    "-bit key: " + keySizeBytes + " bytes");

            if (ciphertextBytes.length != keySizeBytes) {
                TRACE.w("Ciphertext size mismatch. Expected: " + keySizeBytes +
                        ", Actual: " + ciphertextBytes.length);
            }

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            // Decrypt the ciphertext
            byte[] decryptedBytes = cipher.doFinal(ciphertextBytes);
            TRACE.d("Decrypted byte length: " + decryptedBytes.length);

            // **CRITICAL FIX**: Return the actual decrypted content as string
            // First, try to interpret as UTF-8 string (most common case for keys)
            String decryptedString = new String(decryptedBytes, StandardCharsets.UTF_8);
            TRACE.d("Decrypted string: " + decryptedString);


            return hex2ascii(decryptedString.getBytes(StandardCharsets.UTF_8));

        } catch (Exception e) {
            TRACE.e("Decryption error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Create private key from modulus and private exponent components
     */
    public static RSAPrivateKey createPrivateKeyFromComponents(String modulusBase64, String privateExponentBase64) throws Exception {
        byte[] modBytes = Base64.decode(modulusBase64, Base64.NO_WRAP);
        byte[] expBytes = Base64.decode(privateExponentBase64, Base64.NO_WRAP);

        BigInteger modulus = new BigInteger(1, modBytes);
        BigInteger privateExponent = new BigInteger(1, expBytes);

        RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(modulus, privateExponent);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    }

    /**
     * Enhanced method with comprehensive error handling
     */
    public static String decryptWithPrivateKeyComponents(String encryptedData,
                                                         String modulusBase64,
                                                         String privateExponentBase64,
                                                         boolean isHexEncoded) {
        try {
            // Validate inputs
            if (modulusBase64 == null || privateExponentBase64 == null || encryptedData == null) {
                throw new IllegalArgumentException("Modulus, exponent, and encrypted data cannot be null");
            }

            // Create private key
            RSAPrivateKey privateKey_ = createPrivateKeyFromComponents(modulusBase64, privateExponentBase64);

            byte[] encryptedBytes;

            if (isHexEncoded) {
                // Handle hex-encoded ciphertext
                encryptedBytes = hexStringToByteArray(encryptedData);
            } else {
                // Handle Base64-encoded ciphertext
                encryptedBytes = Base64.decode(encryptedData, Base64.NO_WRAP);
            }

            // Decrypt
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey_);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

//            String keyString = hex2ascii(privateKey_.getEncoded());
//            String test = ThreeDES.tdesECBDecrypt(hex2ascii(decryptedBytes),keyString);
//            TRACE.i("TDES DECRYPTION:: "+ test);

           // return new String(decryptedBytes, StandardCharsets.UTF_8);
            return hex2ascii(decryptedBytes);

        } catch (IllegalArgumentException e) {
            System.err.println("Input validation error: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Decryption error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Decrypt large ciphertext by handling it in chunks
     */
    public static String decryptLargeHexCiphertext(String hexCiphertext, RSAPrivateKey privateKey) {
        try {
            byte[] ciphertextBytes = hexStringToByteArray(hexCiphertext);
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            int keySize = privateKey.getModulus().bitLength();
            int blockSize = keySize / 8;
            int maxDecryptSize = blockSize; // RSA decryption block size

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int offset = 0;

            while (offset < ciphertextBytes.length) {
                int length = Math.min(maxDecryptSize, ciphertextBytes.length - offset);
                byte[] decryptedBlock = cipher.doFinal(ciphertextBytes, offset, length);
                outputStream.write(decryptedBlock);
                offset += length;
            }

            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);

        } catch (Exception e) {
            System.err.println("Decryption error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Diagnostic method to check encryption type
     */
    public static void analyzeCiphertext(String hexCiphertext) {
        byte[] ciphertextBytes = hexStringToByteArray(hexCiphertext);
        System.out.println("Ciphertext length: " + ciphertextBytes.length + " bytes");
        System.out.println("Hex string length: " + hexCiphertext.length() + " characters");

        if (ciphertextBytes.length == 32) {
            System.out.println("This looks like AES-256 encrypted data");
        } else if (ciphertextBytes.length == 16) {
            System.out.println("This looks like AES-128 encrypted data");
        } else if (ciphertextBytes.length == 256) {
            System.out.println("This might be RSA-encrypted, but needs 2048-bit key");
        }
    }

    /**
     * AES decryption for hex-encoded ciphertext
     */
    public static String decryptAES(String hexCiphertext, String key) throws Exception {
        byte[] ciphertextBytes = hexStringToByteArray(hexCiphertext);
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);

        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        byte[] decryptedBytes = cipher.doFinal(ciphertextBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    // Getters
    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    public RSAPrivateKey getPrivateKey() {
        return privateKey;
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    // Utility methods for hex conversion (kept for compatibility)
    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02X", b));
        }
        return hexString.toString();
    }

    public static byte[] hexStringToByteArray(String hexString) {

        if (hexString == null || hexString.length() % 2 != 0) {
            throw new IllegalArgumentException("Invalid hex string");
        }

        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Converts byte array to hexadecimal ASCII string
     * @param inp Input byte array
     * @return Hexadecimal string representation
     */
    public static String hex2ascii(byte[] inp) {
        return hex2ascii(inp, inp.length);
    }

    /**
     * Converts byte array to hexadecimal ASCII string
     * @param inp Input byte array
     * @param length Number of bytes to convert
     * @return Hexadecimal string representation
     */
    public static String hex2ascii(byte[] inp, int length) {
        if (inp == null) {
            return "";
        }

        // Validate length
        int actualLength = Math.min(length, inp.length);
        char[] outp = new char[actualLength * 2]; // Each byte becomes 2 hex chars

        for (int index = 0; index < actualLength; index++) {
            // Convert high nibble (4 bits)
            int highNibble = (inp[index] >> 4) & 0x0F;
            if (highNibble > 9) {
                outp[index * 2] = (char) (highNibble + 'A' - 10);
            } else {
                outp[index * 2] = (char) (highNibble + '0');
            }

            // Convert low nibble (4 bits)
            int lowNibble = inp[index] & 0x0F;
            if (lowNibble > 9) {
                outp[(index * 2) + 1] = (char) (lowNibble + 'A' - 10);
            } else {
                outp[(index * 2) + 1] = (char) (lowNibble + '0');
            }
        }

        return new String(outp);
    }

    /**
     * Most efficient Java way using built-in functionality
     */
    public static String bytesToHex(byte[] bytes, String nu) {
        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xFF;
            hexChars[i * 2] = "0123456789ABCDEF".charAt(value >>> 4);
            hexChars[i * 2 + 1] = "0123456789ABCDEF".charAt(value & 0x0F);
        }
        return new String(hexChars);
    }

    /**
     * Convert string to Base64
     * @param text Input string
     * @return Base64 encoded string
     */
    public static String stringToBase64(String text) {
        if (text == null) return null;

        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        return Base64.encodeToString(textBytes, Base64.NO_WRAP);
    }

    /**
     * Convert Base64 back to string
     * @param base64Text Base64 encoded string
     * @return Original string
     */
    public static String base64ToString(String base64Text) {
        if (base64Text == null) return null;

        byte[] decodedBytes = Base64.decode(base64Text, Base64.NO_WRAP);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }
}