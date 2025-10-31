package com.isw.payapp.devices.feitian.utils;

import com.isw.payapp.utils.BytesUtil;

public class FTLV {
    /**
     * Generate a TLVList object from the TLV binary data, which can contain 0~n TLV objects
     *
     * @param data TLV binary data
     * @return TLV objects
     */
    public static FTLVList fromBinary(byte[] data) {
        FTLVList l = new FTLVList();
        int offset = 0;
        while (offset < data.length) {
            FTLVElement d = fromRawData(data, offset);
            l.addTLV(d);
            offset += d.getRawData().length;
        }
        return l;
    }

    /**
     * Generate a TLVList object from the hex string, which can contain 0~n TLV objects
     *
     * @param data the hex string
     * @return TLV objects
     */
    public static FTLVList fromData(String data) {
        return fromBinary(BytesUtil.hexString2Bytes(data));
    }


    /**
     * Extract the valid values from the original data and generate a TLV object
     *
     * @param data   the original data
     * @param offset offset
     * @return a TLV object
     */
    public static FTLVElement fromRawData(byte[] data, int offset) {
        int tLen = getTLength(data, offset);
        int lLen = getLLength(data, offset + tLen);
        int vLen = calcValueLength(data, offset + tLen, lLen);
        int len = tLen + lLen + vLen;
        FTLVElement element = new FTLVElement();
        byte[] rawData = BytesUtil.subBytes(data, offset, len);
        element.setData(rawData);
        element.setTag(BytesUtil.bytes2HexString(BytesUtil.subBytes(rawData, 0, tLen)));
        element.setLength(vLen);
        element.setValue(BytesUtil.subBytes(rawData, rawData.length - vLen, vLen));
        return element;
    }

    /**
     * Only tagName and value are provided to generate a TLV object
     *
     * @param tagName tag
     * @param value   value
     * @return a TLV object
     */
    public static FTLVElement fromData(String tagName, byte[] value) {
        byte[] tag = BytesUtil.hexString2Bytes(tagName);
        FTLVElement element = new FTLVElement();
        element.setData(BytesUtil.merge(tag, makeLengthData(value.length), value));
        element.setTag(tagName);
        element.setLength(value.length);
        element.setValue(value);
        return element;
    }


    /**
     * Generates an array of bytes of data length
     *
     * @param len data length
     * @return
     */
    private static byte[] makeLengthData(int len) {
        if (len > 127) {
            byte[] tempLen = BytesUtil.intToBytesByLow(len);
            int start = 0;
            for (int i = 0; i < tempLen.length; i++) {
                if (tempLen[i] != 0X00) {
                    start = i;
                    break;
                }
            }
            byte[] lenData = BytesUtil.subBytes(tempLen, start, -1);
            lenData = BytesUtil.merge(new byte[]{(byte) (0x80 | lenData.length)}, lenData);
            return lenData;
        } else {
            return new byte[]{(byte) len};
        }
    }


    /**
     * Get the length of the tag length
     *
     * @param data
     * @param offset
     * @return
     */
    private static int getLLength(byte[] data, int offset) {
        if ((data[offset] & 0X80) == 0) {
            return 1;
        }
        return (data[offset] & 0X7F) + 1;
    }

    /**
     * Get the Tag Length
     *
     * @param data   data
     * @param offset offset
     * @return length
     */
    private static int getTLength(byte[] data, int offset) {
        //Determine the first byte of the tag
        if ((data[offset] & 0X1F) == 0X1F) {
            //Determine the remaining bytes of the tag
            return parseTLength(data, ++offset, 2);
        }
        return 1;
    }

    private static int parseTLength(byte[] data, int offset, int clen) {
        if ((data[offset] & 0x80) == 0x80) {
            return parseTLength(data, ++offset, ++clen);
        }
        return clen;
    }


    /**
     * Calculate the length of the value
     *
     * @param l      data
     * @param offset offset
     * @param lLen   the length of the tag length
     * @return the length of the value
     */
    private static int calcValueLength(byte[] l, int offset, int lLen) {
        if (lLen == 1) {
            return l[offset] & 0Xff;
        }
        int vLen = 0;
        for (int i = 1; i < lLen; i++) {
            vLen <<= 8;
            vLen |= (l[offset + i]) & 0Xff;
        }
        return vLen;
    }



    public static boolean paddingData(int flag, byte[] inData, int maxLen, byte pChar, byte[] outData) {
        int wLength;

        wLength = inData.length;
        if (wLength > maxLen) {
            return false;
        }

        if (flag == 0) {
            if (wLength <= maxLen) {
                System.arraycopy(inData, 0, outData, maxLen - wLength, wLength);
                for (int i = 0; i < (maxLen - wLength); i++) {
                    outData[i] = pChar;
                }
            }
        } else {
            if (wLength <= maxLen) {
                System.arraycopy(inData, 0, outData, 0, wLength);
                for (int i = wLength; i < (maxLen - wLength); i++) {
                    outData[i] = pChar;
                }
            }
        }
        return true;
    }

    public static int addTlvData(byte[] tlvBuf, byte[] pbTag, byte[] pbValue, int wLength, int[] pwOffset) {
        int wOffset = 0;
        byte bTagLen;

        wOffset = pwOffset[0];

        if (((pbTag[0]) & 0x1F) == 0x1F)//Tag?2???
        {
            if ((pbTag[1] & 0x80) == 0x80) {
                bTagLen = 3;
            } else {
                bTagLen = 2;
            }
        } else {
            bTagLen = 1;
        }
        System.arraycopy(pbTag, 0, tlvBuf, wOffset, bTagLen);

        wOffset += bTagLen;


        if (wLength >= 256) {
            tlvBuf[wOffset++] = (byte) 0x82;
            tlvBuf[wOffset++] = (byte) ((wLength & 0xFF00) >> 8);
            tlvBuf[wOffset++] = (byte) (wLength & 0x00FF);
        } else if (wLength >= 128) {
            tlvBuf[wOffset++] = (byte) 0x81;
            tlvBuf[wOffset++] = (byte) wLength;
        } else {
            tlvBuf[wOffset++] = (byte) wLength;
        }

        if (wLength > 0) {
            System.arraycopy(pbValue, 0, tlvBuf, wOffset, wLength);
            wOffset += wLength;
        }

        pwOffset[0] = wOffset;

        return 0;
    }
}
