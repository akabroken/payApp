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
public class EMVCLAppParamsBean extends XmlDataBean {
    private static final HashMap<String, String> TAG_LIST = new HashMap<>();

    static {
        TAG_LIST.put("AID", "9F06");
        TAG_LIST.put("KernelID", "1F60");
        TAG_LIST.put("TransType", "9C");
        TAG_LIST.put("TransTypeGroup", "1F62");
        TAG_LIST.put("ASI", "1F14");
        TAG_LIST.put("Removaltimeout", "1F27");
        TAG_LIST.put("StatusCheck", "1F32");
        TAG_LIST.put("ZeroAmountAllowed", "1F33");
        TAG_LIST.put("OnlineOptionOnZeroAmount", "1F34");
        TAG_LIST.put("AdditionalTagObjectList", "1F4C");
        TAG_LIST.put("MandatoryTagObjectList", "1F49");
        TAG_LIST.put("ContactlessAppKernelCapabilities", "1F48");
        TAG_LIST.put("TransactionTypevalueAAT", "1F4B");
        TAG_LIST.put("AcquirerID", "9F01");
        TAG_LIST.put("AppVersionNumber", "9F09");
        TAG_LIST.put("MerchantCategoryCode", "9F15");
        TAG_LIST.put("MerchantID", "9F16");
        TAG_LIST.put("TerminalCountryCode", "9F1A");
        TAG_LIST.put("TerminalID", "9F1C");
        TAG_LIST.put("IFDSerial", "9F1E");
        TAG_LIST.put("TerminalCapabilities", "9F33");
        TAG_LIST.put("TerminalType", "9F35");
        TAG_LIST.put("AdditionalTerminalCapabilities", "9F40");
        TAG_LIST.put("MerchantNameLocation", "9F4E");
        TAG_LIST.put("TerminalTransactionQualifier", "9F66");
        TAG_LIST.put("MagStripeApplicationVersionNumber", "9F6D");
        TAG_LIST.put("ContactlessReaderCapabilities", "9F6D");
        TAG_LIST.put("EnhancedContactlessReaderCapabilities", "9F6E");
        TAG_LIST.put("TransCurrencyCode", "5F2A");
        TAG_LIST.put("TransCurrencyExponent", "5F36");
        TAG_LIST.put("TerminalActionCodeDefault", "1F04");
        TAG_LIST.put("TerminalActionCodeDenial", "1F05");
        TAG_LIST.put("TerminalActionCodeOnline", "1F06");
        TAG_LIST.put("MaxTargetPercentBiasedRandSelection", "1F07");
        TAG_LIST.put("TargetPercentBiasedRandSelection", "1F08");
        TAG_LIST.put("ThresholdValueBiasedRandSelection", "1F09");
        TAG_LIST.put("ExtendedSelectionSupportFlag", "1F38");
        TAG_LIST.put("CombinationOptions", "1F39");
        TAG_LIST.put("StaticTerminalInterchangeProfile", "1F3A");
        TAG_LIST.put("CardDataInputCapability", "DF8117");
        TAG_LIST.put("CVMCapability_Required", "DF8118");
        TAG_LIST.put("CVMCapability_NotRequired", "DF8119");
        TAG_LIST.put("DefaultUDOL", "DF811A");
        TAG_LIST.put("KernelConfiguration", "DF811B");
        TAG_LIST.put("MaxLifetimeofTornTransactionLogRecord", "DF811C");
        TAG_LIST.put("MaxNumberofTornTransactionLogRecords", "DF811D");
        TAG_LIST.put("MagStripeCVMCapabilityCVMRequired", "DF811E");
        TAG_LIST.put("SecurityCapability", "DF811F");
        TAG_LIST.put("MagStripeCVMCapabilityNoCVMRequired", "DF812C");
        TAG_LIST.put("TerminalFloorLimit", "9F1B");
        TAG_LIST.put("ReaderContactlessFloorLimit", "DF8123");
        TAG_LIST.put("CVMRequiredLimit", "DF8126");
        TAG_LIST.put("NoOndeviceCVMTransactionLimit", "DF8124");
        TAG_LIST.put("OndeviceCVMTransactionLimit", "DF8125");
        TAG_LIST.put("AccountType", "5F57");
        TAG_LIST.put("ContactlessPOSImplementationOptions", "1F4A");

        TAG_LIST.put("FieldOffHoldTime", "DF8130");
        TAG_LIST.put("CHVCSMessageTable", "DF8131");
        TAG_LIST.put("MinTimeRRTolerance", "DF8132");
        TAG_LIST.put("MaxTimeRRTolerance", "DF8133");
        TAG_LIST.put("TermTransTimeForRRCommand", "DF8134");
        TAG_LIST.put("TermTransTimeForRRResponse", "DF8135");
        TAG_LIST.put("RRMinTimeDiffLimit", "DF8136");
        TAG_LIST.put("RRTransTimeMismatchLimit", "DF8137");

        TAG_LIST.put("InteracRetryLimit", "1F49");
        TAG_LIST.put("MerchantTypeIndicator", "9F58");
        TAG_LIST.put("TerminalTransactionInformation", "9F59");
        TAG_LIST.put("TerminalCLReceiptRequiredLimit", "9F5A");
        TAG_LIST.put("TerminalOptionStatus", "9F5E");
        TAG_LIST.put("ContactlessReaderFloorLimit", "9F5F");
        TAG_LIST.put("ExtraTags", null);


    }


