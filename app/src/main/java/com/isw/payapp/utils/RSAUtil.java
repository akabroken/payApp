package com.isw.payapp.utils;

import android.content.Context;
import android.util.Base64;


import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class RSAUtil {

    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;
    private KeyPair keyPair;
    private KeyPairGenerator keyPairGenerator;
    private String dataToEncDec;
    private Context context;


    public RSAUtil(){}


    public RSAUtil(RSAPublicKey publicKey, RSAPrivateKey privateKey, KeyPair keyPair, KeyPairGenerator keyPairGenerator) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.keyPair = keyPair;
        this.keyPairGenerator = keyPairGenerator;

    }


    public String GetRsaEnc(String originalMessage) throws Exception {
        String encryptedMessage = "";
        keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048); // You can adjust the key size as needed
        keyPair = keyPairGenerator.generateKeyPair();
        publicKey = (RSAPublicKey) keyPair.getPublic();
        privateKey = (RSAPrivateKey) keyPair.getPrivate();
        encryptedMessage = encrypt(originalMessage, publicKey);
        return encryptedMessage;
    }

    public String GetRsaDec(String encryptedMessage) throws Exception {
        String decryptedMessage = "";
        keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048); // You can adjust the key size as needed
        keyPair = keyPairGenerator.generateKeyPair();
        publicKey = (RSAPublicKey) keyPair.getPublic();
        privateKey = (RSAPrivateKey) keyPair.getPrivate();
        decryptedMessage = decrypt(encryptedMessage, privateKey);
        return decryptedMessage;
    }

    private String encrypt(String plaintext, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes());
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
    }

    private String decrypt(String ciphertext, PrivateKey privateKey) throws Exception {
        byte[] ciphertextBytes = Base64.decode(ciphertext, Base64.DEFAULT);
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = cipher.doFinal(ciphertextBytes);
        return new String(decryptedBytes);
    }

    /////////////////--------MODULO AND EXPONENTS-------------///////

    public Map GetRsaEnc(String originalMessage, String tag) throws Exception {
        String encryptedMessage = "";
        Map<String,Object> pkModExp = new HashMap<>();
        keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(1024); // You can adjust the key size as needed
        keyPair = keyPairGenerator.generateKeyPair();
        publicKey = (RSAPublicKey) keyPair.getPublic();
        privateKey = (RSAPrivateKey) keyPair.getPrivate();
        String pkMod = Base64.encodeToString(publicKey.getModulus().toByteArray(), Base64.DEFAULT);
        String pkExp = Base64.encodeToString(publicKey.getPublicExponent().toByteArray(), Base64.DEFAULT);
        pkModExp.put("pkMod",pkMod);
        pkModExp.put("pkExp",pkExp);
        //encryptedMessage = encrypt(originalMessage, publicKey.getModulus(), publicKey.getPublicExponent());
        encryptedMessage = Base64.encodeToString(publicKey.getModulus().toByteArray(), Base64.DEFAULT)+"T"+Base64.encodeToString(publicKey.getPublicExponent().toByteArray(), Base64.DEFAULT);
        return pkModExp;
    }

    public List<Object> GetRsaEnc() throws Exception {
        String encryptedMessage = "";
        List<Object> pkModExp = new ArrayList<>();
        keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(1024); // You can adjust the key size as needed
        keyPair = keyPairGenerator.generateKeyPair();
        publicKey = (RSAPublicKey) keyPair.getPublic();
        privateKey = (RSAPrivateKey) keyPair.getPrivate();
        String pkMod = Base64.encodeToString(publicKey.getModulus().toByteArray(), Base64.NO_WRAP);
        String pkExp = Base64.encodeToString(publicKey.getPublicExponent().toByteArray(), Base64.NO_WRAP);
        pkModExp.add(pkMod);
        pkModExp.add(pkExp);
        pkModExp.add(privateKey);
        return pkModExp;
    }

    public String GetRsaDec(String encryptedMessage, RSAPrivateKey privateKey, String tag) throws Exception {
        return decrypt(encryptedMessage, privateKey);
    }

    private String encrypt(String plaintext, BigInteger publicModulus, BigInteger publicExponent) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(publicModulus, publicExponent);
        RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(publicKeySpec);

        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes());
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
    }

    public static String decrypt(String hexCiphertext, RSAPrivateKey privateKey) {
        try {
            // Convert the hexadecimal ciphertext to a byte array
            byte[] ciphertextBytes = hexStringToByteArray(hexCiphertext);
            Cipher cipher =   Cipher.getInstance("RSA/ECB/PKCS1Padding"); //Cipher.getInstance("RSA/ECB/PKCS1Padding"); // Ensure the padding matches the encryption

            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            // Decrypt the ciphertext
            byte[] decryptedBytes = cipher.doFinal(ciphertextBytes);

            // Convert the decrypted bytes to a string
           //return new String(decryptedBytes, StandardCharsets.UTF_8);

            return bytesToHex(decryptedBytes);
        } catch (Exception e) {
            System.err.println("Decryption error: " + e.getMessage());
            return null;
        }
    }

    public static String hexToAscii(String hexStr) {
        StringBuilder output = new StringBuilder();

        // Process 2 hex characters (1 byte) at a time
        for (int i = 0; i < hexStr.length(); i += 2) {
            String byteStr = hexStr.substring(i, i + 2);
            int decimal = Integer.parseInt(byteStr, 16); // convert hex → decimal
            output.append((char) decimal); // decimal → ASCII char
        }

        return output.toString();
    }

    public static byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String bytesToHex(byte[] bytes, String t) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02X", b));
        }
        return hexString.toString();
    }
    public  String hexToBinary(String hexString) {
        StringBuilder binaryStringBuilder = new StringBuilder();

        for (int i = 0; i < hexString.length(); i++) {
            char hexChar = hexString.charAt(i);
            int hexValue = Character.digit(hexChar, 16);

            // Append 4-bit binary representation of the hex value
            binaryStringBuilder.append(String.format("%4s", Integer.toBinaryString(hexValue))
                    .replace(' ', '0'));
        }

        return binaryStringBuilder.toString();
    }

}
