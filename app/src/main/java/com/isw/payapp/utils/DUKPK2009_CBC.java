package com.isw.payapp.utils;

import android.content.Context;
import android.util.Log;

import com.isw.payapp.helpers.ConfigManager;
import com.isw.payapp.model.TerminalConfigModel;

import java.math.BigInteger;
import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
public class DUKPK2009_CBC {

    public enum Enum_key {
        DATA, PIN, MAC, DATA_VARIANT;
    }

    public enum Enum_mode {
        ECB, CBC;
    }

    private static String globalIpek;
    private static String getPinKey;

    private static String clearIpek;

    public static  void setClearIpek(String clearIpek){
        DUKPK2009_CBC.clearIpek = clearIpek;
    }

    public static String getClearIpek(){
        return clearIpek;
    }

    public static void setGetPinKey(String getPinKey){
        DUKPK2009_CBC.getPinKey = getPinKey;
    }

    public static String getGetPinKey(){
        return getPinKey;
    }
    /*
     * ksnV:ksn
     * datastrV:data
     * Enum_key:Encryption/Decryption
     * Enum_mode
     *
     * */
    public static String getData(Context context,String ksnV, String datastrV, Enum_key key, Enum_mode mode) {

        return getData(context,ksnV, datastrV, key, mode, null);
    }

    public static String getData(Context context, String ksnV, String datastrV, Enum_key key, Enum_mode mode, String clearIpek) {
        //		// TODO Auto-generated method stub

        ConfigManager.refreshConfig(context);
        TerminalConfigModel config = ConfigManager.getConfig(context);
        String ksn = ksnV;
        String datastr = datastrV;
        byte[] ipek = null;
        byte[] byte_ksn = parseHexStr2Byte(ksn);
        if (clearIpek == null || clearIpek.length() == 0) {
            String bdk = config.getDeskey();//"6276A16D9B8C9BDA382A9BADA4AD2F9B";//"6276A16D9B8C9BDA382A9BADA4AD2F9B" "B30D16EAE5372C9457326464E62C5E61";
            Log.d("BDKDD", bdk);
            byte[] byte_bdk = parseHexStr2Byte(bdk);
            ipek = GenerateIPEK(byte_ksn, byte_bdk);

        } else {
            ipek = parseHexStr2Byte(clearIpek);
        }
        String ipekStr = parseByte2HexStr(ipek);// after testing, ipek is the same
        //setClearIpek(ipekStr);
        System.out.println("ipekStr=" + ipekStr);
        globalIpek = ipekStr;


        byte[] dataKey = GetDataKey(byte_ksn,ipek);
        String dataKeyStr = parseByte2HexStr(dataKey);
        System.out.println("dataKeyStr=" + dataKeyStr);

        byte[] dataKeyVariant = GetDataKeyVariant(byte_ksn, ipek);
        String dataKeyStrVariant = parseByte2HexStr(dataKeyVariant);
        System.out.println("dataKeyStrVariant=" + dataKeyStrVariant);

        byte[] pinKey = GetPinKeyVariant(byte_ksn, ipek);
        String pinKeyStr = parseByte2HexStr(pinKey);
        setClearIpek(pinKeyStr);
        System.out.println("pinKeyStr=" + pinKeyStr);

        byte[] macKey = GetMacKeyVariant(byte_ksn, ipek);
        String macKeyStr = parseByte2HexStr(macKey);
        System.out.println("macKeyStr=" + macKeyStr);

        String keySel = null;
        switch (key) {
            case MAC:
                keySel = macKeyStr;
                break;
            case PIN:
                keySel = pinKeyStr;
                setGetPinKey(keySel);
                break;
            case DATA:
                keySel = dataKeyStr;
                break;
            case DATA_VARIANT:
                keySel = dataKeyStrVariant;
                break;
        }

        byte[] buf = null;
        if (mode == Enum_mode.CBC){
            buf = TriDesDecryptionCBC(parseHexStr2Byte(keySel), parseHexStr2Byte(datastr));
        } else if (mode == Enum_mode.ECB){
            buf = TriDesDecryptionECB(parseHexStr2Byte(keySel), parseHexStr2Byte(datastr));
        }
        String deResultStr = parseByte2HexStr(buf);
//        System.out.println("data: " + deResultStr);
        return deResultStr;
    }

    public static String generatePinBlock(String pinKsn, String clearPin, String pan, String clearIpek){
        //		// TODO Auto-generated method stub
        int length = 14-clearPin.length();
        String newClearPin = "0" + clearPin.length() + clearPin;
        for (int i = 0; i<length; i++){
            newClearPin = newClearPin + "F";
        }
            String newPan = pan.substring(pan.length()-13 ,pan.length()-1);
        newPan = "0000" + newPan;
        System.out.println("newPan: " + newPan);
        String xorResult = xor(newClearPin,newPan);
        System.out.println("data: " + xorResult);

        byte[] byte_ksn = parseHexStr2Byte(pinKsn);
        byte[] byte_ipek = parseHexStr2Byte(clearIpek);
        byte[] byte_pin = parseHexStr2Byte(xorResult);

        byte[] pinKey = GetPinKeyVariant(byte_ksn, byte_ipek);
        String pinKeyStr = parseByte2HexStr(pinKey);
        System.out.println("pinKeyStr=" + pinKeyStr);

        byte[] buf = TriDesEncryption(pinKey,byte_pin);
        String deResultStr = parseByte2HexStr(buf);
        System.out.println("data: " + deResultStr);
        return deResultStr;
    }

