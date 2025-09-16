package com.isw.payapp.tasks;

import android.util.Log;

import com.isw.payapp.model.CardModel;
import com.isw.payapp.model.EmvModel;
import com.isw.payapp.model.TransactionData;
import com.isw.payapp.utils.StringUtils;
import com.telpo.emv.EmvService;
import com.telpo.emv.EmvTLV;
import com.telpo.tps550.api.util.StringUtil;

import java.util.HashMap;
import java.util.Map;

public class EmvTLVExtractor {

    private CardModel cardModel;

    private final EmvService emvService;
    private final TransactionData payData;

    public EmvTLVExtractor(EmvService emvService, TransactionData payData) {
        this.emvService = emvService;
        this.payData = payData;
    }

    public EmvModel extractEmvData() {
        EmvModel emvModel = new EmvModel();
        Map<Integer, Integer> tagMap = initializeTagMap();

        EmvTLV emvTLVTest = new EmvTLV(0x56);
        int retr = emvService.Emv_GetTLV(emvTLVTest);
        Log.i("TAG 0x56 Track 1 data", StringUtils.bytesToHexString(emvTLVTest.Value));
        emvTLVTest = new EmvTLV(0x5F20);
        retr = emvService.Emv_GetTLV(emvTLVTest);
        Log.i("TAG 0x9F1F Track 1 data", StringUtils.bytesToHexString(emvTLVTest.Value)
                + "-__ACTUAL DATA-" + StringUtils.hexToAscii(StringUtils.bytesToHexString(emvTLVTest.Value)));

        EmvTLV trw = new EmvTLV(0x56);
        retr = emvService.Emv_GetTLV(trw);
        if (retr == EmvService.EMV_TRUE) {
            String panstr = StringUtils.bytesToHexString(trw.Value);
            Log.w("RRRRT", "RRRRT: " + panstr);
//            int index = panstr.indexOf("D");
//            Log.w("pan", "index: " + index);

        } else {
            Log.w("RRRRT", "RRRRT: " + retr);
        }

        for (Map.Entry<Integer, Integer> entry : tagMap.entrySet()) {
            int tag = entry.getKey();
            int length = entry.getValue();
            EmvTLV emvTLV = new EmvTLV(tag);

            int ret = emvService.Emv_GetTLV(emvTLV);
            StringBuffer p = new StringBuffer(StringUtil.toHexString(emvTLV.Value));

//            if (p.charAt(p.length() - 1) == 'F') {
//                p.deleteCharAt(p.length() - 1);
//            }

            String value = p.toString();
//            if (value.length() > length) {
//                value = value.substring(0, length);
//            }

            switch (tag) {
                //PAN
                case 0x5A:
                    if (value.length() > 0) {
                        emvModel.setPan(value);
                    }
                    break;
//AID
                case 0x9F06:
                    emvModel.setApplicationIdentifier(value);
                    break;
//EXP DATE
                case 0x5F24:
                    Log.i("0x5F24", value);
                    if (!value.isEmpty()) {
                        emvModel.setExMonth(value.substring(2, 4));
                        emvModel.setExpYear(value.substring(0, 2));
                    }
                    break;
//TRACK 2 DATA
                case 0x57:
                    emvModel.setTrack2data(value);
                    Log.i("TRACK_2_DATA", value);
                    if (emvModel.getExMonth() == null && emvModel.getExpYear() == null) {
                        int expiryYearIndex = 1 + value.indexOf('D') + 2; // Position of the first digit of the year
                        int expiryMonthIndex = expiryYearIndex + 2;
                        emvModel.setExMonth(value.substring(expiryYearIndex, expiryYearIndex + 2));
                        emvModel.setExpYear(value.substring(expiryMonthIndex, expiryMonthIndex + 2));
                    }
                    if (emvModel.getPan() == null) {
                        int dIndex = value.indexOf('D');
                        emvModel.setPan(value.substring(0, dIndex));
                    }
                    break;
// SERVICE RISTRICTION CODE
                case 0x5F30:
                    emvModel.setServiceCode(value);
                    break;
//CARD SEQUENCE NUMBER
                case 0x5F34:
                    emvModel.setCarSeqNo(value);
                    break;
//Amount Authorized
                case 0x9F02:
                    emvModel.setAmountAuthorized(value);
                    break;
//AmountOther
                case 0x9F03:
                    emvModel.setAmountOther(value);
                    break;
//Application Interchange Profile
                case 0x82:
                    emvModel.setApplicationInterchangeProfile(value);
                    break;
//Application Transaction Counter
                case 0x9F36:
                    emvModel.setAtc(value);
                    break;
//Cryptogram
                case 0x9F26:
                    emvModel.setCryptogram(value);
                    break;
//CryptogramInformationData
                case 0x9F27:
                    emvModel.setCryptogramInformationData(value);
                    break;
//CVM Results
                case 0x9F34:
                    emvModel.setCvmResults(value);
                    break;
//Issuer Application Data
                case 0x9F10:
                    emvModel.setIssuerApplicationData(value);
                    break;
//Transaction Currency Code
                case 0x5F2A:
                    emvModel.setTransactionCurrencyCode(value);
                    break;
//Terminal Verification Result
                case 0x95:
                    emvModel.setTerminalVerificationResult(value);
                    break;
////TerminalCountryCode
                case 0x9F1A:
                    Log.i("TERMINALCOUNTRYCODE", "::::" + value);
                    emvModel.setTerminalCountryCode(value);
                    break;
////TerminalType -- TO BE REVIEWED
                case 0x9F35:
                    Log.i("EMVTLVEXTRACT", value);
                    emvModel.setTerminalType(value);
                    break;
////Terminal Capabilities
                case 0x9F33:
                    emvModel.setTerminalCapabilities(value);
                    break;
////Transaction Type
                case 0x9C:
                    emvModel.setTransactionType(value);
                    break;
////Unpredictable Number
                case 0x9F37:
                    Log.i("UPREDICTABLENUMBER", "::::" + value);
                    emvModel.setUnpredictableNumber(value);
                    break;
////Dedicated File Name
                case 0x84:
                    Log.i("DEDICATEDFILENAME", "::::" + value);
                    emvModel.setDedicatedFileName(value);
                    break;
////Transaction Date
                case 0x9A:
                    emvModel.setTransactionDate(value);
                    //Track 1 Data
                    break;
                case 0x5F20:
                    Log.i("TRACK 1", "::::->>>>>>> " + value);
                    payData.setCardName(value);
                    emvModel.setTrack1data("B" + emvModel.getPan() + "=" + StringUtils.hexToAscii(value) + "=" + emvModel.getExpYear() + emvModel.getExMonth() + emvModel.getServiceCode() + "0000059600000");
                    break;

//                    //0x9F1F
                case 0x9F1F:
//                    Log.i("TRACK 1 second option ", "::::" + value);
//                    if (!value.isEmpty()){
//                        emvModel.setTrack1data(value);
//                    }else {
//                        emvModel.setTrack1data("value");
//                    }
                    break;
                default:
                    break;
            }
        }
        return emvModel;
    }


