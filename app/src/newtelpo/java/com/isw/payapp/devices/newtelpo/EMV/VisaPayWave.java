package com.isw.payapp.devices.newtelpo.EMV;

import android.content.Context;

import com.telpo.emv.EmvAmountData;
import com.telpo.emv.EmvCandidateApp;
import com.telpo.emv.EmvOnlineData;
import com.telpo.emv.EmvParam;
import com.telpo.emv.EmvPinData;
import com.telpo.emv.EmvService;
import com.telpo.emv.EmvServiceListener;
import com.telpo.emv.EmvTLV;
import com.telpo.emv.QvsdcParam;
import com.telpo.util.StringUtil;

import java.util.List;

public class VisaPayWave {

    private final Context context;

    private final EMVHandler emvHandler;

    private int _LastCode = 0;

    public VisaPayWave(Context context) {
        this.context = context;
        this.emvHandler = new EMVHandler(context);
    }

    EmvServiceListener listener = new EmvServiceListener() {
        @Override
        public int onInputAmount(EmvAmountData emvAmountData) {
            return EmvService.EMV_TRUE;
        }

        @Override
        public int onInputPin(EmvPinData emvPinData) {
            return EmvService.EMV_TRUE;
        }

        @Override
        public int onSelectApp(EmvCandidateApp[] emvCandidateApps) {
            return emvCandidateApps[0].index;
        }

        @Override
        public int onSelectAppFail(int i) {
            return EmvService.EMV_TRUE;
        }

        @Override
        public int onFinishReadAppData() {
            int ret = 0;
            EmvTLV tlv = new EmvTLV(0x5A);
            ret = emvHandler.emvService.Emv_GetTLV(tlv);
            if(EmvService.EMV_TRUE == ret){
                emvHandler.cardNum = StringUtil.bytesToHexString(tlv.Value).replace("F", "");
                emvHandler.appendDisplay("0x5A:"+ StringUtil.bytesToHexString(tlv.Value));
            }else{
                tlv = new EmvTLV(0x57);
                ret = emvHandler.emvService.Emv_GetTLV(tlv);
                if(EmvService.EMV_TRUE == ret) {
                    String str_57 = StringUtil.bytesToHexString(tlv.Value);
                    emvHandler.cardNum = str_57.substring(0, str_57.indexOf('D'));
                    emvHandler.appendDisplay("0x57:" + StringUtil.bytesToHexString(tlv.Value));
                }else {
                    emvHandler.appendDisplay("Get cardNum Fail");
                }
            }
            if(!(null == emvHandler.cardNum || emvHandler.cardNum.isEmpty())){
                emvHandler.appendDisplay("cardNum:"+ emvHandler.cardNum);
                emvHandler.appendDisplay("encryptPan:" + emvHandler.encryptPan(emvHandler.cardNum));
            }
            return EmvService.EMV_TRUE;
        }

        @Override
        public int onVerifyCert() {
            return EmvService.EMV_TRUE;
        }

        @Override
        public int onOnlineProcess(EmvOnlineData emvOnlineData) {
            if(null == emvOnlineData) {
                return EmvService.ONLINE_FAILED;
            }
            emvHandler.changeDialogText("Online processing...");
            //is need to get pin
            if (emvHandler.emvService.qVsdc_IsNeedPin() == EmvService.EMV_TRUE) {
                if(null == emvHandler.cardNum || emvHandler.cardNum.isEmpty()){
                    emvHandler.appendDisplay("Transaction Fail,No Card info!");
                    return EmvService.ONLINE_FAILED;
                }
                String hidePan = emvHandler.cardNum.substring(0, 6) + "******" + emvHandler.cardNum.substring(emvHandler.cardNum.length() - 4,emvHandler.cardNum.length());
                emvHandler.changePanUIVisibility(true, "PAN:" + hidePan);
                //online PIN
                if (emvHandler.isMkMode) {
                    //MK/SK mode
                    emvHandler.pinBlock = emvHandler.getMkPin(emvHandler.cardNum);
                } else {
                    //Dukpt mode
                    emvHandler.pinBlock = emvHandler.getDukptPin(emvHandler.cardNum);
                }
                if("" == emvHandler.pinBlock) {
                    emvHandler.changePanUIVisibility(false, null);
                    return EmvService.ONLINE_FAILED;
                }
            }
            emvHandler.changePanUIVisibility(false, null);
            //the online result
            if (emvHandler.pay()) {
                emvOnlineData.ResponeCode = "00".getBytes();
                return EmvService.ONLINE_APPROVE;
            } else {
                return EmvService.ONLINE_FAILED;
            }
        }

        @Override
        public int onRequireTagValue(int i, int i1, byte[] bytes) {
            return EmvService.EMV_TRUE;
        }

        @Override
        public int onRequireDatetime(byte[] bytes) {
            return EmvService.EMV_TRUE;
        }

        @Override
        public int onReferProc() {
            return EmvService.EMV_TRUE;
        }

        @Override
        public int OnCheckException(String s) {
            return EmvService.EMV_FALSE;
        }

        @Override
        public int OnCheckException_qvsdc(int i, String s) {
            return EmvService.EMV_FALSE;
        }
    };

