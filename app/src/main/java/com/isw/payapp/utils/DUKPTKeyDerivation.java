package com.isw.payapp.utils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.util.Arrays;
public class DUKPTKeyDerivation {

    // Main method to derive session key from IPEK and KSN
    public static SecretKey deriveSessionKey(SecretKey ipek, String ksn) {
        try {
            // Convert KSN from hex string to bytes
            //KeyType
            byte[] ksnBytes = hexStringToByteArray(ksn);

            // Extract current transaction counter from KSN
            long counter = extractCounterFromKSN(ksnBytes);

            // Derive the base derivation key
            SecretKey bdk = deriveBDK(ipek, ksnBytes);

            // Derive the unique key for this transaction
            byte[] derivedKey = deriveKeyFromCounter(bdk, ksnBytes, counter, KeyType.DATA_KEY);

            return new SecretKeySpec(derivedKey, "DESede");

        } catch (Exception e) {
            throw new RuntimeException("Key derivation failed", e);
        }
    }

    // Extract transaction counter from KSN (last 21 bits)
    private static long extractCounterFromKSN(byte[] ksn) {
        // KSN format: [8 bytes] where last 21 bits are the counter
        long counter = ((ksn[7] & 0xFFL) << 16) |
                ((ksn[8] & 0xFFL) << 8) |
                (ksn[9] & 0xFFL);

        // Mask to get only the 21 bits
        return counter & 0x1FFFFF;
    }

    // Derive Base Derivation Key (BDK) from IPEK and KSN
    private static SecretKey deriveBDK(SecretKey ipek, byte[] ksn) throws Exception {
        byte[] ipekBytes = ipek.getEncoded();
        byte[] ksnBase = Arrays.copyOf(ksn, 8); // First 8 bytes of KSN

        // XOR IPEK with KSN base
        byte[] bdkBytes = new byte[16];
        for (int i = 0; i < 8; i++) {
            bdkBytes[i] = (byte) (ipekBytes[i] ^ ksnBase[i]);
            bdkBytes[i + 8] = (byte) (ipekBytes[i + 8] ^ ksnBase[i]);
        }

        return new SecretKeySpec(bdkBytes, "DESede");
    }

    // Derive key using the transaction counter
    private static byte[] deriveKeyFromCounter(SecretKey bdk, byte[] ksn, long counter, KeyType keyType) throws Exception {
        byte[] ksnBase = Arrays.copyOf(ksn, 8); // First 8 bytes

        // Create the derivation data
        byte[] derivationData = new byte[8];
        System.arraycopy(ksnBase, 0, derivationData, 0, 6);

        // Set the counter in the derivation data
        derivationData[6] = (byte) ((counter >> 16) & 0x1F); // 5 bits
        derivationData[7] = (byte) ((counter >> 8) & 0xFF);  // 8 bits
        // Last 8 bits are handled in the loop

        // Generate the key using Triple DES ECB
        Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, bdk);

        byte[] derivedKey = new byte[16];

        // For data key (16 bytes), we need two encryption operations
        if (keyType == KeyType.DATA_KEY) {
            // First 8 bytes
            byte[] block1 = new byte[8];
            System.arraycopy(derivationData, 0, block1, 0, 8);
            block1[7] = (byte) (counter & 0xFF); // Set the last byte

            byte[] encrypted1 = cipher.doFinal(block1);
            System.arraycopy(encrypted1, 0, derivedKey, 0, 8);

            // Second 8 bytes - modify the derivation data
            derivationData[7] = (byte) (derivationData[7] ^ 0xFF); // XOR with FF
            block1[7] = (byte) (counter & 0xFF); // Reset the last byte

            byte[] encrypted2 = cipher.doFinal(block1);
            System.arraycopy(encrypted2, 0, derivedKey, 8, 8);

        } else if (keyType == KeyType.PIN_KEY) {
            // PIN key is 8 bytes
            derivationData[7] = (byte) (counter & 0xFF); // Set the last byte
            derivedKey = cipher.doFinal(derivationData);
            derivedKey = Arrays.copyOf(derivedKey, 8); // Ensure 8 bytes
        }

        return derivedKey;
    }

    // Convert hex string to byte array
    public static byte[] hexStringToByteArray(String hex) {
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
        }

        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    // Convert byte array to hex string
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    // Key types for DUKPT
    public enum KeyType {
        DATA_KEY,    // 16 bytes for data encryption
        PIN_KEY      // 8 bytes for PIN encryption
    }

    // Test method
    public static void main(String[] args) {
        try {
            // Example IPEK (16 bytes hex) - typically injected into devices
            String ipekHex = "0123456789ABCDEFFEDCBA9876543210";
            SecretKey ipek = new SecretKeySpec(hexStringToByteArray(ipekHex), "DESede");

            // Example KSN (10 bytes hex) - from the terminal
            String ksn = "FFFF9876543210E00001";

            System.out.println("IPEK: " + ipekHex);
            System.out.println("KSN: " + ksn);

            // Derive data encryption key
            SecretKey dataKey = deriveSessionKey(ipek, ksn);
            System.out.println("Data Key: " + bytesToHex(dataKey.getEncoded()));

            // Derive PIN encryption key
            SecretKey pinKey = deriveSessionKey(ipek, ksn);
            System.out.println("PIN Key: " + bytesToHex(pinKey.getEncoded()));

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
