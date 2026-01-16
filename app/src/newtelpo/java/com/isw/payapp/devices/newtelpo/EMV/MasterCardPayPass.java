package com.isw.payapp.devices.newtelpo.EMV;

import android.content.Context;
import android.util.Log;

import com.telpo.emv.EmvParam;
import com.telpo.emv.EmvService;
import com.telpo.emv.EmvTLV;
import com.telpo.emv.PaypassErrorData;
import com.telpo.emv.PaypassListener;
import com.telpo.emv.PaypassOutCome;
import com.telpo.emv.PaypassParam;
import com.telpo.emv.PaypassResult;
import com.telpo.emv.PaypassUserData;
import com.telpo.util.StringUtil;

import java.util.List;

public class MasterCardPayPass {


    private final Context context;

    private final EMVHandler emvHandler;

    private int _LastCode = 0;


    public MasterCardPayPass(Context context) {
        this.context = context;
        this.emvHandler = new EMVHandler(context);
    }

    PaypassListener paypassListener =new PaypassListener() {
        @Override
        public int onPaypass_InitApp() {

            int ret = 0;
            EmvTLV tlv = new EmvTLV(0x9F06);
            ret = emvHandler.emvService.Emv_GetTLV(tlv);
            Log.e("yw_AID","AID:"+ StringUtil.bytesToHexString(tlv.Value));

            emvHandler.appendDisplay("AID:"+ StringUtil.bytesToHexString(tlv.Value));

            if (StringUtil.bytesToHexString(tlv.Value).equals("A0000000041010")){
                PaypassParam param = new PaypassParam();
                param.TacDefault= StringUtil.hexStringToByte("F45084800C");
                param.TacDenial= StringUtil.hexStringToByte("0000000000");
                param.TacOnline= StringUtil.hexStringToByte("F45084800C");

                _LastCode = emvHandler.emvService.Paypass_SetPaypassParam(param);
                if(EmvService.EMV_TRUE != _LastCode) {
                    emvHandler.appendDisplay("Paypass_SetPaypassParam fail, err code:"+_LastCode);
                }
                else{
                    emvHandler.appendDisplay("Paypass_SetPaypassParam succ!");
                }
            }else if (StringUtil.bytesToHexString(tlv.Value).equals("A0000000043060")){ //Maestro
                PaypassParam param = new PaypassParam();
                param.TacDefault= StringUtil.hexStringToByte("F45004800C");
                param.TacDenial= StringUtil.hexStringToByte("0000800000");
                param.TacOnline= StringUtil.hexStringToByte("F45004800C");
                _LastCode = emvHandler.emvService.Paypass_SetPaypassParam(param);
                if(EmvService.EMV_TRUE != _LastCode) {
                    emvHandler.appendDisplay("Paypass_SetPaypassParam fail, err code:"+_LastCode);
                }
                else{
                    emvHandler.appendDisplay("Paypass_SetPaypassParam succ!");
                }
            }


            return EmvService.EMV_TRUE;
        }

        @Override
        public int onPaypass_SendDEK(int i) {
            return EmvService.EMV_TRUE;
        }

        @Override
        public int onPaypass_WaitDET(int i) {
            return EmvService.EMV_TRUE;
        }

        @Override
        public int onPaypass_SendOut(int i) {
            return EmvService.EMV_TRUE;
        }

        @Override
        public int onPaypass_SendMsg(int i) {
            return EmvService.EMV_TRUE;
        }
    };

    public boolean StartTransaction(double amount)
    {
        //set the listener
        emvHandler.emvService.setListener(paypassListener);
        //set emv param
        EmvParam mEMVParam;
        mEMVParam = new EmvParam();
        _LastCode = emvHandler.emvService.Emv_SetParam(mEMVParam);
        if(EmvService.EMV_TRUE != _LastCode) {
            emvHandler.appendDisplay("Emv_SetParam fail,err code:"+_LastCode);
            return false;
        }
        else{ emvHandler.appendDisplay("Emv_SetParam succ"); }
        //Paypass_TransInit
        PaypassParam param = new PaypassParam();
        param.TermCountryCode=156;
        param.TacDefault= StringUtil.hexStringToByte("F45084800C");
        param.TacDenial= StringUtil.hexStringToByte("0000000000");
        param.TacOnline= StringUtil.hexStringToByte("F45084800C");
        //Paypass_SetPaypassParam
        _LastCode = emvHandler.emvService.Paypass_TransInit(param);
        if(EmvService.EMV_TRUE != _LastCode) {
            emvHandler.appendDisplay("Paypass_TransInit fail, err code:"+_LastCode);
            return false;
        }
        else{
            emvHandler.appendDisplay("Paypass_TransInit succ!");
        }
        //Paypass_StartApp
        _LastCode = emvHandler.emvService.Paypass_StartApp((int)amount*100, 0, 978, 2, 0);

        List<EmvTLV> tagList=EMVUtilsConfigs.getTLVContactlessCardDataTags();
        int ret=0;
        for (EmvTLV emvTLV : tagList) {
            ret = emvHandler.emvService.Emv_GetTLV(emvTLV);
            // appendDisplay("TLV"+Integer.toHexString(emvTLV.Tag).toUpperCase()+ ":"+StringUtil.bytesToHexString(emvTLV.Value));
            if (EmvService.EMV_TRUE == ret) {
                emvHandler.appendDisplay("Tag" + Integer.toHexString(emvTLV.Tag).toUpperCase() + ":" + StringUtil.bytesToHexString(emvTLV.Value));
            } else {
                emvHandler.appendDisplay("Tag" + Integer.toHexString(emvTLV.Tag).toUpperCase() + ":N/G");
            }
        }
        //get result
        PaypassOutCome OutCome = new PaypassOutCome();
        PaypassUserData UserData = new PaypassUserData();
        PaypassErrorData ErrorData = new PaypassErrorData();
        emvHandler.emvService.Paypass_GetResult(OutCome,UserData,ErrorData);

        if(PaypassResult.PAYPASS_STATUS_APPROVED == OutCome.Status) {
            //offline APPROVED
        }
        else if(PaypassResult.PAYPASS_STATUS_ONLINE == OutCome.Status)
        {
            if(OutCome.CVM == PaypassResult.PAYPASS_CVM_ONLINEPIN) {
                if(null == emvHandler.cardNum || emvHandler.cardNum.isEmpty()){
                    emvHandler.appendDisplay("Transaction Fail,No Card info!");
                    return false;
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
                    emvHandler.appendDisplay("Transaction Fail");
                    return false;
                }
            }
        }
        else{
            emvHandler.appendDisplay("Paypass_StartApp Fail:" + _LastCode);
            return false;
        }
        emvHandler.appendDisplay("Transaction Success");
        return true;
    }

}
