package com.isw.payapp.devices.dspread.utils;

import com.dspread.xpos.utils.BASE64Decoder;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class RSA {
    public static final String SIGN_ALGORITHMS = "SHA1WithRSA";

    private static final String DEFAULT_PUBLIC_KEY=
            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQChDzcjw/rWgFwnxunbKp7/4e8w" + "\r" +
                    "/UmXx2jk6qEEn69t6N2R1i/LmcyDT1xr/T2AHGOiXNQ5V8W4iCaaeNawi7aJaRht" + "\r" +
                    "Vx1uOH/2U378fscEESEG8XDqll0GCfB1/TjKI2aitVSzXOtRs8kYgGU78f7VmDNg" + "\r" +
                    "XIlk3gdhnzh+uoEQywIDAQAB" + "\r";

    private static final String DEFAULT_PRIVATE_KEY=
            "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAKEPNyPD+taAXCfG" + "\r" +
                    "6dsqnv/h7zD9SZfHaOTqoQSfr23o3ZHWL8uZzINPXGv9PYAcY6Jc1DlXxbiIJpp4" + "\r" +
                    "1rCLtolpGG1XHW44f/ZTfvx+xwQRIQbxcOqWXQYJ8HX9OMojZqK1VLNc61GzyRiA" + "\r" +
                    "ZTvx/tWYM2BciWTeB2GfOH66gRDLAgMBAAECgYBp4qTvoJKynuT3SbDJY/XwaEtm" + "\r" +
                    "u768SF9P0GlXrtwYuDWjAVue0VhBI9WxMWZTaVafkcP8hxX4QZqPh84td0zjcq3j" + "\r" +
                    "DLOegAFJkIorGzq5FyK7ydBoU1TLjFV459c8dTZMTu+LgsOTD11/V/Jr4NJxIudo" + "\r" +
                    "MBQ3c4cHmOoYv4uzkQJBANR+7Fc3e6oZgqTOesqPSPqljbsdF9E4x4eDFuOecCkJ" + "\r" +
                    "DvVLOOoAzvtHfAiUp+H3fk4hXRpALiNBEHiIdhIuX2UCQQDCCHiPHFd4gC58yyCM" + "\r" +
                    "6Leqkmoa+6YpfRb3oxykLBXcWx7DtbX+ayKy5OQmnkEG+MW8XB8wAdiUl0/tb6cQ" + "\r" +
                    "FaRvAkBhvP94Hk0DMDinFVHlWYJ3xy4pongSA8vCyMj+aSGtvjzjFnZXK4gIjBjA" + "\r" +
                    "2Z9ekDfIOBBawqp2DLdGuX2VXz8BAkByMuIh+KBSv76cnEDwLhfLQJlKgEnvqTvX" + "\r" +
                    "TB0TUw8avlaBAXW34/5sI+NUB1hmbgyTK/T/IFcEPXpBWLGO+e3pAkAGWLpnH0Zh" + "\r" +
                    "Fae7oAqkMAd3xCNY6ec180tAe57hZ6kS+SYLKwb4gGzYaCxc22vMtYksXHtUeamo" + "\r" +
                    "1NMLzI2ZfUoX" + "\r";

    /**
     * 私钥
     */
    private RSAPrivateKey privateKey;

    /**
     * 公钥
     */
    private RSAPublicKey publicKey;

    /**
     * 字节数据转字符串专用集合
     */
    private static final char[] HEX_CHAR= {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};


    /**
     * 获取私钥
     * @return 当前的私钥对�?
     */
    public RSAPrivateKey getPrivateKey() {
        return privateKey;
    }

    /**
     * 获取公钥
     * @return 当前的公钥对�?
     */
    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * 随机生成密钥�?
     */
    public void genKeyPair(){
        KeyPairGenerator keyPairGen= null;
        try {
            keyPairGen= KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        keyPairGen.initialize(1024, new SecureRandom());
        KeyPair keyPair= keyPairGen.generateKeyPair();
        this.privateKey= (RSAPrivateKey) keyPair.getPrivate();
        this.publicKey= (RSAPublicKey) keyPair.getPublic();
    }

    /**
     * 从文件中输入流中加载公钥
     * @param in 公钥输入�?
     * @throws Exception 加载公钥时产生的异常
     */
    public void loadPublicKey(InputStream in) throws Exception{
        try {
            BufferedReader br= new BufferedReader(new InputStreamReader(in));
            String line= null;
            StringBuilder sb= new StringBuilder();
//			while((readLine= br.readLine())!=null){
            //			if(readLine.charAt(0)=='-'){
            //				continue;
            //			}else{
            //				sb.append(readLine);
            //				sb.append('\r');
            //			}
            //		}
            while((line= br.readLine())!=null){

                if(line.contains("BEGIN")){
                    sb.delete(0, sb.length());
                    continue;
                }else if (line.contains("END")) {
                    break;
                }else{
                    sb.append(line);
                    sb.append('\r');
                }
            }

            loadPublicKey(sb.toString());
        } catch (IOException e) {
            throw new Exception("公钥数据流读取错�?");
        } catch (NullPointerException e) {
            throw new Exception("公钥输入流为�?");
        }
    }


    /**
     * 从字符串中加载公�?
     * @param publicKeyStr 公钥数据字符�?
     * @throws Exception 加载公钥时产生的异常
     */
    public void loadPublicKey(String publicKeyStr) throws Exception{
        try {
            BASE64Decoder base64Decoder= new BASE64Decoder();
            byte[] buffer= base64Decoder.decodeBuffer(publicKeyStr);
            KeyFactory keyFactory= KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec= new X509EncodedKeySpec(buffer);
            this.publicKey= (RSAPublicKey) keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("无此算法");
        } catch (InvalidKeySpecException e) {
            throw new Exception("公钥非法");
        } catch (IOException e) {
            throw new Exception("公钥数据内容读取错误");
        } catch (NullPointerException e) {
            throw new Exception("公钥数据为空");
        }
    }

    /**
     * 从文件中加载私钥
     * @return 是否成功
     * @throws Exception
     */
    public void loadPrivateKey(InputStream in) throws Exception{
        try {
            BufferedReader br= new BufferedReader(new InputStreamReader(in));
            String readLine= null;
            StringBuilder sb= new StringBuilder();
            while((readLine= br.readLine())!=null){
                if(readLine.charAt(0)=='-'){
                    continue;
                }else{
                    sb.append(readLine);
                    sb.append('\r');
                }
            }
            loadPrivateKey(sb.toString());
        } catch (IOException e) {
            throw new Exception("私钥数据读取错误");
        } catch (NullPointerException e) {
            throw new Exception("私钥输入流为�?");
        }
    }

    public void loadPrivateKey(String privateKeyStr) throws Exception{
        try {
            BASE64Decoder base64Decoder= new BASE64Decoder();
            byte[] buffer= base64Decoder.decodeBuffer(privateKeyStr);
            PKCS8EncodedKeySpec keySpec= new PKCS8EncodedKeySpec(buffer);
            KeyFactory keyFactory= KeyFactory.getInstance("RSA");
            this.privateKey= (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("无此算法");
        } catch (InvalidKeySpecException e) {
            throw new Exception("私钥非法");
        } catch (IOException e) {
            throw new Exception("私钥数据内容读取错误");
        } catch (NullPointerException e) {
            throw new Exception("私钥数据为空");
        }
    }

    /**
     * 加密过程
     * @param plainTextData 明文数据
     * @return
     * @throws Exception 加密过程中的异常信息
     */
    public byte[] encrypt(byte[] plainTextData) throws Exception{
        RSAPublicKey publicKey = this.getPublicKey();
        if(publicKey== null){
            throw new Exception("加密公钥为空, 请设�?");
        }
        Cipher cipher= null;
        try {
            cipher= Cipher.getInstance("RSA/None/PKCS1Padding", new BouncyCastleProvider());
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] output= cipher.doFinal(plainTextData);
            return output;
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("无此加密算法");
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
            return null;
        }catch (InvalidKeyException e) {
            throw new Exception("加密公钥非法,请检�?");
        } catch (IllegalBlockSizeException e) {
            throw new Exception("明文长度非法");
        } catch (BadPaddingException e) {
            throw new Exception("明文数据已损�?");
        }
    }

    /**
     * 解密过程
     * @param cipherData 密文数据
     * @return 明文
     * @throws Exception 解密过程中的异常信息
     */
    public byte[] decrypt(byte[] cipherData) throws Exception{
        RSAPrivateKey privateKey = this.getPrivateKey();
        if (privateKey== null){
            throw new Exception("解密私钥为空, 请设�?");//None
        }
        Cipher cipher= null;
        try {
            cipher= Cipher.getInstance("RSA/None/PKCS1Padding", new BouncyCastleProvider());
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] output= cipher.doFinal(cipherData);
            return output;
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("无此解密算法");
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
            return null;
        }catch (InvalidKeyException e) {
            throw new Exception("解密私钥非法,请检�?");
        } catch (IllegalBlockSizeException e) {
            throw new Exception("密文长度非法");
        } catch (BadPaddingException e) {
            throw new Exception("密文数据已损�?");
        }
    }


    /**
     * 字节数据转十六进制字符串
     * @param data 输入数据
     * @return 十六进制内容
     */

    public static String byteArrayToString(byte[] data){
        StringBuilder stringBuilder= new StringBuilder();
        for (int i=0; i<data.length; i++){
            //取出字节的高四位 作为索引得到相应的十六进制标识符 注意无符号右�?
            stringBuilder.append(HEX_CHAR[(data[i] & 0xf0)>>> 4]);
            //取出字节的低四位 作为索引得到相应的十六进制标识符
            stringBuilder.append(HEX_CHAR[(data[i] & 0x0f)]);
            if (i<data.length-1){
                stringBuilder.append(' ');
            }
        }
        return stringBuilder.toString();
    }

    public static byte[] hexStringToByte(String hex) {
        int len = (hex.length() / 2);
        System.out.println(len);
        byte[] result = new byte[len];
        char[] achar = hex.toCharArray();
        for (int i = 0; i < len; i++) {
            int pos = i * 2;
            result[i] = (byte) (toByte(achar[pos]) << 4 | toByte(achar[pos + 1]));
        }
        return result;
    }

    private static byte toByte(char c) {
        byte b = (byte) "0123456789ABCDEF".indexOf(c);
        return b;
    }

    /** * 16进制 To byte[] * @param hexString * @return byte[] */
    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    /** * Convert char to byte * @param c char * @return byte */
    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }
    /*
   	public byte[] sign(byte[] content)
	{
        try {
            java.security.Signature signature = java.security.Signature.getInstance(SIGN_ALGORITHMS);
            signature.initSign(this.getPrivateKey());
            signature.update(content);
            byte[] signed = signature.sign();
            return signed;
        }catch (Exception e) {
        	e.printStackTrace();
        }

        return new byte[0];
    }*/

    public byte[] sign(byte[] content)
    {
        try
        {
            Signature signature = Signature.getInstance("SHA1WithRSA");
            signature.initSign(this.privateKey);
            signature.update(content);
            return signature.sign();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    boolean check(byte[] content) {
        try {

            Signature signature = Signature.getInstance("SHA1WithRSA");
            signature.initVerify(this.publicKey);
            signature.update(content);
            return signature.verify(content);
        } catch (Exception e) {
            System.out.println("验签失败");
        }
        return false;
    }



    public static void main(String[] args){
        RSA rsaEncrypt= new RSA();
        //rsaEncrypt.genKeyPair();
        try {
            String n = "150479491925261579720208703452547977089765773133163483855836185828048489607003741592913530042316680238856089069019174749776137982649856843271794093582822170915724974369562653137375918136521773899771851799179130917321516038499217993296118718532162256761744707253251577731818247634523594321069305828794576805901";
            String e = "65537";
            rsaEncrypt.loadPublicKey(new BigInteger(n), new BigInteger(e));
            //rsaEncrypt.loadPublicKey(new FileInputStream(new File("./keys/pub.pem")));
            rsaEncrypt.loadPrivateKey(new FileInputStream(new File("./keys/debug_certificate.pem")));

        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        //加载公钥
		/*
		try {
			//rsaEncrypt.loadPublicKey(RSAEncrypt.DEFAULT_PUBLIC_KEY);
			System.out.println("加载公钥成功");
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.err.println("加载公钥失败");
		}

		//加载私钥
		try {
			//rsaEncrypt.loadPrivateKey(RSAEncrypt.DEFAULT_PRIVATE_KEY);
			System.out.println("加载私钥成功");
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.err.println("加载私钥失败");
		}*/

        //测试字符�?
        String encryptStr= "你好, BOB!";

        try {
            //加密
            byte[] cipher = rsaEncrypt.encrypt(encryptStr.getBytes());
            System.out.println("密文长度:"+ cipher.length);
            System.out.println(RSA.byteArrayToString(cipher));
            //解密

            String str = "b213e8074be982e332b70588668759ce58522386917d065ddb7c6272bd7e2646b89fb298632809f727514dd4de8b76ea53f063dbd9b35a3d43529c2464f7300c44a27eafe9f0ea5c5bd37d5f39e451190e136496ad86858dc71c2b3809fd845a9f7b20d320e22fdf70980a4b53fdae07e2eba6ce06c9f989b8ffc77b4bed7fa3";
            byte[] plainText = rsaEncrypt.decrypt(cipher);
            //byte[] plainText = rsaEncrypt.decrypt(cipher);
            //System.out.println(RSA.byteArrayToString(hexStringToBytes(str)));
            //plainText = rsaEncrypt.decrypt(hexStringToBytes(str));

            System.out.println("明文长度:"+ plainText.length);
            //System.out.println(RSA.byteArrayToString(plainText));
            System.out.println(new String(plainText,"utf-8"));

            byte[] sign = rsaEncrypt.sign("你好, BOB!".getBytes("utf-8"));
            System.out.println("签名长度:"+ sign.length);
            System.out.println(RSA.byteArrayToString(sign));

            str = "6b479adc770b3e86cedda99af7bb111f46241e433f6e72c719295cf09001e4a803b7d1d44b4696f0e9fcc1529e1aecc423472001d84d991e769414db6ee5070e0dacef77e6b5651f1deea250dc1a140cbbd4ae31ad9cc986d6ce8bf2a63013f451f8a7dd2f675c2d0e9354e36308cc7a369320e359cc8b354cdfbfaa637cb059";

            boolean r = rsaEncrypt.check(sign);
            if(r){
                System.out.println("验签成功");
            }else{
                System.out.println("验签失败");
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public void loadPublicKey(String moduls,String publicExponent) {
        KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
            this.publicKey= (RSAPublicKey) keyFactory.generatePublic(new RSAPublicKeySpec(new BigInteger(moduls,16),new BigInteger(publicExponent,16)));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }

    public void loadPublicKey(BigInteger pn,BigInteger pe) {
        KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
            this.publicKey= (RSAPublicKey) keyFactory.generatePublic(new RSAPublicKeySpec(pn,pe));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }
}
