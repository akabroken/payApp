package com.isw.payapp.devices.dspread.utils;

import android.text.TextUtils;

import com.dspread.print.util.TRACE;

import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public final class Utils {
    private static String	digits = "0123456789abcdef";

    /**
     * Return length many bytes of the passed in byte array as a hex string.
     *
     * @param data the bytes to be converted.
     * @param length the number of bytes in the data block to be converted.
     * @return a hex representation of length bytes of data.
     */
    public static String toHex(byte[] data, int length)
    {
        StringBuffer	buf = new StringBuffer();

        for (int i = 0; i != length; i++)
        {
            int	v = data[i] & 0xff;

            buf.append(digits.charAt(v >> 4));
            buf.append(digits.charAt(v & 0xf));
        }

        return buf.toString();
    }

    /**
     * Return the passed in byte array as a hex string.
     *
     * @param data the bytes to be converted.
     * @return a hex representation of data.
     */
    public static String toHex(byte[] data)
    {
        return toHex(data, data.length);
    }

    public static String bytes2Hex(byte[] data){

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            if ((i & 0x0f) == 0) {
                sb.append("\n");
            }
            sb.append(String.format("%02X", data[i]));
        }

        return sb.toString();
    }
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();

        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte)
                    ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));


        }
        return data;
    }
    /*
     * v must be less than 100 and great than -1
     **/
    public static byte int2d2Bcd(int v) {
        return (byte)(((v / 10) << 4) | ((v % 10) & 0x0F));
    }

    /*
     * v must be less than 16 and greater than -1
     **/
    public static byte int1d2leftbyte(byte b, int v) {
        return (byte) ((b & 0x0f) | ((v & 0x0f) << 4));
    }

    /*
     * v must be less than 16 and greater than -1
     **/
    public static byte int1d2rightbyte(byte b, int v) {
        return (byte) ((b & 0xf0) | (v & 0x0f));
    }

    public static int byteright2int(byte b) {
        return b & 0x0f;
    }

    /*
     * left aligned, v must be less than 100 and great than -1
     **/
    public static byte int2d2Bcdl(int v){
        if (v < 10) {
            v *= 10;
        }
        return (byte)(((v / 10) << 4) | ((v % 10) & 0x0F));
    }

    /* v must be great than 0 */
    public static byte[] int2Bcd(int v) {
        int n = countBytes(v);
        byte[] data = new byte[n];

        for (int i = n - 1; i > -1; i--) {
            data[i] = int2d2Bcd(v % 100);
            v /= 100;
        }

        return data;
    }

    /*
     * v must be great than 0
     **/
    public static int int2Bcd(int v, byte[] data) {
        int nb = countBytes(v);
        if (nb > data.length) {
            return -1;
        }

        int n = data.length;
        int i = n - 1;

        for (; i > n - nb - 1; i--) {
            data[i] = int2d2Bcd(v % 100);
            v /= 100;
        }

        for (; i > -1; i--) {
            data[i] = 0;
        }

        return 0;
    }

    /*
     * v must be great than 0
     **/
    public static int int2Bcd(int v, byte[] data, int offset, int length) {
        int nb = countBytes(v);
        if (nb > length) {
            return -1;
        }

        int last = offset + length - 1 ;
        int i = last;

        for (; i > last - nb; i--) {
            data[i] = int2d2Bcd(v % 100);
            v /= 100;
        }

        for (; i >= offset; i--) {
            data[i] = 0;
        }

        return 0;
    }

    /*
     * v must be great than 0
     **/
    public static int long2Bcd(long v, byte[] data, int offset, int length) {
        int nb = countBytes(v);
        if (nb > length) {
            return -1;
        }

        int last = offset + length - 1 ;
        int i = last;

        for (; i > last - nb; i--) {
            data[i] = int2d2Bcd((int)(v % 100));
            v /= 100;
        }

        for (; i >= offset; i--) {
            data[i] = 0;
        }

        return 0;
    }

    public static int numstr2Bcd(byte[] numstr, byte[] data, int offset, int length) {
        int vlen = numstr.length;
        int nb = (vlen + 1) >> 1;
        if (nb > length) {
            return -1;
        }

        byte[] str = numstr;

        int vlast = vlen;
        if ((vlen & 0x01) != 0) {
            vlast--;
        }

        int p = offset;
        int i = 0;
        for (; i < vlast; i += 2) {
            data[p++] = (byte) ((((str[i] - 0x30) & 0x0f) << 4) | ((str[i+1] - 0x30) & 0x0f));
        }

        if ((vlen & 0x01) != 0) {
            data[p++] = (byte) (((str[i] - 0x30) & 0x0f) << 4);
        }

        return 0;
    }

    /*
     * left aligned, v must be great than 0
     **/
    public static byte[] int2Bcdl(int v) {
        int numbers = countNumbers(v);
        if ((numbers & 0x01) == 1) {
            v *= 10;
        }

        byte[] data = int2Bcd(v);

        return data;
    }

    /*
     * left aligned, v must be great than 0
     **/
    public static int int2Bcdl(int v, byte[] data) {
        int numbers = countNumbers(v);
        if ((numbers & 0x01) == 1) {
            v *= 10;
        }

        byte[] vd = int2Bcd(v);
        if (vd.length > data.length) {
            return -1;
        }

        int i = 0;
        for (; i < vd.length; i++) {
            data[i] = vd[i];
        }
        for (; i < data.length; i++) {
            data[i] = 0;
        }

        return 0;
    }

    public static int countBytes(long v) {
        int n = 0;
        while (v > 0) {
            n++;
            v /= 100;
        }
        return n;
    }

    public static int countNumbers(long v) {
        int n = 0;
        while (v > 0) {
            n++;
            v /= 10;
        }
        return n;
    }

    public static int bcd2Int(byte b){
        return ((b >> 4) & (byte)0x0F) * 10 + (b & 0x0F);
    }

    public static int bcd2Int(byte b1, byte b2){
        return bcd2Int(b1) * 100 + bcd2Int(b2);
    }

    public static int bcd2Int(byte[] data, int offset, int length) {
        if (length > 4) {
            /* numbers of int value should not be great than 8 */
            return -1;
        }

        int p = offset;
        int last = offset + length;
        int v = 0;
        while (p < last) {
            if (p + 1 < last) {
                /* at lest 2 bytes remained */
                v = v * 10000 + bcd2Int(data[p], data[p+1]);
                p += 2;
            } else {
                /* only one byte remained */
                v = v * 100 + bcd2Int(data[p]);
                p++;
            }
        }
        return v;
    }

    public static long bcd2Long(byte[] data, int offset, int length) {
        // long value has 19 numbers; the leftmost number is 9
        if (length > 9) {
            /* numbers of int value should not be great than 8 */
            return -1;
        }

        int p = offset;
        int last = offset + length;
        if ((length & 0x01) != 0) {
            last--;
        }

        long v = 0;
        while (p < last) {
            v = v * 10000 + (long)bcd2Int(data[p], data[p+1]);
            p += 2;
        }

        if ((length & 0x01) != 0) {
            v = v * 100 + (long)bcd2Int(data[p]);
            p++;
        }

        return v;
    }

    public static int bcd2Numstr(byte[] data, int offset, int length, byte[] numstr, int strlen) {
        int maxstrlen = (length << 1);
        maxstrlen = maxstrlen < strlen ? maxstrlen : strlen;
        maxstrlen = maxstrlen < numstr.length ? maxstrlen : numstr.length;

        int p = offset;
        int vlast = maxstrlen;
        if ((strlen & 0x01) != 0) {
            vlast--;
        }

        int i = 0;
        for (;i < vlast; i += 2) {
            numstr[i] = (byte) (((data[p] & 0xf0) >>> 4) + 0x30);
            numstr[i+1] = (byte) ((data[p] & 0x0f) + 0x30);
            p++;
        }

        if ((strlen & 0x01) != 0) {
            numstr[i] = (byte) (((data[p] & 0xf0) >>> 4) + 0x30);
        }

        return maxstrlen;
    }

    public static byte[] int2Byte(int intValue) {
        byte[] b = new byte[4];
        byte[] r = new byte[4];

        for(int i = 0; i < 4; ++i) {
            b[i] = (byte)(intValue >> 8 * (3 - i) & 255);
        }

        r[3] = b[0];
        r[2] = b[1];
        r[1] = b[2];
        r[0] = b[3];
        return r;
    }
    //将hex值转为ascii码
    public static  String convertHexToString(String hex){

        StringBuilder sb = new StringBuilder();
        StringBuilder temp = new StringBuilder();

        //49204c6f7665204a617661 split into two characters 49, 20, 4c...
        for( int i=0; i<hex.length()-1; i+=2 ){
            //grab the hex in pairs
            String output = hex.substring(i, (i + 2));
            //convert hex to decimal
            int decimal = Integer.parseInt(output, 16);
            //convert the decimal to character
            sb.append((char)decimal);

            temp.append(decimal);
        }
        return sb.toString();
    }
    public static String getStr(String string){

        if(string.endsWith("f")||string.endsWith("F")){
            string= string.substring(0,string.length()-1);
        }
        return string;
    }

    //	private static final int MIN_CLICK_DELAY_TIME = 2000;
    private static long lastClickTime;
    //
