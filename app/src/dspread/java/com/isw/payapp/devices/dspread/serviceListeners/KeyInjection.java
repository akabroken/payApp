package com.isw.payapp.devices.dspread.serviceListeners;

import static com.isw.payapp.devices.dspread.utils.Utils.getKeyIndex;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.dspread.xpos.QPOSService;
import com.isw.payapp.devices.dspread.utils.DUKPK2009_CBC;
import com.isw.payapp.devices.dspread.utils.Session;
import com.isw.payapp.devices.dspread.utils.Utils;

public class KeyInjection {
    QPOSService pos;
    private Context context_;
    private boolean bRet = false;

    private int masterKeyID = 97;
    private int workKeyId = 1;

    private String TEK = "";

    private Handler handler;


    public KeyInjection(Context context, QPOSService pos){
        this.context_ = context;
        this.pos = pos;
    }




    private void updateMasterKey(QPOSService pos,String tmkFromApi,String kvc)
    {
        try
        {
            Log.d("Master key start-->","");
            Log.d("updateMK::QPOSSvc",pos+"");


            String previousKey = "0123456789ABCDEFFEDCBA9876543210";

            byte[] encDemoNewMasterKey = DUKPK2009_CBC.TriDesEncryption(Utils.hexStringToByteArray(previousKey),
                    Utils.hexStringToByteArray(tmkFromApi));

            assert encDemoNewMasterKey != null;
            String newTMK = Utils.bytes2Hex(encDemoNewMasterKey).trim();
            Log.d("encDemoNewMasterKey: ",newTMK);
            Session.setTMKKEY(tmkFromApi);
            int keyIndex = getKeyIndex();


            if(pos !=null){
                //pos.setMasterKey(Utils.bytes2Hex(encDemoNewMasterKey).trim(), Utils.bytes2Hex(demoNewMasterKeyKcv).trim(), //new master key
                pos.setMasterKey(newTMK, kvc, keyIndex);

                Log.d("DEVICESTATUS: ","Device Is NOT Null");
            }else{
                Log.d("DEVICESTATUS: ","Device Is Null");
            }



            Log.d("Master key end-->","");
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
    }


    public boolean setWorkKeyNew(QPOSService pos,String tpk,String kvc){
        bRet =false;
        try {
            updateWorkKey(pos,tpk,kvc);
            bRet=true;
            if(bRet){
                Log.d("loadWorkKey","successful");
            }else
            {
                Log.d("loadWorkKey","Failed");
            }

        }
        catch (Exception e)
        {
            Log.d("LoadWorkKeyException",e.getMessage());
            return bRet;
        }
        return bRet;
    }
    //-----------------------------------D60--------------------------

    public void  updateWorkKey(QPOSService pos,String tpk,String kvc){
        try {

            int keyIndex = getKeyIndex();

            Session.setIsKeyReset(false);
            pos.updateWorkKey(tpk, kvc,//PIN KEY
                    tpk, kvc,  //TRACK KEY
                    tpk, kvc, //MAC KEY
                    keyIndex, 5);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public boolean loadD60MasterKey(QPOSService pos,String tmk,String kvc){
        try {
            //tmk="644B2324D1D0E117C141DCAE7376C409";
            Log.i("QPOSService-----", pos+"");
            // Log.i("QPOSService", this.pos.isQposPresent()+"");
            Log.i("MasterKey***", tmk);

            int keyIndex = getKeyIndex();
            Log.d("KeyIndex: ",keyIndex+"");
            //pos.setMasterKey(tmk, kvc, keyIndex,3600);
            updateMasterKey(pos,tmk,kvc);
            bRet=true;
            if(bRet){
                Log.d("LoadMasterKey","executed");
            }else{
                Log.d("LoadMasterKey","Failed");
            }

        } catch (Exception e) {
            //e.printStackTrace();
            Log.d("LoadMasterKeyException",e.getMessage());
            return false;
        }
        return bRet;
    }

}
