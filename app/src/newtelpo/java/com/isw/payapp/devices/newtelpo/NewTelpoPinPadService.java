package com.isw.payapp.devices.newtelpo;

import android.content.Context;
import android.util.Log;

import com.common.sdk.emv.PinpadEnum;
import com.common.sdk.emv.PinpadService;
import com.isw.payapp.devices.interfaces.IPinPadProcessor;
import com.isw.payapp.utils.DUKPK2009_CBC;
import com.isw.payapp.utils.ThreeDES;


public class NewTelpoPinPadService implements IPinPadProcessor {

    private final Context context;
    private static String TAG = "NewTelpoPinPadService";

    private PinpadService pinpadService;

    public NewTelpoPinPadService(Context context) {
        this.context = context;
    }


    @Override
    public void initPinPad() {
        pinpadService = new PinpadService(context);
        int iret = 0;
        iret = pinpadService.Pinpad_Open(context);
        Log.i(TAG, "Pinpad_Open: " + iret);
        if (iret == 0) {
            Log.d(TAG, "PINPAD_OPEN SUCCESS");
        }
        else {
            Log.d(TAG, "PINPAD_OPEN FAILED :"+iret);
           // return iret;
        }
    }

    @Override
    public int injectDukptKey(String key, String iKsn, String kcv) {

        int iRet = 0;

        Log.d(TAG,"injectDukptKey :"+"\n"+key+"\n"+iKsn+"\n"+kcv);

        String bdk = key.substring(8,40);
        Log.d(TAG, "BDK : "+ bdk);

        byte[] bdk_bytes = ThreeDES.hexStringToByteArray(bdk);

        byte[]ksn_bytes = ThreeDES.hexStringToByteArray(iKsn);


        byte[] ipekBytes = DUKPK2009_CBC.GenerateIPEK(
                ThreeDES.hexStringToByteArray(iKsn),
                ThreeDES.hexStringToByteArray(key)
        );
        String ipek = ThreeDES.byteArrayToHexString(ipekBytes).toUpperCase();
        Log.i("IPEK: " , ipek+"===="+iKsn);

        iRet = pinpadService.Pinpad_Check_Key(PinpadEnum.ENUM_KEY_TYPE.KEY_TYPE_DEA_DUKPT_KEY,
                0);
        Log.d(TAG, "Pinpad_Check_Key: " + iRet);

        if(iRet !=0){
            iRet = pinpadService.Pinpad_Write_DEA_DUKPT_IPEK(0,ipekBytes,ksn_bytes);
            Log.d(TAG, "Pinpad_Write_DEA_DUKPT_IPEK: " + iRet);
            pinpadService.Pinpad_Close();
        }

       // int ret = pinpadService.Pinpad_Write_DEA_DUKPT_BDK();
        return 0;
    }

    @Override
    public int resetKey() {
        int ret = pinpadService.Pinpad_Format();
        if (ret == 0) {
            Log.d(TAG, "PINPAD_FORMAT SUCCESS");
        }
        else {
            Log.d(TAG, "PINPAD_FORMAT FAILED :"+ret);
        }

        return 0;
    }

    @Override
    public void deleteKey() {
        int ret = pinpadService.Pinpad_Format();
        if (ret == 0) {
            Log.d(TAG, "PINPAD_FORMAT SUCCESS");
        }
        else {
            Log.d(TAG, "PINPAD_FORMAT FAILED :"+ret);
        }

    }

    @Override
    public int deleteKeys() {
        int ret = pinpadService.Pinpad_Format();
        if (ret == 0) {
            Log.d(TAG, "PINPAD_FORMAT SUCCESS");
        }
        else {
            Log.d(TAG, "PINPAD_FORMAT FAILED :"+ret);
        }

        return 0;
    }

    @Override
    public void deviceClose() {
        int ret = pinpadService.Pinpad_Close();
        if (ret == 0) {
            Log.d(TAG, "PINPAD_CLOSE SUCCESS");
        }
        else {
            Log.d(TAG, "PINPAD_CLOSE FAILED :"+ret);
        }
    }
}
