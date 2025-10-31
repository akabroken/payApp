package com.isw.payapp.devices.dspread.utils;

import android.util.Log;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
public class PinBlockISO0Dukpt {

    /**
     * Recreates ISO-0 PIN block using DUKPT (KSN and IPEK)
     *
     * @param encryptedPinBlock The encrypted ISO-0 PIN block (hex string)
     * @param pan The Primary Account Number
     * @param ksn The Key Serial Number (hex string)
     * @param ipek The Initial PIN Encryption Key (hex string)
     * @return Recreated ISO-0 PIN block (hex string)
     */
    public static String recreateISOPinBlockWithDukpt(String encryptedPinBlock, String pan, String ksn, String ipek) {
        Log.d("ipek]]]]",ipek);
        try {
            // Step 1: Derive current session key from IPEK and KSN
            String sessionKey = deriveSessionKey(ipek, ksn);

            // Step 2: Decrypt the PIN block to get clear PIN
            String clearPin = decryptPinFromPinBlockWithDukpt(encryptedPinBlock, pan, sessionKey);

            System.out.println("Decrypted PIN: " + clearPin);
            System.out.println("Session Key: " + sessionKey);

            // Step 3: Recreate ISO-0 PIN block with the decrypted PIN
            String recreatedPinBlock = buildISOPinBlock(clearPin, pan);

            // Step 4: Encrypt the recreated PIN block with the same session key
            String encryptedRecreatedPinBlock = encryptPinBlockWithDukpt(recreatedPinBlock, sessionKey);

            return encryptedRecreatedPinBlock;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error recreating PIN block with DUKPT: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypts PIN from ISO-0 PIN block using DUKPT session key
     */
    public static String decryptPinFromPinBlockWithDukpt(String encryptedPinBlock, String pan, String sessionKey) {
        try {
            // Step 1: Decrypt the PIN block using session key
            String decryptedPinBlock = decryptAES(encryptedPinBlock, sessionKey);

            // Step 2: Create PAN block
            String panBlock = createPanBlock(pan);

            // Step 3: XOR to get clear PIN block
            String clearPinBlock = xorHexStrings(decryptedPinBlock, panBlock);

            // Step 4: Extract PIN from clear PIN block
            return extractPinFromClearPinBlock(clearPinBlock);

        } catch (Exception e) {
            throw new RuntimeException("Error decrypting PIN with DUKPT: " + e.getMessage(), e);
        }
    }

    /**
     * Derives session key from IPEK and KSN using DUKPT
     */
    public static String deriveSessionKey(String ipek, String ksn) {
        try {
            // For simplicity, using a basic DUKPT derivation
            // In production, use a proper DUKPT library like jPOS, BouncyCastle, or vendor SDK

            // Convert IPEK and KSN to byte arrays
            byte[] ipekBytes = hexToBytes(ipek);
            byte[] ksnBytes = hexToBytes(ksn);

            // Basic DUKPT derivation (simplified - replace with proper implementation)
            String derivedKey = simpleDukptDerivation(ipekBytes, ksnBytes);

            return derivedKey;

        } catch (Exception e) {
            throw new RuntimeException("DUKPT key derivation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Simplified DUKPT derivation - REPLACE WITH PROPER DUKPT IMPLEMENTATION
     */
    private static String simpleDukptDerivation(byte[] ipek, byte[] ksn) {
        try {
            // This is a simplified version. In production, use:
            // - jPOS DUKPT implementation
            // - BouncyCastle
            // - Thales DUKPT library
            // - Your payment terminal vendor's SDK

            // Example using XOR for demonstration (NOT SECURE FOR PRODUCTION)
            byte[] derived = new byte[ipek.length];
            for (int i = 0; i < ipek.length; i++) {
                derived[i] = (byte) (ipek[i] ^ ksn[i % ksn.length]);
            }

            // For PIN encryption, we typically use the data key part
            // Extract 16 bytes for AES-128
            byte[] sessionKey = new byte[16];
            System.arraycopy(derived, 0, sessionKey, 0, Math.min(derived.length, 16));

            return bytesToHex(sessionKey);

        } catch (Exception e) {
            throw new RuntimeException("DUKPT derivation error: " + e.getMessage(), e);
        }
    }

    /**
     * Proper DUKPT implementation using jPOS (if available)
     */
    private static String jposDukptDerivation(String ipek, String ksn) {
        try {
            /*
            // If you have jPOS in your project:
            DUKPT dukpt = new DUKPT();
            KeySerialNumber ksnObj = new KeySerialNumber(hexToBytes(ksn));
            BaseKey ipekKey = new BaseKey(hexToBytes(ipek), "IPEK");
            Key dataKey = dukpt.deriveKey(ipekKey, ksnObj, KeyType.DATA_ENCRYPTION);
            return bytesToHex(dataKey.getKey());
            */

            // Placeholder - implement with your DUKPT library
            throw new UnsupportedOperationException("Implement with proper DUKPT library");

        } catch (Exception e) {
            throw new RuntimeException("jPOS DUKPT derivation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Builds ISO-0 format PIN block (clear)
     */
    public static String buildISOPinBlock(String pin, String pan) {
        // Validate inputs
        if (pin == null || pin.isEmpty()) {
            throw new IllegalArgumentException("PIN cannot be null or empty");
        }
        if (pan == null || pan.isEmpty()) {
            throw new IllegalArgumentException("PAN cannot be null or empty");
        }

        // Clean PAN - remove any non-digit characters
        pan = pan.replaceAll("\\D", "");

        // Validate PIN
        if (!pin.matches("\\d+")) {
            throw new IllegalArgumentException("PIN must contain only digits");
        }

        int pinLength = pin.length();
        if (pinLength < 4 || pinLength > 12) {
            throw new IllegalArgumentException("PIN must be between 4 and 12 digits");
        }

        // Create PIN block
        String pinBlock = createPinBlock(pin);

        // Create PAN block
        String panBlock = createPanBlock(pan);

        // XOR the two blocks to get ISO-0 PIN block
        return xorHexStrings(pinBlock, panBlock);
    }

    /**
     * Creates the PIN block portion for ISO-0 format
     */
    private static String createPinBlock(String pin) {
        int pinLength = pin.length();

        // Control field: 0 + PIN length in hex
        String controlField = "0" + Integer.toHexString(pinLength);

        StringBuilder pinBlock = new StringBuilder();
        pinBlock.append(controlField);
        pinBlock.append(pin);

        // Fill to 16 characters (8 bytes) with 'F'
        while (pinBlock.length() < 16) {
            pinBlock.append("F");
        }

        return pinBlock.toString();
    }

    /**
     * Creates the PAN block portion for ISO-0 format
     */
    private static String createPanBlock(String pan) {
        // Take last 12 digits of PAN (excluding check digit if present)
        String relevantPan;
        if (pan.length() >= 13) {
            // Take last 13 digits, then remove the last digit (check digit) to get 12 digits
            relevantPan = pan.substring(pan.length() - 13, pan.length() - 1);
        } else {
            // Pad with zeros from the left to make 12 digits
            relevantPan = String.format("%012d", Long.parseLong(pan));
        }

        // PAN block format: 0000 + 12 PAN digits
        return "0000" + relevantPan;
    }

    /**
     * Encrypts PIN block using DUKPT session key
     */
    public static String encryptPinBlockWithDukpt(String clearPinBlock, String sessionKey) throws Exception {
        return encryptAES(clearPinBlock, sessionKey);
    }

    /**
     * Extracts PIN from clear PIN block
     */
    private static String extractPinFromClearPinBlock(String clearPinBlock) {
        // ISO-0 format: First nibble is format (0), second nibble is PIN length
        int pinLength = Character.digit(clearPinBlock.charAt(1), 16);

        // Extract PIN digits (positions 2 to pinLength+1)
        StringBuilder pin = new StringBuilder();
        for (int i = 2; i < 2 + pinLength; i++) {
            char digit = clearPinBlock.charAt(i);
            if (digit >= '0' && digit <= '9') {
                pin.append(digit);
            } else {
                throw new IllegalArgumentException("Invalid PIN digit in PIN block");
            }
        }

        return pin.toString();
    }

    /**
     * AES Encryption (ECB mode, No padding)
     */
    private static String encryptAES(String dataHex, String keyHex) throws Exception {
        byte[] keyBytes = hexToBytes(keyHex);
        byte[] dataBytes = hexToBytes(dataHex);

        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);

        byte[] encryptedBytes = cipher.doFinal(dataBytes);
        return bytesToHex(encryptedBytes);
    }

    /**
     * AES Decryption (ECB mode, No padding)
     */
    private static String decryptAES(String encryptedHex, String keyHex) throws Exception {
        byte[] keyBytes = hexToBytes(keyHex);
        byte[] encryptedBytes = hexToBytes(encryptedHex);

        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);

        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return bytesToHex(decryptedBytes);
    }

    /**
     * Performs XOR operation on two hexadecimal strings
     */
    private static String xorHexStrings(String hex1, String hex2) {
        if (hex1.length() != hex2.length()) {
            throw new IllegalArgumentException("Hex strings must be of equal length");
        }

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < hex1.length(); i++) {
            int char1 = Character.digit(hex1.charAt(i), 16);
            int char2 = Character.digit(hex2.charAt(i), 16);
            int xorResult = char1 ^ char2;
            result.append(Integer.toHexString(xorResult));
        }

        return result.toString();
    }

    /**
     * Converts byte array to hex string
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Converts hex string to byte array
     */
    public static byte[] hexToBytes(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }

    // Test method with DUKPT parameters
    public static void main(String[] args) {
        try {
            // Test data with DUKPT parameters
            String testPin = "1234";
            String testPan = "1234567890123456";
            String testKsn = "FFFF000002DDDDE00000";
            String testIpek = "C4259D858624327B6D89047D86252006";

            System.out.println("=== DUKPT PIN Block Recreation ===");
            System.out.println("PIN: " + testPin);
            System.out.println("PAN: " + testPan);
            System.out.println("KSN: " + testKsn);
            System.out.println("IPEK: " + testIpek);

            // First, create an initial encrypted PIN block
            String sessionKey = deriveSessionKey(testIpek, testKsn);
            String clearPinBlock = buildISOPinBlock(testPin, testPan);
            String encryptedPinBlock = encryptPinBlockWithDukpt(clearPinBlock, sessionKey);

            System.out.println("\nOriginal Encrypted PIN Block: " + encryptedPinBlock);

            // Now recreate using DUKPT parameters
            String recreatedPinBlock = recreateISOPinBlockWithDukpt(
                    encryptedPinBlock, testPan, testKsn, testIpek
            );

            System.out.println("Recreated PIN Block: " + recreatedPinBlock);

            System.out.println("\n=== Verification ===");
            System.out.println("Blocks match: " + encryptedPinBlock.equals(recreatedPinBlock));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