    public boolean StartTransaction(double amount)
    {
        //set the listener
        emvHandler.emvService.setListener(listener);


        //qVsdc_TransInit
        QvsdcParam param = new QvsdcParam();
        param.AMOUNT_Amount = (long) (amount*100);
        _LastCode = emvHandler.emvService.qVsdc_TransInit(param);
        if(EmvService.EMV_TRUE != _LastCode) {
            emvHandler.appendDisplay("qVsdc_TransInit fail, err code:"+_LastCode);
            return false;
        }
        else{
            emvHandler.appendDisplay("qVsdc_TransInit succ!");
        }

        //set emv param
        EmvParam mEMVParam;
        mEMVParam = new EmvParam();
        _LastCode = emvHandler.emvService.Emv_SetParam(mEMVParam);
        if(EmvService.EMV_TRUE != _LastCode) {
            emvHandler.appendDisplay("Emv_SetParam fail,err code:"+_LastCode);
            return false;
        }
        else{ emvHandler.appendDisplay("Emv_SetParam succ"); }

        //qVsdc_Preprocess
        _LastCode = emvHandler.emvService.qVsdc_Preprocess();
        if(EmvService.EMV_TRUE != _LastCode){
            emvHandler.appendDisplay("qVsdc_Preprocess fail,err code:"+ _LastCode);
            return false;
        }
        emvHandler.appendDisplay("qVsdc_Preprocess succ");

        //qVsdc_StartApp
        _LastCode = emvHandler.emvService.qVsdc_StartApp();
        List<EmvTLV> tagList =EMVUtilsConfigs.getTLVCardDataTags();
        for (EmvTLV emvTLV : tagList) {
            int ret = emvHandler.emvService.Emv_GetTLV(emvTLV);
            // appendDisplay("TLV"+Integer.toHexString(emvTLV.Tag).toUpperCase()+ ":"+StringUtil.bytesToHexString(emvTLV.Value));
            if (EmvService.EMV_TRUE == ret) {
                emvHandler.appendDisplay("Tag" + Integer.toHexString(emvTLV.Tag).toUpperCase() + ":" + StringUtil.bytesToHexString(emvTLV.Value));
            } else {
                emvHandler.appendDisplay("Tag" + Integer.toHexString(emvTLV.Tag).toUpperCase() + ":N/G");
            }
        }
        if((EmvService.QVSDC_OFFLINE_APPROVE == _LastCode) || (EmvService.QVSDC_ONLINE_APPROVE == _LastCode))
        {
            emvHandler.appendDisplay("Transaction Success");
        }else{
            emvHandler.appendDisplay("qVsdc_StartApp Fail:" + _LastCode );
            return false;
        }
        return true;
    }
}
