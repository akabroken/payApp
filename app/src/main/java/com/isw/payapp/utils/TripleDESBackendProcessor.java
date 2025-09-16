package com.isw.payapp.utils;

import android.util.Log;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.security.spec.KeySpec;
public class TripleDESBackendProcessor {
    // Backend method to derive 3DES key from C0 value
    public  SecretKey deriveKeyFromC0(String c0Value) throws Exception {
        // Assuming C0 value is a base64 encoded key or similar
        // You might need to adjust this based on your actual C0 format
        byte[] keyBytes = Base64.getDecoder().decode(c0Value);

        Log.d("TTT", "Le: "+keyBytes.length);
        byte[] combined = new byte[24];//new byte[DataProcessUtil.hexStringToByte(c0Value).length];
        System.arraycopy(DataProcessUtil.hexStringToByte(c0Value), 0, combined, 0, DataProcessUtil.hexStringToByte(c0Value).length);
        byte[] dataKey = new byte[24]; // 3DES requires 24 bytes
        for (int i = 0; i < 24; i++) {
            dataKey[i] = (byte) (combined[i % combined.length] ^ (i + 1));
        }
        // Ensure the key is the correct length for 3DES (24 bytes)
//        if (keyBytes.length != 24) {
//            throw new IllegalArgumentException("Key must be 24 bytes for 3DES");
//        }

        KeySpec keySpec = new DESedeKeySpec(dataKey);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DESede");
        return keyFactory.generateSecret(keySpec);
    }

    // Backend method to decrypt C2 value using the derived key
    public  String decryptC2Value(String encryptedC2, SecretKey key) throws Exception {
        // Assuming C2 is base64 encoded
        byte[] encryptedData = Base64.getDecoder().decode(encryptedC2);

        Cipher cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);

        byte[] decryptedData = cipher.doFinal(encryptedData);
        return new String(decryptedData, StandardCharsets.UTF_8);
    }

    // Complete backend processing method
    public  String processOnlineMessage(String c0Value, String c2Value) {
        try {
            // Step 1: Derive key from C0
            SecretKey key = deriveKeyFromC0(c0Value);

            // Step 2: Decrypt C2 using the derived key
            String plainText = decryptC2Value(c2Value, key);

            return plainText;

        } catch (Exception e) {
            throw new RuntimeException("Failed to process online message", e);
        }
    }

    // Example usage
//    public static void main(String[] args) {
//        // These values would come from the frontend/app
//        String c0Value = "your-base64-encoded-key-here"; // 24 bytes base64 encoded
//        String c2Value = "encrypted-base64-data-here";   // Encrypted message
//
//        try {
//            String result = processOnlineMessage(c0Value, c2Value);
//            System.out.println("Decrypted message: " + result);
//        } catch (Exception e) {
//            System.err.println("Error: " + e.getMessage());
//        }
//    }
}