//	public static boolean isFastClick() {
//		boolean flag = false;
//		long curClickTime = System.currentTimeMillis();
//		if ((curClickTime - lastClickTime) >= MIN_CLICK_DELAY_TIME) {
//			flag = true;
//		}
//		lastClickTime = curClickTime;
//		return flag;
//	}
    public static int MIN_CLICK_DELAY_TIME2 = 3000;
    public static boolean islistFastClick() {
        TRACE.i("islistFastClick");
        boolean flag = false;
        long curClickTime = System.currentTimeMillis();
        if ((curClickTime - lastClickTime) >= MIN_CLICK_DELAY_TIME2) {
            flag = true;
        }
        MIN_CLICK_DELAY_TIME2 = 00;
        lastClickTime = curClickTime;
        return flag;
    }

    public static int getKeyIndex() {
//        String s = mKeyIndex.getText().toString();
        String s = "";
        if (TextUtils.isEmpty(s)) {
            return 0;
        }
        int i = 0;
        try {
            i = Integer.parseInt(s);
            if (i > 9 || i < 0) {
                i = 0;
            }
        } catch (Exception e) {
            i = 0;
            return i;
        }
        return i;
    }


    public static String byte2HexStr(byte[] var0) {
        if (var0 == null) {
            return "";
        } else {
            String var1 = "";
            StringBuilder var2 = new StringBuilder("");

            for (int var3 = 0; var3 < var0.length; ++var3) {
                var1 = Integer.toHexString(var0[var3] & 255);
                var2.append(var1.length() == 1 ? "0" + var1 : var1);
            }

            return var2.toString().toUpperCase().trim();
        }
    }
    private static byte[] TDES_Dec(byte[] data, byte[] key) {
        try {
            Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding");
            Key secretKey = new SecretKeySpec(key, "DESede");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return cipher.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

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
    public static byte[] decrypt_icc_info(String ksn, String data)
    {
        parseHexStr2Byte
        //0123456789ABCDEFFEDCBA9876543210
        byte[] BDK = hexToBytes("0123456789ABCDEFFEDCBA9876543210");//70202309280476e00027
        byte[] ksnBytes = hexToBytes(ksn);
        byte[] dataBytes = hexToBytes(data);
        byte[] IPEK = DUKPK2009_CBC.GenerateIPEK(ksnBytes, BDK);
        byte[] DATA_KEY = DUKPK2009_CBC.GetDataKey(ksnBytes, IPEK);
        //System.out.println(bytesToHex(DATA_KEY));
        byte[] res = TDES_Dec(dataBytes, DATA_KEY);
        return res;
    }

    */
    public static byte[] decrypt_icc_info(String ksn, String data)
    {

        //0123456789ABCDEFFEDCBA9876543210
        byte[] BDK = parseHexStr2Byte("0123456789ABCDEFFEDCBA9876543210");//70202309280476e00027
        byte[] ksnBytes = parseHexStr2Byte(ksn);
        byte[] dataBytes = parseHexStr2Byte(data);
        byte[] IPEK = DUKPK2009_CBC.GenerateIPEK(ksnBytes, BDK);
        byte[] DATA_KEY = DUKPK2009_CBC.GetDataKey(ksnBytes, IPEK);
        //System.out.println(bytesToHex(DATA_KEY));
        //byte[] res = TDES_Dec(dataBytes, DATA_KEY);
        byte[] res = DUKPK2009_CBC.TriDesDecryptionECB(dataBytes, DATA_KEY);
        return res;
    }
}
