package com.isw.payapp.utils.tlvs;


import androidx.annotation.NonNull;


import com.isw.payapp.utils.BytesUtil;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * @author GuoJirui.
 * @date 2021/4/29.
 * @desc The TLV data element
 */
public class TLVElement {
    private byte[] data;    //raw data
    private String tag;     //tag
    private int length = -1;//length
    private byte[] value;   //value

    public TLVElement() {

    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

    /**
     * Get the original TLV data,
     *
     * @return byte array
     */
    public byte[] getRawData() {
        return data;
    }

    /**
     * Get the tag
     *
     * @return tag
     */
    public String getTag() {
        return tag;
    }

    /**
     * Get the length of the value
     *
     * @return the length of the value
     */
    public int getLength() {
        return length;
    }

    /**
     * Gets the original Value byte array
     *
     * @return byte array
     */
    public byte[] getBytesValue() {
        return value;
    }

    /**
     * Gets Value,the hex string
     *
     * @return hex string
     */
    public String getValue() {
        byte[] temp = getBytesValue();
        return BytesUtil.bytes2HexString(temp == null ? new byte[0] : temp);
    }

    /**
     * Gets Value,GBK encoded string
     *
     * @return GBK encoded string
     */
    public String getGBKValue() {
        try {
            return new String(getBytesValue(), "GBK");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Determine if the data is valid
     *
     * @return true, valid; or false
     */
    public boolean isValid() {
        return data != null;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof TLVElement)) {
            return false;
        }

        if (data == null || ((TLVElement) obj).data == null) {
            return false;
        }

        return Arrays.equals(data, ((TLVElement) obj).data);
    }

    @NonNull
    @Override
    public String toString() {
        if (data == null) {
            return super.toString();
        }
        return BytesUtil.bytes2HexString(data);
    }
}

