package com.isw.payapp.devices.feitian.configBean;

import android.text.TextUtils;


import com.isw.payapp.utils.BytesUtil;
import com.isw.payapp.utils.tlvs.TLV;

import java.util.HashMap;

/**
 * @author GuoJirui.
 * @date 2021/4/26.
 * @desc
 */
public class ExceptionListBean extends XmlDataBean {
    private static final HashMap<String, String> TAG_LIST = new HashMap<>();

    static {
        TAG_LIST.put("PAN", "5A");
        TAG_LIST.put("PAN_SN", "5F34");
    }


    private byte[] tlvBuff = new byte[1024];
    private int[] tlvLen = new int[1];

    @Override
    public void setTagValue(String name, String... value) {
        byte[] dataBuf = new byte[256];
        byte[] tagBuf = new byte[3];
        int valueLen = 0;

        if (TextUtils.isEmpty(value[0])) return;
        String tag = TAG_LIST.get(name);
        String val = trimSpace(value[0]);
        if (name.equalsIgnoreCase("EXCEPTIONlIST")) {
            tagBuf = BytesUtil.hexString2Bytes(TAG_LIST.get("PAN"));
            dataBuf = BytesUtil.hexString2Bytes(val);
            valueLen = dataBuf.length;
            TLV.addTlvData(tlvBuff, tagBuf, dataBuf, valueLen, tlvLen);

        } else {
            if (!TextUtils.isEmpty(tag)) {
                if (val.length() % 2 == 1) {
                    //TODO:Throw Exception
                } else {
                    dataBuf = BytesUtil.hexString2Bytes(val);
                    valueLen = dataBuf.length;
                }
                tagBuf = BytesUtil.hexString2Bytes(tag);
                TLV.addTlvData(tlvBuff, tagBuf, dataBuf, valueLen, tlvLen);
            }
        }
    }


    @Override
    public byte[] getBytes() {
        return tlvBuff;
    }

    @Override
    public int getTlvLens() {
        return tlvLen[0];
    }
}
