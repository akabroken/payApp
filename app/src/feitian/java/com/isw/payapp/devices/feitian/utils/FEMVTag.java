package com.isw.payapp.devices.feitian.utils;

import android.text.TextUtils;

import java.util.HashMap;

public class FEMVTag {
    public static final String TAG_RFU = "RFU";
    public static HashMap<String, String> EMV_TAG_MAP = new HashMap<>();
    public static HashMap<String, String> EMV_TAG95_MAP = new HashMap<>();

    public static String toString(String tag) {
        String str = EMV_TAG_MAP.get(tag);
        if (TextUtils.isEmpty(str)) {
            return tag;
        } else {
            return str;
        }
    }

    public static String tag95ToString(String tag) {
        String str = EMV_TAG95_MAP.get(tag);
        if (TextUtils.isEmpty(str)) {
            return TAG_RFU;
        } else {
            return str;
        }
    }

    static {
        EMV_TAG_MAP.put("1F60", "Kernel ID");
        EMV_TAG_MAP.put("1F63", "PIN Result");

        EMV_TAG_MAP.put("9F01", "Acquirer Identifier");
        EMV_TAG_MAP.put("9F02", "Amount");
        EMV_TAG_MAP.put("9F03", "Additional Amounts");
        EMV_TAG_MAP.put("9F26", "Application Cryptogram");
        EMV_TAG_MAP.put("5F25", "Application Effective Date");
        EMV_TAG_MAP.put("5F24", "Application Expiration Date");
        EMV_TAG_MAP.put("82", "Application Interchange Profile");
        EMV_TAG_MAP.put("5A", "Application PAN");
        EMV_TAG_MAP.put("5F34", "Application PAN Sequence Number");
        EMV_TAG_MAP.put("9F36", "Application Transaction Counter");
        EMV_TAG_MAP.put("9F07", "Application Usage Control");
        EMV_TAG_MAP.put("89", "Authorisation Code");
        EMV_TAG_MAP.put("8A", "Authorisation Response Code");
        EMV_TAG_MAP.put("9F27", "Cryptogram Information Data");
        EMV_TAG_MAP.put("8E", "CVM List");
        EMV_TAG_MAP.put("9F34", "CVM Results");
        EMV_TAG_MAP.put("9F1E", "IFD Serial Number");
        EMV_TAG_MAP.put("9F0D", "Issuer Action Code - Default");
        EMV_TAG_MAP.put("9F0E", "Issuer Action Code - Denial");
        EMV_TAG_MAP.put("9F0F", "Issuer Action Code - Online");
        EMV_TAG_MAP.put("9F10", "Issuer Application Data");
        EMV_TAG_MAP.put("91", "Issuer Authentication Data");
        EMV_TAG_MAP.put("5F28", "Issuer Country Code");
        EMV_TAG_MAP.put("9F15", "Merchant Category Code");
        EMV_TAG_MAP.put("9F16", "Merchant Identifier");
        EMV_TAG_MAP.put("9F39", "POS Entry Mode");
        EMV_TAG_MAP.put("5F30", "Service Code");
        EMV_TAG_MAP.put("9F33", "Terminal Capabilities");
        EMV_TAG_MAP.put("9F40", "Additional Terminal Capabilities");
        EMV_TAG_MAP.put("9F1A", "Terminal Country Code");
        EMV_TAG_MAP.put("9F1C", "Terminal Identification");
        EMV_TAG_MAP.put("9F35", "Terminal Type");
        EMV_TAG_MAP.put("95", "TVR");
        EMV_TAG_MAP.put("57", "Track 2 Equivalent Data");
        EMV_TAG_MAP.put("5F2A", "Transaction Currency Code");
        EMV_TAG_MAP.put("9A", "Transaction Date");
        EMV_TAG_MAP.put("9F21", "Transaction Time");
        EMV_TAG_MAP.put("9C", "Transaction Type");
        EMV_TAG_MAP.put("9F37", "Unpredictable Number");
        EMV_TAG_MAP.put("9F66", "TTQ");


        EMV_TAG95_MAP.put("0180", "Offline data authentication was not performed");
        EMV_TAG95_MAP.put("0140", "SDA failed");
        EMV_TAG95_MAP.put("0120", "ICC data missing");
        EMV_TAG95_MAP.put("0110", "Card appears on terminal exception file");
        EMV_TAG95_MAP.put("0108", "DDA failed");
        EMV_TAG95_MAP.put("0104", "CDA failed");

        EMV_TAG95_MAP.put("0280", "ICC and terminal have different application versions");
        EMV_TAG95_MAP.put("0240", "Expired application");
        EMV_TAG95_MAP.put("0220", "Application not yet effective");
        EMV_TAG95_MAP.put("0210", "Requested service not allowed for card product");
        EMV_TAG95_MAP.put("0208", "New card");

        EMV_TAG95_MAP.put("0380", "Cardholder verification was not successful");
        EMV_TAG95_MAP.put("0340", "Unrecognised CVM");
        EMV_TAG95_MAP.put("0320", "PIN Try Limit exceeded");
        EMV_TAG95_MAP.put("0310", "PIN entry required and PIN pad not present or not working");
        EMV_TAG95_MAP.put("0308", "PIN entry required, PIN pad present, but PIN was not entered");
        EMV_TAG95_MAP.put("0304", "Online PIN entered");

        EMV_TAG95_MAP.put("0480", "Transaction exceeds floor limit");
        EMV_TAG95_MAP.put("0440", "Lower consecutive offline limit exceeded");
        EMV_TAG95_MAP.put("0420", "Upper consecutive offline limit exceeded");
        EMV_TAG95_MAP.put("0410", "Transaction selected randomly for online processing");
        EMV_TAG95_MAP.put("0408", "Merchant forced transaction online");

        EMV_TAG95_MAP.put("0580", "Default TDOL used");
        EMV_TAG95_MAP.put("0540", "Issuer authentication failed");
        EMV_TAG95_MAP.put("0520", "Script processing failed before final GENERATE AC");
        EMV_TAG95_MAP.put("0510", "Script processing failed after final GENERATE AC");
    }
}
