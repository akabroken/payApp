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
public class EMVCLDRLBean extends XmlDataBean {
    private static final HashMap<String, String> TAG_LIST = new HashMap<>();

    static {
        TAG_LIST.put("ProgramId", "1F61");
        TAG_LIST.put("KernelID", "1F60");
        TAG_LIST.put("StatusCheck", "1F32");
        TAG_LIST.put("ZeroAmountAllowed", "1F33");
        TAG_LIST.put("OnlineOptionOnZeroAmount", "1F34");
        TAG_LIST.put("TerminalFloorLimit", "9F1B");
        TAG_LIST.put("ReaderContactlessFloorLimit", "DF8123");
        TAG_LIST.put("CVMRequiredLimit", "DF8126");
        TAG_LIST.put("NoOndeviceCVMTransactionLimit", "DF8124");
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
        if (name.equalsIgnoreCase("DRL")) {
            tagBuf = BytesUtil.hexString2Bytes(TAG_LIST.get("ProgramId"));
            dataBuf = BytesUtil.hexString2Bytes(val);
            valueLen = dataBuf.length;
            TLV.addTlvData(tlvBuff, tagBuf, dataBuf, valueLen, tlvLen);

            value[1] = trimSpace(value[1]);
            tagBuf = BytesUtil.hexString2Bytes(TAG_LIST.get("KernelID"));
            dataBuf = BytesUtil.hexString2Bytes(value[1]);
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
