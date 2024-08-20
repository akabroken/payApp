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

public class RSAUtil {

    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;
    private KeyPair keyPair;
    private KeyPairGenerator keyPairGenerator;
    private String dataToEncDec;
    private Context context;



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

    public List<String> GetRsaEnc() throws Exception {
        String encryptedMessage = "";
        List<String> pkModExp = new ArrayList<>();
        keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048); // You can adjust the key size as needed
        keyPair = keyPairGenerator.generateKeyPair();
        publicKey = (RSAPublicKey) keyPair.getPublic();
        privateKey = (RSAPrivateKey) keyPair.getPrivate();
        String pkMod = Base64.encodeToString(publicKey.getModulus().toByteArray(), Base64.DEFAULT);
        String pkExp = Base64.encodeToString(publicKey.getPublicExponent().toByteArray(), Base64.DEFAULT);
        pkModExp.add(pkMod);
        pkModExp.add(pkExp);
        //encryptedMessage = encrypt(originalMessage, publicKey.getModulus(), publicKey.getPublicExponent());
        encryptedMessage = Base64.encodeToString(publicKey.getModulus().toByteArray(), Base64.DEFAULT)+"T"+Base64.encodeToString(publicKey.getPublicExponent().toByteArray(), Base64.DEFAULT);
        return pkModExp;
    }

    public String GetRsaDec(String encryptedMessage, String tag) throws Exception {
        String decryptedMessage = "";
        keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048); // You can adjust the key size as needed
        keyPair = keyPairGenerator.generateKeyPair();
        publicKey = (RSAPublicKey) keyPair.getPublic();
        privateKey = (RSAPrivateKey) keyPair.getPrivate();
        decryptedMessage = decrypt(encryptedMessage, privateKey.getModulus(), privateKey.getPrivateExponent());
        return decryptedMessage;
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

    public String decrypt(String ciphertext, BigInteger privateModulus, BigInteger privateExponent) {
        try{
            //byte[] ciphertextBytes = hexStringToByteArray(ciphertext);
            HexConverter hexConverter = new HexConverter();
          //  byte[] ciphertextBytes = hexConverter.fromHex2ByteArray(ciphertext.getBytes());
           // byte[] ciphertextBytes = hexConverter.fromHex2ByteArray(ciphertext.getBytes());//.getBytes(StandardCharsets.UTF_8);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPrivateKeySpec privateKeySpec = new RSAPrivateKeySpec(privateModulus, privateExponent);
            RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(privateKeySpec);
            System.out.println("Test val + "+ hexConverter.fromHex2Binary(ciphertext.getBytes()));
            byte[] ciphertextBytes = Base64.decode(ciphertext , Base64.DEFAULT);
           // byte[] ciphertextBytes = Base64.getDecoder
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedBytes = cipher.doFinal(ciphertextBytes);
            System.out.println("TEST "+ new String(decryptedBytes,StandardCharsets.UTF_8));
//            String out_key = hexConverter.fr(decryptedBytes);
//            return out_key;
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        }catch (IllegalArgumentException e){
            System.err.println("Error decoding Base64: " + e.getMessage());
        }catch (Exception e){
            System.err.println("Error decoding String: " + e.getMessage());
        }
       return "NO Data Available";
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
