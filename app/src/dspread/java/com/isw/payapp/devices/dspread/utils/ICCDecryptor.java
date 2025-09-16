package com.isw.payapp.devices.dspread.utils;

import android.os.Build;

import com.isw.payapp.utils.DataProcessUtil;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HexFormat;
public class ICCDecryptor {
    private  final String BDK_HEX = "0123456789ABCDEFFEDCBA9876543210";
    private  final byte[] BDK = DataProcessUtil.hexStringToByte(BDK_HEX);

    public  String decryptIccInfo(String ksnHex, String dataHex) {
        try {
            System.out.println("RRR:"+ksnHex+"\n"+dataHex);
            byte[] ksn = DataProcessUtil.hexStringToByte(ksnHex);
            byte[] data =DataProcessUtil.hexStringToByte(dataHex);

            byte[] ipek = generateIPEK(ksn, BDK);
           // ipek = DataProcessUtil.hexStringToByte("33707E4927C4A0D50000000000000000");//hexStringToByteArray("33707E4927C4A0D50000000000000000");//generateIPEK(ksn, BDK);
            byte[] dataKey = getDataKey(ksn, ipek);

            byte[] decryptedData = tdesDecrypt(data, dataKey);//dataKey
           return DataProcessUtil.bytesToHexString(decryptedData);
          //  return decryptedData;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Decryption failed", e);
        }
    }

    private  byte[] generateIPEK(byte[] ksn, byte[] bdk) {
        // This is a simplified implementation - you may need to adjust based on your specific key derivation requirements
        // Typically, this would involve using the KSN and BDK to derive the IPEK
        try {
            // For demonstration - actual implementation may vary
            byte[] combined = new byte[32]; //bdk.length + ksn.length
            System.arraycopy(bdk, 0, combined, 0, bdk.length);
            System.arraycopy(ksn, 0, combined, bdk.length, ksn.length);

            // Simple XOR derivation (replace with your actual algorithm)
            byte[] ipek = new byte[16];
            for (int i = 0; i < 16; i++) {
                ipek[i] = (byte) (combined[i] ^ combined[i + 16]);
            }
            return ipek;

        } catch (Exception e) {
            throw new RuntimeException("IPEK generation failed", e);
        }
    }

    private  byte[] getDataKey(byte[] ksn, byte[] ipek) {
        // Derive data key from KSN and IPEK
        // This is algorithm-specific - adjust as needed
        if ( ipek == null) {
            throw new IllegalArgumentException(" IPEK cannot be null");
        }
        if (ksn == null) {
            throw new IllegalArgumentException("KSN cannot be null");
        }



        try {
            //byte[] combined = new byte[ipek.length + ksn.length];
            byte[] combined = new byte[ksn.length];
           // System.arraycopy(ipek, 0, combined, 0, ipek.length);
//            System.arraycopy(ksn, 0, combined, ipek.length, ksn.length);
            System.arraycopy(ksn, 0, combined, 0, ksn.length);

            // Simple derivation (replace with your actual algorithm)
            byte[] dataKey = new byte[24]; // 3DES requires 24 bytes
            for (int i = 0; i < 24; i++) {
                dataKey[i] = (byte) (combined[i % combined.length] ^ (i + 1));
            }
            return dataKey;

        } catch (Exception e) {
            throw new RuntimeException("Data key generation failed", e);
        }
    }

    private  byte[] tdesDecrypt(byte[] data, byte[] key) throws GeneralSecurityException {
        try {
            // Create Triple DES key specification
            DESedeKeySpec keySpec = new DESedeKeySpec(key);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DESede");
            SecretKey secretKey = keyFactory.generateSecret(keySpec);

            // Initialize cipher for decryption
            Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding"); // Adjust mode/padding as needed
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            return cipher.doFinal(data);

        } catch (Exception e) {
            throw new GeneralSecurityException("TDES decryption failed", e);
        }
    }

    // Utility methods for hex conversion
    private  byte[] hexStringToByteArray(String hex) {
        HexFormat hexFormat = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            hexFormat = HexFormat.of();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return hexFormat.parseHex(hex);
        }
        return null;
    }

    private  String bytesToHex(byte[] bytes) {
        HexFormat hexFormat = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            hexFormat = HexFormat.of();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return hexFormat.formatHex(bytes);
        }
        return null;
    }

//    // Main method for testing
//    public  void main(String[] args) {
//        String KSN = "00000332100300E00003";
//        String DATA = "E84B5D0D2AA9F40A04127284B0BEF5BA7901E0914C3F71C6042C61AE9576615360A8B32507769EA9876CCAE0BD87CE43F729D4509E436C422B556E5E480F1309ACD278EF779911E799D5F8A7C28ABA26654E5A99623E507330446A0C0E8463361E53B6712E080C973EDC44DD09FAB019481DABA6C33933DCE66BB5D2846F3005A5A5A1573199C1FFFA483396D1EE8E5918BD13F761011186C58CF5F2E3A45B4C35508DFD1D28C27207C6D0D6FBFADF9038E97B51B6422F56CFE024B2F284572B2C7DCD7E107D5A1550481A1ADB277F8CA37753E8EFF7980DA392EFCF9D2473F7D37E84B608158FA87FB140701FA09CD2FCC2329CF5A8B92D70FD15010BAF751BC4A3C6FD6DBB3C7C90BB5286AFEC923FAE96C09A4FDB65256FE5D8606132B12A4853B609FB80C8E9172C01F7A152E715A5A75D5305165EDA939E4C51313A176691F55B35207C8857221793AB7C045E48";
//
//        String result = decryptIccInfo(KSN, DATA);
//        System.out.println("Decrypted result: " + result);
//    }
}
