package com.isw.payapp.devices.telpo;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;


import com.isw.payapp.devices.interfaces.IPinPadProcessor;
import com.isw.payapp.utils.DUKPK2009_CBC;
import com.isw.payapp.utils.ThreeDES;
import com.telpo.emv.EmvService;
import com.telpo.pinpad.PinpadService;
import com.telpo.tps550.api.util.StringUtil;

public class TelpoPinpad implements IPinPadProcessor {

    private Context context;

    private EmvService emvService;

    private TelpoPinpad telpoPinpad;

    private int ret;

    public TelpoPinpad(Context context){
        this.context = context;
    }
    @Override
    public void initPinPad() {
        ret = EmvService.Open(context);
        if (ret != EmvService.EMV_TRUE) {
            Log.e("ISWKENYA", "EmvService.Open fail");
        }

        ret = EmvService.deviceOpen();
        if (ret != 0) {
            Log.e("ISWKENYA", "EmvService.Open fail");
        }

        ret = PinpadService.Open(context);//Returns 0 on success and otherwise on failure


        if (ret == PinpadService.PIN_ERROR_NEED_TO_FOMRAT) {
            PinpadService.TP_PinpadFormat(context);
            ret = PinpadService.Open(context);//Returns 0 on success and otherwise on failure
        }

        Log.d("ISWKENYA", "PinpadService deviceOpen open:" + ret);
        if (ret != 0) {
            Toast.makeText(context, "PinpadService open fail", Toast.LENGTH_SHORT).show();
        }

    }


    @Override
    public int injectDukptKey(String key, String iKsn, String kcv) {
        Log.i("BDK KEY",key);
        try {
            String bdk_key = "";
            if(key.length()>32){
                key = key.substring(8,40);
            }
            byte[] ipekBytes = DUKPK2009_CBC.GenerateIPEK(
                    ThreeDES.hexStringToByteArray(iKsn),
                    ThreeDES.hexStringToByteArray(key)
            );
            String ipek = ThreeDES.byteArrayToHexString(ipekBytes).toUpperCase();
            Log.i("IPEK: " , ipek+"===="+iKsn);

            ret = PinpadService.TP_PinpadCheckKey(PinpadService.KEY_TYPE_DUKPT, 0);
            Log.i("IPEK", "PinpadService.TP_PinpadCheckKey(PinpadService.KEY_TYPE_DUKPT, 0) Value is "+ ret);

            if (ret == -9||0==ret) {
                ret = PinpadService.TP_PinpadWriteDukptIPEK(StringUtil.toBytes(ipek), StringUtil.toBytes(iKsn), 0, PinpadService.KEY_WRITE_DIRECT, 0);
                Log.i("RKI", "PinpadService.TP_PinpadWriteDukptIPEK:"+ret);
            } else {
                ret = 100;
            }
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }

        //301C0410B30D16EAE5372C9457326464E62C5E610408103BAA944EBBD1FE

        return  ret;
    }

    @Override
    public int resetKey() {
      ret = PinpadService.TP_PinpadFormat(context);
      return ret;
    }

    @Override
    public void deleteKey() {

    }

    @Override
    public int deleteKeys() {

        ret = PinpadService.TP_PinpadDeleteKey(PinpadService.KEY_TYPE_DUKPT, 0);
        Log.i("DUKPTDEL", "DELETE IPEK :" + ret);
        ret = PinpadService.TP_PinpadDeleteKey(PinpadService.KEY_TYPE_NORMAL, 0);
        Log.i("DUKPTDEL", "DELETE MASTER KEY :" + ret);
        ret = PinpadService.TP_PinpadDeleteKey(PinpadService.KEY_TYPE_NORMAL, 1);
        Log.i("DUKPTDEL", "DELETE MASTER KEY :" + ret);
        ret = PinpadService.TP_PinpadDeleteKey(PinpadService.KEY_TYPE_DUKPT, 1);
        Log.i("DUKPTDEL", "DELETE MASTER KEY :" + ret);
        ret = PinpadService.TP_PinpadDeleteKey(PinpadService.KEY_TYPE_NORMAL, 2);
        Log.i("DUKPTDEL", "DELETE MASTER KEY :" + ret);
        ret = PinpadService.TP_PinpadDeleteKey(PinpadService.KEY_TYPE_DUKPT, 2);
        Log.i("DUKPTDEL", "DELETE MASTER KEY :" + ret);
        ret = PinpadService.TP_PinpadDeleteKey(PinpadService.KEY_TYPE_NORMAL, 3);
        Log.i("DUKPTDEL", "DELETE MASTER KEY :" + ret);
        ret = PinpadService.TP_PinpadDeleteKey(PinpadService.KEY_TYPE_DUKPT, 3);
        Log.i("DUKPTDEL", "DELETE MASTER KEY :" + ret);
        ret = PinpadService.TP_PinpadDeleteKey(PinpadService.KEY_TYPE_NORMAL, 3);
        Log.i("DUKPTDEL", "DELETE MASTER KEY :" + ret);
        ret = PinpadService.TP_PinpadDeleteKey(PinpadService.KEY_TYPE_DUKPT, 3);
        Log.i("DUKPTDEL", "DELETE MASTER KEY :" + ret);
        return ret;
    }

    @Override
    public void deviceClose() {
        PinpadService.Close();
        EmvService.deviceClose();
    }
}
