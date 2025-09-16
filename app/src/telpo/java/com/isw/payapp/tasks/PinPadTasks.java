package com.isw.payapp.tasks;

import android.content.Context;
import android.util.Log;

import com.isw.payapp.dialog.MyProgressDialog;
import com.isw.payapp.model.CardModel;
import com.telpo.emv.EmvPinData;
import com.telpo.emv.EmvService;
import com.telpo.emv.EmvTLV;
import com.telpo.pinpad.PinParam;
import com.telpo.pinpad.PinTextInfo;
import com.telpo.pinpad.PinpadService;
import com.telpo.tps550.api.util.StringUtil;

public class PinPadTasks {
    private PinParam param;
    EmvService emvService;
    private String amount;
    private Context context;
    private EmvPinData PinData;
    private MyProgressDialog progressDialog;
    boolean bUIThreadisRunning = true;
    private int event;
    String[] data = new String[3];

    public PinPadTasks(Context context, EmvPinData PinData, String amount, int event) {
        this.context = context;
        this.amount = amount;
        this.PinData = PinData;
        this.event = event;
    //    this.progressDialog = progressDialog;
    }

    public CardModel extractCardData() {
        bUIThreadisRunning = true;
        emvService = EmvService.getInstance();
        CardModel cardModel = new CardModel();

        PinParam pinParam = new PinParam(context);
        int ret, mResult;
        if(event == 1){
            EmvTLV pan = new EmvTLV(0x5A);
            ret = emvService.Emv_GetTLV(pan);
            Log.e("TESTSPIN", "result" + ret);
            Log.i("TESTSPIN", "PINDATA RESULT "+PinData.type);

            if (ret == EmvService.EMV_TRUE) {
                StringBuffer p = new StringBuffer(StringUtil.toHexString(pan.Value));
                if (p.charAt(p.toString().length() - 1) == 'F') {
                    p.deleteCharAt(p.toString().length() - 1);
                }
                pinParam.CardNo = p.toString();
                cardModel.setPan(p.toString());
            }
        }else{
            data[0] = EmvService.MagStripeReadStripeData(1);
            data[1] = EmvService.MagStripeReadStripeData(2);
            data[2] = EmvService.MagStripeReadStripeData(3);
            PinParam param = new PinParam(context);
            String data2 = data[1].toUpperCase();
            int i = data2.indexOf('=');
            if (i < 1 || i > 21) {
                i = data2.indexOf('D');
            }
            pinParam.CardNo = data2.subSequence(0, i).toString();
        }

        pinParam.PinBlockFormat = 0;
        pinParam.KeyIndex = 0;
        pinParam.WaitSec = 60;
        pinParam.MaxPinLen = 6;
        pinParam.MinPinLen = 4;
        pinParam.IsShowCardNo = 1;
        pinParam.Amount = amount;

//        PinpadService.Open(context);
//        progressDialog.dismiss();

        if (PinData.type == 0) {
            Log.d("PINPAD", "START DUKPT PIN:");
            Boolean isIsw = true;
            if (!isIsw) {
                ret = PinpadService.TP_PinpadGetPin(pinParam);
                Log.i("PINPAD", "TEST: " + ret);

            } else {
                PinTextInfo pinTextInfo = new PinTextInfo ();
                pinTextInfo.sText ="PLEASE INSERT PIN";
               // pinTextInfo.FontColor = androidx.fragment.R.;
                pinTextInfo.FontSize = 20;
                pinTextInfo.LanguageID ="en";
                pinTextInfo.PosX =40;
                pinTextInfo.PosY = 40;

               PinTextInfo[] test = new PinTextInfo[0];
//
//                test[0] = pinTextInfo;

                ret = PinpadService.TP_PinpadDukptSessionStart(0);
                if (0 != ret) {
                    //  mResult = EmvService.EMV_FALSE;
                }

                Log.e("PINPAD ", "TP_PinpadDukptSessionStart : " + ret);
                ret = PinpadService.TP_PinpadDukptGetPin(pinParam);
               // ret = PinpadService.TP_PinpadDukptGetPinCustomize(pinParam,test,1,1,60);
                if (ret != 0) {
                    Log.e("PINPADERR", "PIN INPUT ERROR : " + ret);
                    // mResult = EmvService.ERR_NOPIN;
                    cardModel = null;
                }
                if(!StringUtil.toHexString(pinParam.Pin_Block).isEmpty()&&StringUtil.toHexString(pinParam.Pin_Block).length()>0){
                    cardModel.setPinBlock(StringUtil.toHexString(pinParam.Pin_Block));
                    cardModel.setKsn(StringUtil.toHexString(pinParam.Curr_KSN).substring(4));
                    cardModel.setKSNTag(StringUtil.toHexString(pinParam.Curr_KSN).substring(4, 10));
                    cardModel.setKsnd("605");
                    cardModel.setPinType("Dukpt");
                    Log.i("PINPAD ", "RES : " + cardModel.getPinBlock()+"\n"+cardModel.getKsn());
                }else {
                    cardModel = null;
                }
                PinpadService.TP_PinpadDukptSessionEnd();
            }

        } else {
            Log.i("PINPAD", "PinpadService.TP_PinpadGetPlainPin(param, 0, 0, 0)");
            ret = PinpadService.TP_PinpadGetPlainPin(pinParam, 0, 0, 0);  //plain text pinpad
            if (ret == PinpadService.PIN_OK) {
            }
        }
        if (ret == PinpadService.PIN_ERROR_CANCEL) {
            mResult = EmvService.ERR_USERCANCEL;
            Log.e("PINPADERR", "PIN_ERROR_CANCEL");
            pinParam = null;
        } else if (ret == PinpadService.PIN_OK) {
            mResult = EmvService.EMV_TRUE;

        } else {
            mResult = EmvService.EMV_FALSE;
            cardModel = null;
        }
        return cardModel;
    }
}
