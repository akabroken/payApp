package com.isw.payapp.devices.newtelpo.EMV;

import android.content.Context;

import com.telpo.emv.EmvParam;
import com.telpo.emv.EmvService;
import com.telpo.emv.EmvTLV;
import com.telpo.util.StringUtil;

import java.util.List;

public class UpiPBOC {

    private final Context context;
    private final EMVHandler emvHandler;

    private int _LastCode = 0;

    public UpiPBOC(Context context) {
        this.context = context;
        this.emvHandler = new EMVHandler(context);
    }

    public boolean StartTransaction(double amount)
    {
        EmvParam mEMVParam;
        mEMVParam = new EmvParam();

        _LastCode = emvHandler.emvService.qPboc_InitParam(mEMVParam);
        if(EmvService.EMV_TRUE != _LastCode) {
            emvHandler.appendDisplay("qPboc_InitParam fail, err code:"+_LastCode);
            return false;
        }
        else{
            emvHandler.appendDisplay("qPboc_InitParam succ!");
        }

        _LastCode = emvHandler.emvService.qPboc_Preprocess((int)(amount*100),2,156,EmvService.KERNEL_QPBOC);
        if(EmvService.EMV_TRUE != _LastCode){
            emvHandler.appendDisplay("qPboc_Preprocess fail,err code:"+ _LastCode);
            return false;
        }
        emvHandler.appendDisplay("qPboc_Preprocess succ");

        _LastCode = emvHandler.emvService.qPboc_StartApp(EmvService.KERNEL_QPBOC);

        List<EmvTLV> tagList=EMVUtilsConfigs.getTLVContactlessCardDataTags();
        int ret=0;
        for (EmvTLV emvTLV : tagList) {
            ret= emvHandler.emvService.Emv_GetTLV(emvTLV);
            // appendDisplay("TLV"+Integer.toHexString(emvTLV.Tag).toUpperCase()+ ":"+StringUtil.bytesToHexString(emvTLV.Value));
            if (EmvService.EMV_TRUE == ret){
                emvHandler.appendDisplay("Tag"+Integer.toHexString(emvTLV.Tag).toUpperCase()+ ":"+ StringUtil.bytesToHexString(emvTLV.Value));
            }else {
                emvHandler.appendDisplay("Tag" + Integer.toHexString(emvTLV.Tag).toUpperCase() + ":N/G" );
            }
            //showMessage(String.format("TLV%s : %s", Integer.toHexString(emvTLV.Tag).toUpperCase(), StringUtil.bytesToHexString(emvTLV.Value)));
            //   Log.e(TAG, String.format("Getting TLV: %s : %s Result %d", Integer.toHexString(emvTLV.Tag), StringUtils.bytesToHex(emvTLV.Value), ret));

        }
        if(EmvService.QPBOC_TC == _LastCode)
        {
            emvHandler.appendDisplay("Transaction Success");
        }
        else if(EmvService.QPBOC_ARQC == _LastCode)
        {
            EmvTLV tlv = new EmvTLV(0x5A);
            _LastCode = emvHandler.emvService.Emv_GetTLV(tlv);
            if(EmvService.EMV_TRUE == _LastCode){
                emvHandler.cardNum = StringUtil.bytesToHexString(tlv.Value).replace("F", "");
                emvHandler.appendDisplay("0x5A:"+ StringUtil.bytesToHexString(tlv.Value));
            }else{
                tlv = new EmvTLV(0x57);
                _LastCode = emvHandler.emvService.Emv_GetTLV(tlv);
                if(EmvService.EMV_TRUE == _LastCode) {
                    String str_57 = StringUtil.bytesToHexString(tlv.Value);
                    emvHandler.cardNum = str_57.substring(0, str_57.indexOf('D'));
                    emvHandler.appendDisplay("0x57:" + StringUtil.bytesToHexString(tlv.Value));
                }else {
                    emvHandler.appendDisplay("Get cardNum Fail");
                }
            }
            if(!(null == emvHandler.cardNum || emvHandler.cardNum.isEmpty())){
                emvHandler.appendDisplay("cardNum:" + emvHandler.cardNum);
                emvHandler.appendDisplay("encryptPan:" + emvHandler.encryptPan(emvHandler.cardNum));
            }

            if(EmvService.EMV_TRUE == emvHandler.emvService.qPboc_IsNeedPin())
            {
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
            emvHandler.appendDisplay("qPboc_StartApp Fail:" + _LastCode );
            return false;
        }
        emvHandler.changePanUIVisibility(false, null);
        //the online result
        if (emvHandler.pay()) {
            emvHandler.appendDisplay("Transaction Success");
            return true;
        } else {
            emvHandler.appendDisplay("Transaction Fail");
            return false;
        }


    }


}
