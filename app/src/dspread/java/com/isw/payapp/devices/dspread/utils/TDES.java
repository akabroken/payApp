package com.isw.payapp.devices.dspread.utils;

import android.os.RemoteException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import com.dspread.print.util.TRACE;

public class TDES {

    public static byte[] desEncypt(byte[] keyBytes,byte[] input) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        SecretKeySpec key = new SecretKeySpec(keyBytes, "DES");
        Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding", new BouncyCastleProvider());
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] cipherText = new byte[cipher.getOutputSize(input.length)];
        int ctLength = cipher.update(input, 0, input.length, cipherText, 0);
        ctLength += cipher.doFinal(cipherText, ctLength);
        return cipherText;
    }

    public static byte[] desDecrypt(byte[] keyBytes,byte[] input) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        SecretKeySpec key = new SecretKeySpec(keyBytes, "DES");
        Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding", new BouncyCastleProvider());
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] plainText = new byte[cipher.getOutputSize(input.length)];
        int ptLength = cipher.update(input, 0, input.length, plainText, 0);
        ptLength += cipher.doFinal(plainText, ptLength);
        return plainText;
    }
    public static byte[] tdesDecryptIsw(byte[] bArr2) throws Exception, RemoteException {

        //if(DeviceApplication.getDataServiceInstance()!= null){
        byte[] bArr3 =null;
        if(Session.getTPKKEY()!=null && Session.getTMKKEY()!=null ){
            TRACE.i("TDES cached tpkkey: "+Session.getTPKKEY());
            TRACE.i("TDES cached tmkkey: "+Session.getTMKKEY());
            byte[] bArr = Utils.hexStringToByteArray(Session.getTPKKEY());
            byte[] bArr4 = Utils.hexStringToByteArray(Session.getTMKKEY());

            byte[] bArr5 = tdesECBDecrypt(bArr4,bArr);
            SecretKeySpec secretKeySpec = new SecretKeySpec(bArr5, "DES");
            Cipher instance = Cipher.getInstance("DESede/ECB/NoPadding", new BouncyCastleProvider());
            instance.init(2, secretKeySpec);
            bArr3 = new byte[instance.getOutputSize(bArr2.length)];
            instance.doFinal(bArr3, instance.update(bArr2, 0, bArr2.length, bArr3, 0));

        }
        else
        {
            TRACE.i("TDES cached tpkkey: "+Session.getTPKKEY());
            TRACE.i("TDES cached tmkkey: "+Session.getTMKKEY());
        }
        return bArr3;

//        }else{
//            throw new RuntimeException("Null Value");
//        }


    }

    public static byte[] tdesEncypt(byte[] keyBytes,byte[] input) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        SecretKeySpec key = new SecretKeySpec(keyBytes, "DES");
        Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding", new BouncyCastleProvider());
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] cipherText = new byte[cipher.getOutputSize(input.length)];
        int ctLength = cipher.update(input, 0, input.length, cipherText, 0);
        ctLength += cipher.doFinal(cipherText, ctLength);
        return cipherText;
    }

    public static byte[] tdesDecrypt(byte[] keyBytes,byte[] input) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        SecretKeySpec key = new SecretKeySpec(keyBytes, "DES");
        Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding", new BouncyCastleProvider());
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] plainText = new byte[cipher.getOutputSize(input.length)];
        int ptLength = cipher.update(input, 0, input.length, plainText, 0);
        ptLength += cipher.doFinal(plainText, ptLength);
        return plainText;
    }

    public static byte[] tdesCBCEncypt(byte[] keyBytes,byte[] input) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, ShortBufferException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        SecretKeySpec key = new SecretKeySpec(keyBytes, "DES");
        Cipher cipher = Cipher.getInstance("DESede/CBC/NoPadding", new BouncyCastleProvider());
        byte[] iv = new byte[]{0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0};
        cipher.init(Cipher.ENCRYPT_MODE, key,new IvParameterSpec(iv));
        byte[] cipherText = new byte[cipher.getOutputSize(input.length)];
        //int ctLength = cipher.update(input, 0, input.length, cipherText, 0);
        cipherText=cipher.doFinal(input);
        return cipherText;
    }

    public static byte[] tdesCBCDecrypt(byte[] keyBytes,byte[] input) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, ShortBufferException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        SecretKeySpec key = new SecretKeySpec(keyBytes, "DES");
        Cipher cipher = Cipher.getInstance("DESede/CBC/NoPadding", new BouncyCastleProvider());
        byte[] iv = new byte[]{0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0};
        cipher.init(Cipher.DECRYPT_MODE, key,new IvParameterSpec(iv));
        byte[] plainText = new byte[cipher.getOutputSize(input.length)];
        int ptLength = cipher.update(input, 0, input.length, plainText, 0);
        ptLength += cipher.doFinal(plainText, ptLength);
        return plainText;
    }

    public static byte[] tdesECBEncypt(byte[] keyBytes,byte[] input) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, ShortBufferException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        SecretKeySpec key = new SecretKeySpec(keyBytes, "DES");
        Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding", new BouncyCastleProvider());
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] cipherText = new byte[cipher.getOutputSize(input.length)];
        int ctLength = cipher.update(input, 0, input.length, cipherText, 0);
        ctLength += cipher.doFinal(cipherText, ctLength);
        return cipherText;
    }

    public static byte[] tdesECBDecrypt(byte[] keyBytes,byte[] input) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, ShortBufferException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        SecretKeySpec key = new SecretKeySpec(keyBytes, "DES");
        Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding", new BouncyCastleProvider());
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] plainText = new byte[cipher.getOutputSize(input.length)];
        int ptLength = cipher.update(input, 0, input.length, plainText, 0);
        //System.out.println(RSA.byteArrayToString(plainText));
        ptLength += cipher.doFinal(plainText, ptLength);
        return plainText;
    }


    public static void main(String[] args) {
        byte[]  input = new byte[] {
                0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
                0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f,
                0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07};
        byte[] deskeyBytes = new byte[] {
                0x01, 0x23, 0x45, 0x67, (byte)0x89, (byte)0xab, (byte)0xcd, (byte)0xef};
        byte[] tdeskeyBytes = new byte[] {
                0x01, 0x23, 0x45, 0x67, (byte)0x89, (byte)0xab, (byte)0xcd, (byte)0xef,
                0x01, 0x23, 0x45, 0x67, (byte)0x89, (byte)0xab, (byte)0xcd, (byte)0xef};

        try {
            byte[] e = TDES.desEncypt(deskeyBytes, input);

        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}
