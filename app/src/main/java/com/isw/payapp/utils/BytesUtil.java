package com.isw.payapp.utils;

import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author GuoJirui.
 * @date 2021/4/26.
 * @desc
 */
public class BytesUtil {
    private static final String EMPTY_STRING = "";
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    /**
     * Byte array to hex string
     *
     * @param data Byte array
     * @return Hex string
     */
    public static String bytes2HexString(byte[] data) {
        if (isNullEmpty(data)) {
            return EMPTY_STRING;
        }
        StringBuilder buffer = new StringBuilder();
        for (byte b : data) {
            String hex = Integer.toHexString(b & 0xff);
            if (hex.length() == 1) {
                buffer.append('0');
            }
            buffer.append(hex);
        }
        return buffer.toString().toUpperCase();
    }


    /**
     * Hex string to byte array
     *
     * @param data Hex string
     * @return Byte array
     */
    public static byte[] hexString2Bytes(String data) {
        if (isNullEmpty(data)) {
            return EMPTY_BYTE_ARRAY;
        }

        byte[] result = new byte[(data.length() + 1) / 2];
        if ((data.length() & 1) == 1) {
            data += "0";
        }
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) (hex2byte(data.charAt(i * 2 + 1)) | (hex2byte(data.charAt(i * 2)) << 4));
        }
        return result;
    }

    /**
     * Multiple data merge
     *
     * @param data Data Array
     * @return An array of merged data
     */
    public static byte[] merge(byte[]... data) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            for (byte[] d : data) {
                if (d == null) {
                    throw new IllegalArgumentException("");
                }
                buffer.write(d);
            }
            return buffer.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                buffer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }


    /**
     * Converts an integer to a 4-byte array in small-endian mode
     *
     * @param intValue integer
     * @return byte array
     */
    public static byte[] intToBytesByLow(int intValue) {
        byte[] bytes = new byte[4];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) ((intValue >> ((3 - i) << 3)) & 0xFF);
        }
        return bytes;
    }

    /**
     * Get a sub array
     *
     * @param data   data
     * @param offset offset （0-data.length）
     * @param len    length
     * @return sub array
     */
    public static byte[] subBytes(byte[] data, int offset, int len) {
        if (isNullEmpty(data)) {
            return null;
        }

        if (offset < 0 || data.length <= offset) {
            return null;
        }

        if (len < 0 || data.length < offset + len) {
            len = data.length - offset;
        }

        byte[] ret = new byte[len];

        System.arraycopy(data, offset, ret, 0, len);
        return ret;
    }

    public static boolean isNullEmpty(byte[] array) {
        return (array == null) || (array.length == 0);
    }

    public static boolean isNullEmpty(String str) {
        return TextUtils.isEmpty(str);
    }

    /**
     * Hex char to byte
     *
     * @param hex Hex char
     * @return Byte
     */
    public static byte hex2byte(char hex) {
        if (hex <= 'f' && hex >= 'a') {
            return (byte) (hex - 'a' + 10);
        }

        if (hex <= 'F' && hex >= 'A') {
            return (byte) (hex - 'A' + 10);
        }

        if (hex <= '9' && hex >= '0') {
            return (byte) (hex - '0');
        }

        return 0;
    }


    /**
     * Byte to hex char
     *
     * @param data byte
     * @return hex char
     */
    public static String byte2Hex(byte data) {
        StringBuilder buffer = new StringBuilder();
        String hex = Integer.toHexString(data & 0xff);
        if (hex.length() == 1) {
            buffer.append('0');
        }
        buffer.append(hex);
        return buffer.toString().toUpperCase();
    }


    /**
     * Special, handle return codes
     *
     * @param code code
     * @return
     */
    public static String int2HexString(int code) {
        return "0x" + bytes2HexString(intToBytesByLow(code));
    }
}
