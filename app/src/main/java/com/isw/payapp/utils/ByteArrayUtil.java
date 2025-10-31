package com.isw.payapp.utils;

import java.io.UnsupportedEncodingException;

/**
 * Created by liangjz on 2016/3/22.
 */
public class ByteArrayUtil {

    /**
     * 对比src从指定locate位置开始是否是com字节数组
     * @param src
     * @param locate
     * @param com
     * @return
     */
    static final String HEXES = "0123456789ABCDEF";
    public static boolean ByteArrayCmp(byte[] src, int locate, byte[] com){
        if (src.length - locate<com.length){
            return false;
        }
        for (int i =0;i<com.length;i++){
            if (src[i+locate] != com[i] ){
                return false;
            }
        }
        return true;
    }

    /**
     * 去掉 byte_1 前 lenToTrim 个字节
     * @param byte_1
     * @param lenToTrim
     * @return 去掉后的数组
     */
    public static byte[] byteArrayTrimHead(byte[] byte_1, int lenToTrim) {
        if (byte_1.length < lenToTrim) {
            return null;
        }
        byte[] byte_3 = new byte[byte_1.length - lenToTrim];
        System.arraycopy(byte_1, lenToTrim, byte_3, 0, byte_1.length - lenToTrim);
        return byte_3;
    }
    /**
     * 获取 byte_1 前 lenToTrim 个字节
     * @param byte_1
     * @param lenToTrim
     * @return 前lenToTrim个字节的数组
     */
    public static byte[] byteArrayGetHead(byte[] byte_1, int lenToTrim) {
        if (byte_1.length < lenToTrim) {
            return null;
        }
        byte[] byte_3 = new byte[lenToTrim];
        System.arraycopy(byte_1, 0, byte_3, 0, lenToTrim);
        return byte_3;
    }

    /**
     * 合并两个数组
     * @param byte_1
     * @param byte_2
     * @return 合并后的数组
     */
    public static byte[] byteMerger(byte[] byte_1, byte[] byte_2) {
        byte[] byte_3 = new byte[byte_1.length + byte_2.length];
        System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);
        System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);
        return byte_3;
    }

    /**
     * 复制src字节数组到dest，长度取两个数据的最小长度
     * @param dest
     * @param src
     * @return
     */

    public static boolean byteArrayCopy(byte[] dest, byte[]src){
        if (dest == null || src == null){
            return false;
        }
        if (dest.length<1 || src.length <1){
            return false;
        }
        int dest_len = dest.length;
        int src_len=src.length;
        int len = MIN(dest_len,src_len);
        System.arraycopy(src,0,dest,0,len);
        return true;
    }

    public static int MIN(int a,int b){
        if (a<b){
            return a;
        }else {
            return b;
        }
    }

    /**
     * Mispos use "GBK" Character encoding
     *
     * Mispos使用GBK编码
     * @param text
     * @return
     */
    public static byte[] getMisPosTextByte(String text){
        byte[] msgDisplay = null;
        try {
            msgDisplay = text.getBytes("GBK");
        }catch (UnsupportedEncodingException e){
            e.printStackTrace();
        }
        return msgDisplay;
    }

    public static String byteArray2Hex(byte[] raw) {
        if (raw == null) {
            return null;
        }
        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (final byte b : raw) {
            hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
        }
        return hex.toString();
    }

    /**
     * convert int to byte[]
     * @param i need to be converted to byte array
     * @return byte array
     */
    public static byte[] intToByteArray(int i) {
        byte[] result = new byte[2];
//		result[0] = (byte)((i >> 24) & 0xFF);
//		result[1] = (byte)((i >> 16) & 0xFF);
        result[0] = (byte)((i >> 8) & 0xFF);
        result[1] = (byte)(i & 0xFF);
        return result;
    }

    /**
     * convert int to byte[]
     * @param i need to be converted to byte array
     * @return byte array
     */
//    public static byte[] intToByteArray(int i) {
//        byte[] result = new byte[2];
////		result[0] = (byte)((i >> 24) & 0xFF);
////		result[1] = (byte)((i >> 16) & 0xFF);
//        result[0] = (byte)((i >> 8) & 0xFF);
//        result[1] = (byte)(i & 0xFF);
//        return result;
//    }
}
