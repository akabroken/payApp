package com.isw.payapp.utils;

import android.util.Base64;
import android.util.Log;

import com.dspread.print.util .TRACE;

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

/**
 * RSA Utility class for asymmetric encryption and decryption
 * Provides methods for key generation, encryption, decryption, and key management
 */
public class RSAUtil {
    private static final String TAG = "RSAUtil";

    // Constants
    private static final String RSA_ALGORITHM = "RSA";
    private static final String RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding";
    private static final int DEFAULT_KEY_SIZE = 2048;
    private static final int MIN_KEY_SIZE = 1024;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // Instance fields
    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;

    // Constructors
    public RSAUtil() {
        generateKeyPair(DEFAULT_KEY_SIZE);
    }

    public RSAUtil(int keySize) {
        validateKeySize(keySize);
        generateKeyPair(keySize);
    }

    public RSAUtil(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
        validateKeys(publicKey, privateKey);
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    // Validation methods
    private void validateKeySize(int keySize) {
        if (keySize < MIN_KEY_SIZE) {
            throw new IllegalArgumentException("Key size must be at least " + MIN_KEY_SIZE + " bits");
        }
    }

    private void validateKeys(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
        if (publicKey == null) {
            throw new IllegalArgumentException("Public key cannot be null");
        }
        if (privateKey == null) {
            throw new IllegalArgumentException("Private key cannot be null");
        }

        // Verify modulus matches for key pair
        if (!publicKey.getModulus().equals(privateKey.getModulus())) {
            throw new IllegalArgumentException("Public and private key modulus do not match");
        }
    }

    /**
     * Generate RSA key pair
     * @param keySize Key size in bits (recommended: 2048 or higher)
     */
    public void generateKeyPair(int keySize) {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
            keyPairGenerator.initialize(keySize, SECURE_RANDOM);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            this.publicKey = (RSAPublicKey) keyPair.getPublic();
            this.privateKey = (RSAPrivateKey) keyPair.getPrivate();

            TRACE.i("Generated RSA key pair with size: " + keySize);
        } catch (Exception e) {
            String errorMsg = "Failed to generate RSA key pair with size: " + keySize;
            TRACE.e(errorMsg);
            throw new RuntimeException(errorMsg, e);
        }
    }

    // Encryption methods
    public String encrypt(String plaintext) throws Exception {
        validateInitialization();
        return encrypt(plaintext, publicKey);
    }