    /**
     * Extract clear PIN from formatted PIN data (decrypted PINBLOCK) and PAN according to ANSI X9.8
     *
     * @param formattedPinData The decrypted PINBLOCK (formatted PIN data), e.g., "041127ADEDAFEFFF"
     * @param pan The full PAN number, e.g., "6210003652125010004"
     * @return The clear PIN value, e.g., "1111"
     */
    public static String extractClearPIN(String formattedPinData, String pan) {
        try {
            // Step 1: Extract 12 rightmost PAN digits without checksum
            String cleanPan = pan.replaceAll("[^0-9]", "");
            String twelveDigits;

            if (cleanPan.length() == 19) {
                // For 19-digit PAN: get digits 7-18 (excluding last checksum)
                twelveDigits = cleanPan.substring(7, 19); // Should be "365212501000"
            } else if (cleanPan.length() == 16) {
                // For 16-digit PAN: get digits 4-15 (excluding last checksum)
                twelveDigits = cleanPan.substring(4, 16);
            } else {
                // Handle other PAN lengths by taking 12 rightmost digits excluding last digit
                if (cleanPan.length() > 12) {
                    twelveDigits = cleanPan.substring(cleanPan.length() - 13, cleanPan.length() - 1);
                } else {
                    throw new IllegalArgumentException("PAN too short");
                }
            }

            System.out.println("12 right most PAN digits without checksum: " + twelveDigits);

            // Step 2: Add "0000" to the left
            String formattedPAN = "0000" + twelveDigits;
            System.out.println("Add 0000 to the left: " + formattedPAN);

            // Step 3: XOR formatted PAN with formatted PIN data
            String xorResult = xor(formattedPAN, formattedPinData);
            System.out.println("XOR (" + formattedPAN + ", " + formattedPinData + ") = " + xorResult);

            // Step 4: Extract PIN from XOR result
            // The format is: First nibble (0) + PIN length (4) + PIN digits + F padding
            // Example: "041111FFFFFFFFFF" -> PIN length=4, PIN="1111"

            // Get the PIN length from the second nibble (first byte)
            String firstByte = xorResult.substring(0, 2);
            int pinLength = Integer.parseInt(firstByte.substring(1, 2), 16);

            System.out.println("PIN length from formatted data: " + pinLength);

            // Extract PIN digits
            StringBuilder pinBuilder = new StringBuilder();
            int pinDigitsExtracted = 0;

            // Start from position 2 (after the length byte)
            for (int i = 2; i < xorResult.length() && pinDigitsExtracted < pinLength; i += 2) {
                String byteStr = xorResult.substring(i, i + 2);

                // Check if we've reached padding
                if (byteStr.equals("FF") || byteStr.equals("F0")) {
                    break;
                }

                // Each byte contains 2 PIN digits in BCD format
                // First digit is high nibble, second digit is low nibble
                int byteValue = Integer.parseInt(byteStr, 16);

                // Extract first digit (high nibble)
                int firstDigit = (byteValue >> 4) & 0x0F;
                if (firstDigit <= 9 && pinDigitsExtracted < pinLength) {
                    pinBuilder.append(firstDigit);
                    pinDigitsExtracted++;
                }

                // Extract second digit (low nibble)
                int secondDigit = byteValue & 0x0F;
                if (secondDigit <= 9 && pinDigitsExtracted < pinLength) {
                    pinBuilder.append(secondDigit);
                    pinDigitsExtracted++;
                }
            }

            String clearPIN = pinBuilder.toString();

            // Validate PIN length
            if (clearPIN.length() != pinLength) {
                System.out.println("Warning: Extracted PIN length (" + clearPIN.length() +
                        ") doesn't match expected length (" + pinLength + ")");
            }

            System.out.println("The clear PIN is: " + clearPIN);
            return clearPIN;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Alternative simpler method that matches your example exactly
     */
    public static String getClearPINFromFormattedData(String formattedPinData, String pan) {
        try {
            // Format PAN as described in your example
            String cleanPan = pan.replaceAll("[^0-9]", "");
            String twelveDigits;

            if (cleanPan.length() == 19) {
                twelveDigits = cleanPan.substring(7, 19); // "365212501000"
            } else if (cleanPan.length() == 16) {
                twelveDigits = cleanPan.substring(3, 15);
            } else {
                // Take 12 rightmost digits excluding last checksum
                twelveDigits = cleanPan.substring(cleanPan.length() - 13, cleanPan.length() - 1);
            }

            String formattedPAN = "0000" + twelveDigits;

            // XOR operation
            String xorResult = xor(formattedPinData,formattedPAN);

            // Extract PIN - simplified version for standard format
            // Find first non-F character after the length nibble
            String pinDigits = "";
            boolean collecting = false;

            for (int i = 2; i < xorResult.length(); i += 2) {
                String byteStr = xorResult.substring(i, i + 2);

                if (byteStr.equals("FF") || byteStr.equals("F0")) {
                    break;
                }

                // Convert byte to two digits
                int byteValue = Integer.parseInt(byteStr, 16);

                // First digit (high nibble)
                int firstDigit = (byteValue >> 4) & 0x0F;
                if (firstDigit <= 9) {
                    pinDigits += firstDigit;
                }

                // Second digit (low nibble)
                int secondDigit = byteValue & 0x0F;
                if (secondDigit <= 9) {
                    pinDigits += secondDigit;
                }
            }

            return pinDigits;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Extract clear PIN from encrypted PIN block using PAN and PEK (Pin Encryption Key)
     *
     * @param encryptedPinBlock The encrypted PIN block (hex string)
     * @param pan The full PAN number
     * @param pek The Pin Encryption Key (hex string)
     * @return The clear PIN value
     */
    public static String extractPINFromPINBlock(String encryptedPinBlock, String pan, String pek) {
        try {
            System.out.println("=== Starting PIN Extraction ===");
            System.out.println("Encrypted PIN Block: " + encryptedPinBlock);
            System.out.println("PAN: " + pan);
            System.out.println("PEK: " + pek);

            // Step 1: Decrypt the PIN block using PEK (ECB mode for PIN blocks)
            byte[] decryptedBlock = TriDesDecryptionCBC(parseHexStr2Byte(pek),
                    parseHexStr2Byte(encryptedPinBlock));
            String formattedPinData = parseByte2HexStr(decryptedBlock);
            System.out.println("Decrypted PINBLOCK (formatted PIN data): " + formattedPinData);

            // Step 2: Extract 12 rightmost PAN digits without checksum
            String cleanPan = pan.replaceAll("[^0-9]", "");
            String twelveDigits;

            if (cleanPan.length() == 19) {
                // For 19-digit PAN (like your example): get digits 7-18 (excluding last checksum)
                twelveDigits = cleanPan.substring(7, 19); // "365212501000"
            } else if (cleanPan.length() == 16) {
                // For standard 16-digit PAN: get digits 4-15 (excluding last checksum)
                twelveDigits = cleanPan.substring(3, 15);
            } else if (cleanPan.length() > 12) {
                // Generic: take 12 rightmost digits excluding the last checksum digit
                twelveDigits = cleanPan.substring(cleanPan.length() - 13, cleanPan.length() - 1);
            } else {
                throw new IllegalArgumentException("PAN too short: " + cleanPan);
            }

            System.out.println("12 rightmost PAN digits without checksum: " + twelveDigits);

            // Step 3: Add "0000" to the left as per ANSI X9.8
            String formattedPAN = "0000" + twelveDigits;
            System.out.println("Add 0000 to the left: " + formattedPAN);

            // Step 4: XOR formatted PAN with formatted PIN data
            String xorResult = xor(formattedPAN, formattedPinData);
            System.out.println("XOR (" + formattedPAN + ", " + formattedPinData + ") = " + xorResult);
            System.out.println("globalIpek :"+globalIpek);
            byte[] decryptedXorBlock = TriDesDecryptionCBC(parseHexStr2Byte(globalIpek),
                    parseHexStr2Byte(xorResult));
            String formattedXorData = parseByte2HexStr(decryptedXorBlock);
            System.out.println("Decrypted formattedXorData (formatted PIN data): " + formattedXorData);


            // Step 5: Extract PIN length from the second nibble


            String firstByte = xorResult.substring(0, 2);
            int pinLength = Integer.parseInt(firstByte.substring(1, 2), 16);
            System.out.println("PIN length from formatted data: " + pinLength);

            // Step 6: Extract PIN digits
            StringBuilder pinBuilder = new StringBuilder();
            int pinDigitsExtracted = 0;

            // Start from position 2 (after the length byte)
            for (int i = 2; i < xorResult.length() && pinDigitsExtracted < pinLength; i += 2) {
                String byteStr = xorResult.substring(i, i + 2);

                // Check if we've reached padding (F or 0F)
                if (byteStr.equals("FF") || byteStr.equals("F0") ||
                        byteStr.equals("0F") || byteStr.equals("00")) {
                    break;
                }

                // Each byte contains 2 PIN digits in BCD format
                int byteValue = Integer.parseInt(byteStr, 16);

                // Extract first digit (high nibble)
                int firstDigit = (byteValue >> 4) & 0x0F;
                if (firstDigit >= 0 && firstDigit <= 9 && pinDigitsExtracted < pinLength) {
                    pinBuilder.append(firstDigit);
                    pinDigitsExtracted++;
                }

                // Extract second digit (low nibble)
                int secondDigit = byteValue & 0x0F;
                if (secondDigit >= 0 && secondDigit <= 9 && pinDigitsExtracted < pinLength) {
                    pinBuilder.append(secondDigit);
                    pinDigitsExtracted++;
                }
            }

            String clearPIN = pinBuilder.toString();

            // Validate the extracted PIN
            if (clearPIN.length() == 0) {
                System.out.println("Warning: No PIN digits extracted, trying alternative parsing...");

                // Alternative parsing: look for continuous digits
                clearPIN = "";
                for (int i = 2; i < xorResult.length(); i += 2) {
                    String byteStr = xorResult.substring(i, i + 2);
                    if (!byteStr.equals("FF") && !byteStr.equals("F0")) {
                        // Try to extract digits
                        int byteValue = Integer.parseInt(byteStr, 16);
                        int firstDigit = (byteValue >> 4) & 0x0F;
                        int secondDigit = byteValue & 0x0F;

                        if (firstDigit >= 0 && firstDigit <= 9) {
                            clearPIN += firstDigit;
                        }
                        if (secondDigit >= 0 && secondDigit <= 9) {
                            clearPIN += secondDigit;
                        }
                    } else {
                        break;
                    }
                }

                // Trim to PIN length if we extracted too many digits
                if (clearPIN.length() > pinLength) {
                    clearPIN = clearPIN.substring(0, pinLength);
                }
            }

            System.out.println("=== PIN Extraction Complete ===");
            System.out.println("Extracted clear PIN: " + clearPIN);

            return clearPIN;

        } catch (Exception e) {
            System.err.println("Error extracting PIN: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Simplified version of the method that follows your example format exactly
     */
    public static String getPINFromPINBlock(String encryptedPinBlock, String pan, String pek) {
        try {
            // 1. Decrypt PIN block
            byte[] pekBytes = parseHexStr2Byte(pek);
            byte[] encryptedBytes = parseHexStr2Byte(encryptedPinBlock);
            byte[] decryptedBytes = TriDesDecryptionECB(pekBytes, encryptedBytes);
            String formattedPinData = parseByte2HexStr(decryptedBytes);

            // 2. Format PAN as per example
            String cleanPan = pan.replaceAll("[^0-9]", "");
            String twelveDigits;

            if (cleanPan.length() == 19) {
                twelveDigits = cleanPan.substring(7, 19);
            } else if (cleanPan.length() == 16) {
                twelveDigits = cleanPan.substring(4, 16);
            } else {
                twelveDigits = cleanPan.substring(cleanPan.length() - 13, cleanPan.length() - 1);
            }

            String formattedPAN = "0000" + twelveDigits;

            // 3. XOR operation
            String xorResult = xor(formattedPAN, formattedPinData);

            // 4. Extract PIN (simplified logic)
            String pin = "";
            // Start from position 4 (skipping the "04" length indicator)
            for (int i = 2; i < xorResult.length(); i += 2) {
                String byteStr = xorResult.substring(i, i + 2);
                if (byteStr.equals("FF") || byteStr.equals("F0")) {
                    break;
                }

                // Convert hex byte to two digits
                for (int j = 0; j < 2; j++) {
                    char digitChar = byteStr.charAt(j);
                    if (digitChar >= '0' && digitChar <= '9') {
                        pin += digitChar;
                    }
                }
            }

            return pin;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public static byte[] GenerateIPEK(byte[] ksn, byte[] bdk) {
        byte[] result;
        byte[] temp, temp2, keyTemp;

        result = new byte[16];
        temp = new byte[8];
        keyTemp = new byte[16];

//        Array.Copy(bdk, keyTemp, 16);
        System.arraycopy(bdk, 0, keyTemp, 0, 16);   //Array.Copy(bdk, keyTemp, 16);
//        Array.Copy(ksn, temp, 8);
        System.arraycopy(ksn, 0, temp, 0, 8);    //Array.Copy(ksn, temp, 8);
        temp[7] &= 0xE0;
//        TDES_Enc(temp, keyTemp, out temp2);
        temp2 = TriDesEncryption(keyTemp, temp);    //TDES_Enc(temp, keyTemp, out temp2);temp
//        Array.Copy(temp2, result, 8);
        System.arraycopy(temp2, 0, result, 0, 8);   //Array.Copy(temp2, result, 8);
        keyTemp[0] ^= 0xC0;
        keyTemp[1] ^= 0xC0;
        keyTemp[2] ^= 0xC0;
        keyTemp[3] ^= 0xC0;
        keyTemp[8] ^= 0xC0;
        keyTemp[9] ^= 0xC0;
        keyTemp[10] ^= 0xC0;
        keyTemp[11] ^= 0xC0;
//        TDES_Enc(temp, keyTemp, out temp2);
        temp2 = TriDesEncryption(keyTemp, temp);    //TDES_Enc(temp, keyTemp, out temp2);
//        Array.Copy(temp2, 0, result, 8, 8);
        System.arraycopy(temp2, 0, result, 8, 8);   //Array.Copy(temp2, 0, result, 8, 8);
        return result;
    }


    public static byte[] GetDUKPTKey(byte[] ksn, byte[] ipek) {
//    	System.out.println("ksn===" + parseByte2HexStr(ksn));
        byte[] key;
        byte[] cnt;
        byte[] temp;
//    	byte shift;
        int shift;

        key = new byte[16];
//        Array.Copy(ipek, key, 16);
        System.arraycopy(ipek, 0, key, 0, 16);

        temp = new byte[8];
        cnt = new byte[3];
        cnt[0] = (byte) (ksn[7] & 0x1F);
        cnt[1] = ksn[8];
        cnt[2] = ksn[9];
//        Array.Copy(ksn, 2, temp, 0, 6);
        System.arraycopy(ksn, 2, temp, 0, 6);
        temp[5] &= 0xE0;

        shift = 0x10;
        while (shift > 0) {
            if ((cnt[0] & shift) > 0) {
//            	System.out.println("**********");
                temp[5] |= shift;
                NRKGP(key, temp);
            }
            shift >>= 1;
        }
        shift = 0x80;
        while (shift > 0) {
            if ((cnt[1] & shift) > 0) {
//            	System.out.println("&&&&&&&&&&");
                temp[6] |= shift;
                NRKGP(key, temp);
            }
            shift >>= 1;
        }
        shift = 0x80;
        while (shift > 0) {
            if ((cnt[2] & shift) > 0) {
//            	System.out.println("^^^^^^^^^^");
                temp[7] |= shift;
                NRKGP(key, temp);
            }
            shift >>= 1;
        }

        return key;
    }

    /*<summary>
    Non Reversible Key Generatino Procedure
    private function used by GetDUKPTKey
    </summary>
    **/
    private static void NRKGP(byte[] key, byte[] ksn) {

        byte[] temp, key_l, key_r, key_temp;
        int i;

        temp = new byte[8];
        key_l = new byte[8];
        key_r = new byte[8];
        key_temp = new byte[8];

//        Console.Write("");

//        Array.Copy(key, key_temp, 8);
        System.arraycopy(key, 0, key_temp, 0, 8);
        for (i = 0; i < 8; i++) {
            temp[i] = (byte) (ksn[i] ^ key[8 + i]);
        }
//        DES_Enc(temp, key_temp, out key_r);
        key_r = TriDesEncryption(key_temp, temp);
        for (i = 0; i < 8; i++) {
            key_r[i] ^= key[8 + i];
        }

        key_temp[0] ^= 0xC0;
        key_temp[1] ^= 0xC0;
        key_temp[2] ^= 0xC0;
        key_temp[3] ^= 0xC0;
        key[8] ^= 0xC0;
        key[9] ^= 0xC0;
        key[10] ^= 0xC0;
        key[11] ^= 0xC0;

        for (i = 0; i < 8; i++) {
            temp[i] = (byte) (ksn[i] ^ key[8 + i]);
        }
//        DES_Enc(temp, key_temp, out key_l);
        key_l = TriDesEncryption(key_temp, temp);
        for (i = 0; i < 8; i++) {
            key[i] = (byte) (key_l[i] ^ key[8 + i]);
        }
//        Array.Copy(key_r, 0, key, 8, 8);
        System.arraycopy(key_r, 0, key, 8, 8);
    }

    /*<summary>
    Get current Data Key variant
    Data Key variant is XOR DUKPT Key with 0000 0000 00FF 0000 0000 0000 00FF 0000
    </summary>
    <param name="ksn">Key serial number(KSN). A 10 bytes data. Which use to determine which BDK will be used and calculate IPEK. With different KSN, the DUKPT system will ensure different IPEK will be generated.
    Normally, the first 4 digit of KSN is used to determine which BDK is used. The last 21 bit is a counter which indicate the current key.</param>
    <param name="ipek">IPEK (16 byte).</param>
    <returns>Data Key variant (16 byte)</returns>
    **/
    public static byte[] GetDataKeyVariant(byte[] ksn, byte[] ipek) {
        byte[] key;

        key = GetDUKPTKey(ksn, ipek);
        key[5] ^= 0xFF;
        key[13] ^= 0xFF;

        return key;
    }

    /*<summary>
    Get current PIN Key variant
    PIN Key variant is XOR DUKPT Key with 0000 0000 0000 00FF 0000 0000 0000 00FF
    </summary>
    <param name="ksn">Key serial number(KSN). A 10 bytes data. Which use to determine which BDK will be used and calculate IPEK. With different KSN, the DUKPT system will ensure different IPEK will be generated.
    Normally, the first 4 digit of KSN is used to determine which BDK is used. The last 21 bit is a counter which indicate the current key.</param>
    <param name="ipek">IPEK (16 byte).</param>
    <returns>PIN Key variant (16 byte)</returns>
    **/
    public static byte[] GetPinKeyVariant(byte[] ksn, byte[] ipek) {
        byte[] key;

        key = GetDUKPTKey(ksn, ipek);
        key[7] ^= 0xFF;
        key[15] ^= 0xFF;

        return key;
    }

    public static byte[] GetMacKeyVariant(byte[] ksn, byte[] ipek) {
        byte[] key;

        key = GetDUKPTKey(ksn, ipek);
        key[6] ^= 0xFF;
        key[14] ^= 0xFF;

        return key;
    }

    public static byte[] GetDataKey(byte[] ksn, byte[] ipek) {
        byte[] temp1 = GetDataKeyVariant(ksn, ipek);
        byte[] temp2 = temp1;

        byte[] key = TriDesEncryption(temp2, temp1);

        return key;
    }

    /*
     * 3DES encryption
     **/
    public static byte[] TriDesEncryption(byte[] byteKey, byte[] dec) {

        try {
            byte[] en_key = new byte[24];
            if (byteKey.length == 16) {
                System.arraycopy(byteKey, 0, en_key, 0, 16);
                System.arraycopy(byteKey, 0, en_key, 16, 8);
            } else if (byteKey.length == 8) {
                System.arraycopy(byteKey, 0, en_key, 0, 8);
                System.arraycopy(byteKey, 0, en_key, 8, 8);
                System.arraycopy(byteKey, 0, en_key, 16, 8);
            } else {
                en_key = byteKey;
            }
            SecretKeySpec key = new SecretKeySpec(en_key, "DESede");

            Cipher ecipher = Cipher.getInstance("DESede/ECB/NoPadding");
            ecipher.init(Cipher.ENCRYPT_MODE, key);

            // Encrypt
            byte[] en_b = ecipher.doFinal(dec);

            // String en_txt = parseByte2HexStr(en_b);
            // String en_txt =byte2hex(en_b);
            return en_b;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /*
     * 3DES decryption CBC
     **/
    public static byte[] TriDesDecryptionCBC(byte[] byteKey, byte[] dec) {
        byte[] en_key = new byte[24];
        if (byteKey.length == 16) {
            System.arraycopy(byteKey, 0, en_key, 0, 16);
            System.arraycopy(byteKey, 0, en_key, 16, 8);
        } else if (byteKey.length == 8) {
            System.arraycopy(byteKey, 0, en_key, 0, 8);
            System.arraycopy(byteKey, 0, en_key, 8, 8);
            System.arraycopy(byteKey, 0, en_key, 16, 8);
        } else {
            en_key = byteKey;
        }

        try {
            Key deskey = null;
            byte[] keyiv = new byte[8];
            DESedeKeySpec spec = new DESedeKeySpec(en_key);
            SecretKeyFactory keyfactory = SecretKeyFactory.getInstance("desede");
            deskey = keyfactory.generateSecret(spec);

            Cipher cipher = Cipher.getInstance("desede" + "/CBC/NoPadding");
            IvParameterSpec ips = new IvParameterSpec(keyiv);

            cipher.init(Cipher.DECRYPT_MODE, deskey, ips);

            byte[] de_b = cipher.doFinal(dec);

            return de_b;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /*
     * 3DES decryption ECB
     **/
    public static byte[] TriDesDecryptionECB(byte[] byteKey, byte[] dec) {
        // private String TriDesDecryption(String dnc_key, byte[] dec){
        // byte[] byteKey = parseHexStr2Byte(dnc_key);
        byte[] en_key = new byte[24];
        if (byteKey.length == 16) {
            System.arraycopy(byteKey, 0, en_key, 0, 16);
            System.arraycopy(byteKey, 0, en_key, 16, 8);
        } else if (byteKey.length == 8) {
            System.arraycopy(byteKey, 0, en_key, 0, 8);
            System.arraycopy(byteKey, 0, en_key, 8, 8);
            System.arraycopy(byteKey, 0, en_key, 16, 8);
        } else {
            en_key = byteKey;
        }
        SecretKey key = null;

        try {
            key = new SecretKeySpec(en_key, "DESede");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        try {
            Cipher dcipher = Cipher.getInstance("DESede/ECB/NoPadding");
            dcipher.init(Cipher.DECRYPT_MODE, key);

            // byte[] dec = parseHexStr2Byte(en_data);

            // Decrypt
            byte[] de_b = dcipher.doFinal(dec);

            // String de_txt = parseByte2HexStr(removePadding(de_b));
            return de_b;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /*
     * convert hexadecimal string to byte array
     **/
    public static byte[] parseHexStr2Byte(String hexStr) {
        if (hexStr.length() < 1) {
            return null;
        }
        byte[] result = new byte[hexStr.length() / 2];
        for (int i = 0; i < hexStr.length() / 2; i++) {
            int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
            int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2),
                    16);
            result[i] = (byte) (high * 16 + low);
        }
        return result;
    }

    /*
     * convert byte array to hexadecimal string
     **/
    public static String parseByte2HexStr(byte buf[]) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < buf.length; i++) {
            String hex = Integer.toHexString(buf[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex.toUpperCase());
        }
        return sb.toString();
    }

    /*
     * data fill
     **/
    public static String dataFill(String dataStr) {
        int len = dataStr.length();
        if (len % 16 != 0) {
            dataStr += "80";
            len = dataStr.length();
        }
        while (len % 16 != 0) {
            dataStr += "0";
            len++;
            System.out.println(dataStr);
        }
        return dataStr;
    }


    public static String xor(String key1, String key2) {
        String result = "";

        byte[] arr1 = parseHexStr2Byte(key1);
        byte[] arr2 = parseHexStr2Byte(key2);
        byte[] arr3 = new byte[arr1.length];

        for (int i = 0; i < arr1.length; i++) {
            arr3[i] = (byte) (arr1[i] ^ arr2[i]);
        }

        result = parseByte2HexStr(arr3);
        return result;
    }
    public static String decodeTrack1(String compressedTrack1) {
        String resultTrack1 = "" ;

        for(int i = 0; i<compressedTrack1.length()/6; i++) {
            //1. convert every 6chars(3bytes) to binary string
            String sub = compressedTrack1.substring(i * 6, (i + 1) * 6);
            int threeByteInt = Integer.parseInt(sub, 16);

            BigInteger bigInter = BigInteger.valueOf(threeByteInt);
            String strBinary = bigInter.toString(2);

            //BigInteger.toString(radix) will miss leading 0s, so need padding 0 at the begging with length of 3byte(24 bits)
            String withLeadingZeros = String.format("%24s", strBinary).replace(' ', '0');

            //2. group binary result on every 6 binary chars into 4 groups (bytes)
            byte[] fourBytes = new byte[]{0x00, 0x00, 0x00, 0x00};
            for (int j = 0; j < withLeadingZeros.length() / 6; j++) {
                String byteStr = withLeadingZeros.substring(j * 6, (j + 1) * 6);
                fourBytes[j] = Byte.parseByte(byteStr, 2);
                fourBytes[j] += 0x20;

//                System.out.println(byteStr + "->" + fourBytes[j]);
            }

            //3. append each 4bytes array to result string
            resultTrack1 += new String(fourBytes);
        }

        return resultTrack1;
    }

    public static String extractPanDigits(String pan) {
        // Remove any spaces or non-digit characters
        String cleanPan = pan.replaceAll("[^0-9]", "");

        // Validate PAN length (should be 16 digits for standard PAN)
        if (cleanPan.length() != 16) {
            throw new IllegalArgumentException("Invalid PAN length. Expected 16 digits, got " + cleanPan.length());
        }

        // Extract 12 rightmost digits without the last digit (checksum)
        // PAN structure: 6 digits IIN + 10 digits account number (last digit is checksum)
        // We want digits 4-15 (0-based indexing), which gives us 12 digits
        return cleanPan.substring(4, 16);
    }

    //Test
    // Your XOR method (correct as-is)
    public static String _xor(String key1, String key2) {
        String result = "";

        byte[] arr1 = parseHexStr2Byte_(key1);
        byte[] arr2 = parseHexStr2Byte_(key2);
        byte[] arr3 = new byte[arr1.length];

        for (int i = 0; i < arr1.length; i++) {
            arr3[i] = (byte) (arr1[i] ^ arr2[i]);
        }

        result = parseByte2HexStr_(arr3);
        return result;
    }

    // Required helper methods
    public static byte[] parseHexStr2Byte_(String hexStr) {
        if (hexStr == null || hexStr.length() % 2 != 0) {
            throw new IllegalArgumentException("Invalid hex string");
        }

        byte[] bytes = new byte[hexStr.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int index = i * 2;
            bytes[i] = (byte) Integer.parseInt(hexStr.substring(index, index + 2), 16);
        }
        return bytes;
    }

    public static String parseByte2HexStr_(byte[] buf) {
        StringBuilder sb = new StringBuilder();
        for (byte b : buf) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    // Method to format PAN according to ANSI X9.8
    public static String formatPAN(String pan) {
        // Extract 12 rightmost digits without checksum
        String cleanPan = pan.replaceAll("[^0-9]", "");
        String twelveDigits;

        if (cleanPan.length() == 19) {
            twelveDigits = cleanPan.substring(7, 19); // 365212501000
        } else if (cleanPan.length() == 16) {
            twelveDigits = cleanPan.substring(4, 16);
        } else {
            throw new IllegalArgumentException("Unsupported PAN length");
        }

        // Add "0000" to the left
        return "0000" + twelveDigits; // 0000365212501000
    }

    // Method to extract PIN from XOR result
    public static String extractPIN(String xorResult) {
        // The PIN starts after the first nibble (which indicates PIN length)
        // and continues until we encounter 'F'
        String pin = "";
        for (int i = 2; i < xorResult.length(); i += 2) {
            String byteStr = xorResult.substring(i, i + 2);
            if (byteStr.equals("FF")) {
                break;
            }
            // Each byte contains 2 PIN digits in BCD format
            pin += byteStr;
        }
        return pin;
    }

    public static void main(String[] args)  {
//        07-08 17:50:27.306 12376-12376/com.dspread.demoui D/POS_SDK: onRequestOnlineProcess5F201A20202020202020202020202020202020202020202020202020204F08A0000003330101015F24032612319F160F4243544553542031323334353637389F21031750229A031907089F02060000000001119F03060000000000009F34030203009F120A50424F432044454249549F0607A00000033301015F300202209F4E0F616263640000000000000000000000C408622622FFFFFF3603C10A09118012400705E00002C708DBD7F58811779698C00A09118012400705E00001C28201880C54D377643A72400707E993BDEB6AFD891CFD5EC8CA03A251DF9301E70F76999ADABCECF859C26B9320724644D15B53BDE669414C7C8336EFDC0892A6F883DB5163D0613557949D66349BB6CB6BBCD8877017D3FEF5404C4446F2F2244CB62C62CAAE6EB86F99C9F31E69DB32BBDA2390A73EA907E4D8BDEED105E876319F4D17A5DE1788B0DA32730E4102F42A7232BE4D9D5E7BF46E7313C0F190E4F7A7D320D29DD3765E06DB5FE847C8B2B5ABBBAC0B22E5C9722303EF6E1C050C33B4F88D1BE8E79A8FBACA1086E466CB79A54A528DF53D98DA85E79EACAC4F464B0BC2941A540E1E6DFA47D4D369F50BEECFDC37AED04F63500BED4D4DB524E69345F6FE94A1CB2353D39959953393ADDD7930A43E2FCC3AE8AB348B0A8025C63C8650AF6F7C2F613EEF31549B6E073898D256815A851B5C39341B609BB3DB9974985550F096DEA5440B429BB0346D93FC25A17441F27F219A4004EE2A244014434E5D17B9F645CACB534E0CF7D3D555EE861780CF33A674D0A9A04C523C85D3F8062CE34309514A32F2AA

//        String tlvDate = "104C52518AEDAE281784EA3F4D6892C9ACF31C445668E6C8D9F6F10FE6B3EB9EE8CAA19BCE363CCFC5729B5E282F6587AB86745B7E0D1671943F9049E975B0DDF2D45CEF743817BED492E8B64E4E3459AEB8895D21DAD51F845A36C9395E830F1E06B586048106063315ECA14437F791D0B67E70A33745AC3168FF4F5D7C558C72ECB2FE5A0F64A3AA7DF1FB02FFB0CAF473F143E1ED716A2D995AC21E91225D2A86630E929F027FF08EFAAFC56187D91AFB2DA19F7829616D14215F20F0B2F075A3B8AD4FF2153D57B20711D92DBDBD2905BB9C18AE8B7CDE606D38675382A582304ABFEF7E2DB8437C247D9B269E7D4D8FD153AC370E45317FA3014C8E909ADB531C95E05B81E8AE18C70CE0979CC20BF6E54E326F82231859AF369DA96D7BF65A51CD6C8BBC46E9E48BAF67499F1DEE395BE06AA9E56F762A9698768109C4EBC90EB976DC99886E09BAEDCBD7365F2735B6022756D4B6D1AE76782D3E15788607C0C03665F332";
//        String tlvDate = "5f200a46414e2f4a49554855414f07a00000000310105f24032110319f160f4243544553543132333435363738009f21030542259a037005079f02060000000011119f03060000000000009f34031f03009f120b56495341204352454449549f0607a00000000310105f300202019f4e0f616263640000000000000000000000c408451461ffffff2125c00a00000332100300e0001dc22045e76e7f539c7ae82061b909dcc05b5151210784da7fe1ad82b3b5a9fa14c6e2d0105214696f298eddf4b12519f8d185a01e";
//        List<TLV> parse = TLVParser.parse(tlvDate);

        //c0
//        String onLineksn = TLVParser.searchTLV(parse, "c0").value;
//        //c2
//		String onLineblockData = TLVParser.searchTLV(parse, "c2").value;
//        //c1
//        String Pinksn = TLVParser.searchTLV(parse, "c1").value;
//        //c7
//        String pinblockData = TLVParser.searchTLV(parse, "c7").value;

//        String pin = getDate(Pinksn, pinblockData, Enum_key.PIN, Enum_mode.ECB);
//        String a = "123";
//        System.out.println(a == "123");
//        String onLinedate = getDate("00219090600483E0000F", tlvDate, Enum_key.DATA, Enum_mode.CBC);
//        System.out.println(onLinedate);

//        parse = TLVParser.parse(onLinedate);
//
//        String realPan = TLVParser.searchTLV(parse, "5A").value;
//		String parsCarN = "0000" + realPan.substring(realPan.length() - 13, realPan.length() - 1);
//		String realPin = xor(parsCarN, pin);
//        System.out.println(pin);

    }


}
