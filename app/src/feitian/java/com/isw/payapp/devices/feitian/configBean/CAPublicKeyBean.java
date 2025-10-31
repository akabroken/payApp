package com.isw.payapp.devices.feitian.configBean;

import android.text.TextUtils;

import com.ftpos.library.smartpos.emv.CAPublicKeyInfo;
import com.isw.payapp.utils.BytesUtil;
import com.isw.payapp.utils.tlvs.TLV;


import java.util.HashMap;

/**
 * @author GuoJirui.
 * @date 2021/4/26.
 * @desc
 */
public class CAPublicKeyBean extends XmlDataBean {
    private static final HashMap<String, String> TAG_LIST = new HashMap<>();
    private final CAPublicKeyInfo caPublicKey = new CAPublicKeyInfo();

    static {
        TAG_LIST.put("RID", "1F42");
        TAG_LIST.put("CAPKIndex", "9F22");
        TAG_LIST.put("CAPKModulus", "1F46");
        TAG_LIST.put("CAPKExponent", "1F45");
        TAG_LIST.put("CAPKChecksum", "1F44");
        TAG_LIST.put("CAPKExpirationDate", "1F47");
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
        if (name.equalsIgnoreCase("PUBKEY")) {
            tagBuf = BytesUtil.hexString2Bytes(TAG_LIST.get("RID"));
            dataBuf = BytesUtil.hexString2Bytes(val);
            valueLen = dataBuf.length;
            TLV.addTlvData(tlvBuff, tagBuf, dataBuf, valueLen, tlvLen);
            caPublicKey.setRid(dataBuf);

            tagBuf = BytesUtil.hexString2Bytes(TAG_LIST.get("CAPKIndex"));
            dataBuf = BytesUtil.hexString2Bytes(value[1]);
            valueLen = dataBuf.length;
            TLV.addTlvData(tlvBuff, tagBuf, dataBuf, valueLen, tlvLen);
            caPublicKey.setIndex(dataBuf[0]);

        } else {
            if (!TextUtils.isEmpty(tag)) {
                if (val.length() % 2 == 1) {
                    //TODO:Throw Exception
                } else {
                    dataBuf = BytesUtil.hexString2Bytes(val);
                    valueLen = dataBuf.length;
                }
                TLV.addTlvData(tlvBuff, tagBuf, dataBuf, valueLen, tlvLen);

                switch (name) {
                    case "CAPKModulus":
                        caPublicKey.setPubKey(dataBuf);
                        break;
                    case "CAPKExponent":
                        caPublicKey.setExponent(dataBuf);
                        break;
                    case "CAPKChecksum":
                        if (dataBuf.length == 0) {
                            caPublicKey.setDigestFlag((byte) 0x00);
                        } else {
                            caPublicKey.setDigestFlag((byte) 0x01);
                            caPublicKey.setDigest(dataBuf);
                        }
                        break;
                    case "CAPKExpirationDate":
                        caPublicKey.setExpDate(dataBuf);
                        break;
                    default:
                        break;
                }
            }

        }

    }

    public CAPublicKeyInfo getCAPublicKey() {
        return caPublicKey;
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