    private Map<Integer, Integer> initializeTagMap() {
        Map<Integer, Integer> tagMap = new HashMap<>();
        // Define tags and their corresponding lengths
        tagMap.put(0x5A, 19); // PAN
        tagMap.put(0x9F06, 10); // Application Identifier
        tagMap.put(0x5F24, 10); // Expiry Date
        tagMap.put(0x57, 38);//TRACK 2 DATA
        tagMap.put(0x5F30, 3);// SERVICE RISTRICTION CODE
        tagMap.put(0x5F34, 3);//CARD SEQUENCE NUMBER
        tagMap.put(0x9F02, 12);//Amount Authorized
        tagMap.put(0x9F03, 12);//AmountOther
        tagMap.put(0x82, 4);//Application Interchange Profile
        tagMap.put(0x9F36, 4);//Application Transaction Counter
        tagMap.put(0x9F26, 16);//Cryptogram
        tagMap.put(0x9F27, 2);//CryptogramInformationData
        tagMap.put(0x9F34, 6);//CVM Results
        tagMap.put(0x9F10, 40);//Issuer Application Data
        tagMap.put(0x5F2A, 4);//Transaction Currency Code
        tagMap.put(0x95, 10);//Terminal Verification Result
        tagMap.put(0x9F1A, 2);//Terminal Type
        tagMap.put(0x9F33, 6);//Terminal Capabilities
        tagMap.put(0x9C, 6);//Transaction type
        tagMap.put(0x9F37, 3);//Unpredictable Number
        tagMap.put(0x84, 3);//Dedicated File Name
        tagMap.put(0x9A, 0);//Transaction Date
        tagMap.put(0x9F35, 2);
        tagMap.put(0x56, 76); //Track 1 data
        tagMap.put(0x5F20, 26);
//        tagMap.put(0x0000, 0;);
//        tagMap.put(0x0000, 0;);
//        tagMap.put(0x0000, 0;);
//        tagMap.put(0x0000, 0;);
//        tagMap.put(0x0000, 0;);
//        tagMap.put(0x0000, 0;);
//        tagMap.put(0x0000, 0;);
//        tagMap.put(0x0000, 0;);
        // Add more tags as needed...
        return tagMap;
    }
}