    public String encrypt(String plaintext, PublicKey publicKey) throws Exception {
        validateInput(plaintext, "Plaintext");
        validateKey(publicKey, "Public key");

        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP);
    }

    // Decryption methods
    public String decrypt(String ciphertext) throws Exception {
        validateInitialization();
        return decrypt(ciphertext, privateKey);
    }

    public String decrypt(String ciphertext, PrivateKey privateKey) throws Exception {
        validateInput(ciphertext, "Ciphertext");
        validateKey(privateKey, "Private key");

        byte[] ciphertextBytes = Base64.decode(ciphertext, Base64.NO_WRAP);
        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = cipher.doFinal(ciphertextBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * Get public and private key components as Base64 strings
     */
    public Map<String, String> getKeyComponents() {
        validateInitialization();

        Map<String, String> components = new HashMap<>();
        components.put("modulus", Base64.encodeToString(publicKey.getModulus().toByteArray(), Base64.NO_WRAP));
        components.put("exponent", Base64.encodeToString(publicKey.getPublicExponent().toByteArray(), Base64.NO_WRAP));
        components.put("privateModulus", Base64.encodeToString(privateKey.getModulus().toByteArray(), Base64.NO_WRAP));
        components.put("privateExponent", Base64.encodeToString(privateKey.getPrivateExponent().toByteArray(), Base64.NO_WRAP));

        return components;
    }

    // Key creation from components
    public static RSAPublicKey createPublicKeyFromComponents(String modulusBase64, String exponentBase64) throws Exception {
        validateBase64Input(modulusBase64, "Modulus");
        validateBase64Input(exponentBase64, "Exponent");

        byte[] modBytes = Base64.decode(modulusBase64, Base64.NO_WRAP);
        byte[] expBytes = Base64.decode(exponentBase64, Base64.NO_WRAP);

        BigInteger modulus = new BigInteger(1, modBytes);
        BigInteger exponent = new BigInteger(1, expBytes);

        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        return (RSAPublicKey) keyFactory.generatePublic(keySpec);
    }

    public static RSAPrivateKey createPrivateKeyFromComponents(String modulusBase64, String privateExponentBase64) throws Exception {
        validateBase64Input(modulusBase64, "Modulus");
        validateBase64Input(privateExponentBase64, "Private exponent");

        byte[] modBytes = Base64.decode(modulusBase64, Base64.NO_WRAP);
        byte[] expBytes = Base64.decode(privateExponentBase64, Base64.NO_WRAP);

        BigInteger modulus = new BigInteger(1, modBytes);
        BigInteger privateExponent = new BigInteger(1, expBytes);

        RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(modulus, privateExponent);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    }

    // Key export/import methods
    public String exportPublicKey() {
        validateInitialization();
        return Base64.encodeToString(publicKey.getEncoded(), Base64.NO_WRAP);
    }

    public static RSAPublicKey importPublicKey(String publicKeyBase64) throws Exception {
        validateBase64Input(publicKeyBase64, "Public key");

        byte[] keyBytes = Base64.decode(publicKeyBase64, Base64.NO_WRAP);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        return (RSAPublicKey) keyFactory.generatePublic(keySpec);
    }

    public String exportPrivateKey() {
        validateInitialization();
        return Base64.encodeToString(privateKey.getEncoded(), Base64.NO_WRAP);
    }

    public static RSAPrivateKey importPrivateKey(String privateKeyBase64) throws Exception {
        validateBase64Input(privateKeyBase64, "Private key");

        byte[] keyBytes = Base64.decode(privateKeyBase64, Base64.NO_WRAP);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    }

    /**
     * Decrypt hex-encoded ciphertext using private key
     */
    public static String decryptHexCiphertext(String hexCiphertext, RSAPrivateKey privateKey) {
        try {
            validateInput(hexCiphertext, "Hex ciphertext");
            validateKey(privateKey, "Private key");

            TRACE.d("Hex ciphertext length: " + hexCiphertext.length());

            byte[] ciphertextBytes = hexStringToByteArray(hexCiphertext);
            TRACE.d("Ciphertext byte length: " + ciphertextBytes.length);

            Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            byte[] decryptedBytes = cipher.doFinal(ciphertextBytes);
            TRACE.d("Decrypted byte length: " + decryptedBytes.length);

            // Convert decrypted bytes to string and then to hex ASCII representation
            String decryptedString = new String(decryptedBytes, StandardCharsets.UTF_8);
            return bytesToHex(decryptedString.getBytes(StandardCharsets.UTF_8));

        } catch (Exception e) {
            TRACE.e("Decryption error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Comprehensive decryption method with key components
     */
    public  String decryptWithKeyComponents(String encryptedData, String modulusBase64,
                                                  String privateExponentBase64, boolean isHexEncoded) {
        try {
            validateInput(encryptedData, "Encrypted data");
            validateBase64Input(modulusBase64, "Modulus");
            validateBase64Input(privateExponentBase64, "Private exponent");

            RSAPrivateKey privateKey = createPrivateKeyFromComponents(modulusBase64, privateExponentBase64);
            byte[] encryptedBytes;

            if (isHexEncoded) {
                encryptedBytes = hexStringToByteArray(encryptedData);
            } else {
                encryptedBytes = Base64.decode(encryptedData, Base64.NO_WRAP);
            }

            Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

            TRACE.d("Decrypted Value::"+bytesToHex(decryptedBytes));

            return bytesToHex(decryptedBytes);


        } catch (Exception e) {
            TRACE.e("Decryption with key components error: " + e.getMessage());
            return null;
        }
    }

    // Utility methods
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "";

        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xFF;
            hexChars[i * 2] = "0123456789ABCDEF".charAt(value >>> 4);
            hexChars[i * 2 + 1] = "0123456789ABCDEF".charAt(value & 0x0F);
        }
        return new String(hexChars);
    }

    public static byte[] hexStringToByteArray(String hexString) {
        validateInput(hexString, "Hex string");

        if (hexString.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
        }

        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }

    public static String stringToBase64(String text) {
        if (text == null) return null;
        return Base64.encodeToString(text.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
    }

    public static String base64ToString(String base64Text) {
        if (base64Text == null) return null;
        byte[] decodedBytes = Base64.decode(base64Text, Base64.NO_WRAP);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }

    // Validation helper methods
    private void validateInitialization() {
        if (publicKey == null || privateKey == null) {
            throw new IllegalStateException("RSA keys not properly initialized");
        }
    }

    private static void validateInput(String input, String fieldName) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
    }

    private static void validateKey(PublicKey key, String keyName) {
        if (key == null) {
            throw new IllegalArgumentException(keyName + " cannot be null");
        }
    }

    private static void validateKey(PrivateKey key, String keyName) {
        if (key == null) {
            throw new IllegalArgumentException(keyName + " cannot be null");
        }
    }

    private static void validateBase64Input(String input, String fieldName) {
        validateInput(input, fieldName);
        try {
            Base64.decode(input, Base64.NO_WRAP);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(fieldName + " is not valid Base64");
        }
    }

    // Getters
    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    public RSAPrivateKey getPrivateKey() {
        return privateKey;
    }

    public KeyPair getKeyPair() {
        return new KeyPair(publicKey, privateKey);
    }


    /// IPEK EXTRACTION

    /**
     * Enhanced decryption method for IPEK key derivation
     */
    public static String decryptHexCiphertextForIPEK(String hexCiphertext, RSAPrivateKey privateKey) {
        try {
            validateInput(hexCiphertext, "Hex ciphertext");
            validateKey(privateKey, "Private key");

            TRACE.d("Hex ciphertext length: " + hexCiphertext.length());

            byte[] ciphertextBytes = hexStringToByteArray(hexCiphertext);
            TRACE.d("Ciphertext byte length: " + ciphertextBytes.length);

            Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            byte[] decryptedBytes = cipher.doFinal(ciphertextBytes);
            TRACE.d("Decrypted byte length: " + decryptedBytes.length);

            // The decrypted data might be ASN.1 encoded or contain the IPEK wrapped
            String decryptedHex = bytesToHex(decryptedBytes);
            TRACE.d("Raw decrypted hex: " + decryptedHex);

            // Try to extract IPEK from different possible formats
            String ipek = extractIPEK(decryptedBytes);
            if (ipek != null) {
                return ipek;
            }

            // If extraction fails, return the raw hex for manual inspection
            return decryptedHex;

        } catch (Exception e) {
            e.printStackTrace();
            TRACE.e("IPEK decryption error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extract IPEK from decrypted data in various formats
     */
    private static String extractIPEK(byte[] decryptedData) {
        if (decryptedData == null || decryptedData.length == 0) {
            return null;
        }

        TRACE.d("Attempting IPEK extraction from " + decryptedData.length + " bytes");

        // Method 1: Direct IPEK extraction (if data contains the key directly)
        if (decryptedData.length >= 8) {
            // Try to find the IPEK pattern (8 bytes) in various positions
            String possibleIPEK = extractPossibleIPEK(decryptedData);
            if (possibleIPEK != null) {
                TRACE.i("IPEK found via direct extraction: " + possibleIPEK);
                return possibleIPEK;
            }
        }

        // Method 2: ASN.1 parsing (common for encrypted keys)
        String asn1IPEK = parseASN1ForIPEK(decryptedData);
        if (asn1IPEK != null) {
            TRACE.i("IPEK found via ASN.1 parsing: " + asn1IPEK);
            return asn1IPEK;
        }

        // Method 3: Try to decode as UTF-8 and extract hex
        String stringExtracted = extractIPEKFromString(decryptedData);
        if (stringExtracted != null) {
            TRACE.i("IPEK found via string extraction: " + stringExtracted);
            return stringExtracted;
        }

        TRACE.w("Could not extract IPEK from decrypted data");
        return null;
    }

    /**
     * Extract possible IPEK by looking for 8-byte sequences
     */
    private static String extractPossibleIPEK(byte[] data) {
        // IPEK is typically 8 bytes (16 hex chars)
        // Look for the expected pattern or valid key bytes

        // If data is exactly 8 bytes, it might be the IPEK itself
        if (data.length == 8) {
            String ipek = bytesToHex(data);
            TRACE.d("Data is exactly 8 bytes, treating as IPEK: " + ipek);
            return ipek;
        }

        // Look for common IPEK patterns or positions
        // Sometimes IPEK is at the beginning or end of the decrypted data
        if (data.length > 8) {
            // Try first 8 bytes
            byte[] first8 = new byte[8];
            System.arraycopy(data, 0, first8, 0, 8);
            String first8Hex = bytesToHex(first8);
            TRACE.d("First 8 bytes: " + first8Hex);

            // Try last 8 bytes
            byte[] last8 = new byte[8];
            System.arraycopy(data, data.length - 8, last8, 0, 8);
            String last8Hex = bytesToHex(last8);
            TRACE.d("Last 8 bytes: " + last8Hex);

            // Check if either matches your expected pattern
            if (isValidIPEK(first8Hex) && isValidIPEK(last8Hex)) {
                return first8Hex+last8Hex;
            }
            if (isValidIPEK(last8Hex)) {
                return last8Hex;
            }
        }

        return null;
    }

    /**
     * Parse ASN.1 encoded data to extract IPEK
     */
    private static String parseASN1ForIPEK(byte[] data) {
        try {
            // Simple ASN.1 parsing for common structures
            // Your data starts with 30 1C which is ASN.1 SEQUENCE tag
            if (data.length > 2 && data[0] == 0x30) {
                TRACE.d("ASN.1 SEQUENCE detected, attempting parsing");

                // For complex ASN.1 structures, you might need more sophisticated parsing
                // This is a simplified approach

                // Look for octet strings or embedded key data
                for (int i = 0; i < data.length - 8; i++) {
                    // Check if next 8 bytes could be a key
                    byte[] potentialKey = new byte[8];
                    System.arraycopy(data, i, potentialKey, 0, 8);
                    String potentialKeyHex = bytesToHex(potentialKey);

                    if (isValidIPEK(potentialKeyHex)) {
                        TRACE.d("Found potential IPEK at position " + i + ": " + potentialKeyHex);
                        return potentialKeyHex;
                    }
                }
            }
        } catch (Exception e) {
            TRACE.w("ASN.1 parsing failed: " + e.getMessage());
        }
        return null;
    }

    /**
     * Extract IPEK by interpreting data as string
     */
    private static String extractIPEKFromString(byte[] data) {
        try {
            String asString = new String(data, StandardCharsets.UTF_8);
            TRACE.d("Data as string: " + asString);

            // Look for hex patterns in the string
            // This might contain the IPEK in hexadecimal representation
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("[0-9A-Fa-f]{16}");
            java.util.regex.Matcher matcher = pattern.matcher(asString);

            if (matcher.find()) {
                String foundHex = matcher.group().toUpperCase();
                TRACE.d("Found hex pattern in string: " + foundHex);
                if (isValidIPEK(foundHex)) {
                    return foundHex;
                }
            }
        } catch (Exception e) {
            TRACE.w("String extraction failed: " + e.getMessage());
        }
        return null;
    }

    /**
     * Validate if the extracted string is a likely IPEK
     */
    private static boolean isValidIPEK(String hexString) {
        if (hexString == null || hexString.length() != 16) {
            return false;
        }

        // Check if it's valid hex
        if (!hexString.matches("[0-9A-Fa-f]{16}")) {
            return false;
        }

        // Add any specific validation for your expected IPEK pattern
        // For example, check if it contains your expected value
        if (hexString.toUpperCase().contains("33707E4927C4A0D5")) {
            TRACE.i("Found expected IPEK pattern!");
            return true;
        }

        // Additional validation can be added based on your specific requirements
        return true;
    }

    /**
     * Enhanced decryption with multiple output formats for debugging
     */
    public static Map<String, String> decryptHexCiphertextDetailed(String hexCiphertext, RSAPrivateKey privateKey) {
        Map<String, String> results = new HashMap<>();

        try {
            validateInput(hexCiphertext, "Hex ciphertext");
            validateKey(privateKey, "Private key");

            byte[] ciphertextBytes = hexStringToByteArray(hexCiphertext);
            Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedBytes = cipher.doFinal(ciphertextBytes);

            // Store various representations for analysis
            results.put("rawHex", bytesToHex(decryptedBytes));
            results.put("base64", Base64.encodeToString(decryptedBytes, Base64.NO_WRAP));

            try {
                results.put("asString", new String(decryptedBytes, StandardCharsets.UTF_8));
            } catch (Exception e) {
                results.put("asString", "Invalid UTF-8");
            }

            // Try IPEK extraction
            String ipek = extractIPEK(decryptedBytes);
            results.put("extractedIPEK", ipek != null ? ipek : "Not found");

            return results;

        } catch (Exception e) {
            results.put("error", "Decryption failed: " + e.getMessage());
            return results;
        }
    }
}