    private byte[] tlvBuff = new byte[1024];
    private int[] tlvLen = new int[1];
    private int paypassFlag = 0;

    @Override
    public void setTagValue(String name, String... value) {
        byte[] dataBuf = new byte[256];
        byte[] tagBuf = new byte[3];
        int valueLen = 0;

        if (TextUtils.isEmpty(value[0])) return;
        String tag = TAG_LIST.get(name);
        String val = trimSpace(value[0]);
        if (name.equalsIgnoreCase("Parameters")) {
            tagBuf = BytesUtil.hexString2Bytes(TAG_LIST.get("AID"));
            dataBuf = BytesUtil.hexString2Bytes(val);
            valueLen = dataBuf.length;
            TLV.addTlvData(tlvBuff, tagBuf, dataBuf, valueLen, tlvLen);

            value[1] = trimSpace(value[1]);
            tagBuf = BytesUtil.hexString2Bytes(TAG_LIST.get("KernelID"));
            dataBuf = BytesUtil.hexString2Bytes(value[1]);
            valueLen = dataBuf.length;
            TLV.addTlvData(tlvBuff, tagBuf, dataBuf, valueLen, tlvLen);

            if (value[1].equalsIgnoreCase("02")) {
                paypassFlag = 1;
            } else {
                paypassFlag = 0;
            }
            if(value.length>=3){
                value[2] = trimSpace(value[2]);
                tagBuf = BytesUtil.hexString2Bytes(TAG_LIST.get("TransTypeGroup"));
                dataBuf = BytesUtil.hexString2Bytes(parseTypes(value[2],","));
                TLV.addTlvData(tlvBuff, tagBuf, dataBuf, 2, tlvLen);
            }

        } else {
            if (name.equalsIgnoreCase("ExtraTags")) {
                dataBuf = BytesUtil.hexString2Bytes(val);
                System.arraycopy(dataBuf, 0, tlvBuff, tlvLen[0], dataBuf.length);
                tlvLen[0] += dataBuf.length;
            } else if (!TextUtils.isEmpty(tag)) {
                tagBuf = BytesUtil.hexString2Bytes(tag);
                if ((tag.equals("9F16")) || (tag.equals("9F1C")) || (tag.equals("9F1E")) || (tag.equals("9F4E"))) {
                    if ((tag.equals("9F1C")) || (tag.equals("9F1E"))) {
                        if (val.length() > 8) {
                            //TODO:Throw Exception
                        } else {
                            TLV.paddingData(1, val.getBytes(), 8, (byte) 0x00, dataBuf);
                            valueLen = 8;
                        }
                    }
                    if (tag.equals("9F16")) {
                        if (val.length()> 15) {
                            //TODO:Throw Exception
                        } else {
                            TLV.paddingData(1, val.getBytes(), 15, (byte) 0x00, dataBuf);
                            valueLen = 15;
                        }
                    }
                    if (tag.equals("9F4E")) {
                        dataBuf = val.getBytes();
                        valueLen = dataBuf.length;
                    }
                } else {
                    if (val.length() % 2 == 1) {
                        //TODO:Throw Exception
                    } else {
                        dataBuf = BytesUtil.hexString2Bytes(val);
                        valueLen = dataBuf.length;
                    }

                    if (tag.equals("1F04") && paypassFlag == 1) {
                        tagBuf = BytesUtil.hexString2Bytes("DF8120");
                    } else if (tag.equals("1F05") && paypassFlag == 1) {
                        tagBuf = BytesUtil.hexString2Bytes("DF8121");
                    } else if (tag.equals("1F06") && paypassFlag == 1) {
                        tagBuf = BytesUtil.hexString2Bytes("DF8122");
                    } else {
                        tagBuf = BytesUtil.hexString2Bytes(tag);
                    }
                }
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

    public  String parseTypes(String value, String separate){
        String[] types = value.split(separate);
        Integer val = 0;
        for(String type : types){
            if("00".equals(type)){
                val |= 0x01;
            }else if("01".equals(type)){
                val |= 0x02;
            }else if("09".equals(type)){
                val |= 0x04;
            }else if("20".equals(type)){
                val |= 0x08;
            }
        }
        return "000" + Integer.toHexString(val);
    }
}
