package com.isw.payapp.devices.castles.CastlesApplication;

import static CTOS.CtKMS2SymmetryKey.MAC_METHOD_CBC;

import android.content.Context;
import android.util.Log;

import com.isw.payapp.devices.interfaces.IPinPadProcessor;
import com.isw.payapp.utils.DUKPK2009_CBC;
import com.isw.payapp.utils.ThreeDES;

import java.nio.charset.StandardCharsets;

import CTOS.CtKMS2Dukpt;
import CTOS.CtKMS2Exception;
import CTOS.CtKMS2Key;
import CTOS.CtKMS2System;

public class CastlesPinPadService implements IPinPadProcessor {
    private Context context;
    private CtKMS2System system;
    private CtKMS2Key key;
    private static final String TAG = "CastlesPinPadService";

    public CastlesPinPadService(Context context){
        this.context = context;
        system = new CtKMS2System();
        key = new CtKMS2Key();
    }
    @Override
    public void initPinPad() {
        try {
            system.init();
        }catch (CtKMS2Exception e){
            e.getStackTrace();
        }
    }

    @Override
    public int injectDukptKey(String key, String iKsn, String kcv) {

        int iRet = 0;
        try {
            // Extract the actual key value
            String actualKey = key.substring(8, 40);
            Log.d(TAG, "Injecting DUKPT key: " + actualKey + ", KSN: " + iKsn);
            byte[] ipekBytes = DUKPK2009_CBC.GenerateIPEK(
                    ThreeDES.hexStringToByteArray(iKsn),
                    ThreeDES.hexStringToByteArray(actualKey)
            );
            String ipek = ThreeDES.byteArrayToHexString(ipekBytes).toUpperCase();
            Log.i(TAG,"IPEK: " + ipek+"===="+iKsn);

            // Set the key group name using application context
            String packageName = context.getPackageName();

            String kcv__ = ThreeDES.generateKeyCheckValue(ipek, ThreeDES.KcvMethod.ANSI);
            Log.i(TAG,"IPEK KCV: "+ kcv__);

            // Convert key and KSN to bytes
            byte[] bKeyValue = hexStringToBytes(ipek);
            byte[] bKsnValue = hexStringToBytes(iKsn);
            byte[] icv = hexStringToBytes(kcv__);

            if (bKeyValue == null || bKsnValue == null) {
                Log.e(TAG, "Invalid key or KSN format");
                return -1;
            }

            String version = system.getKMS2LibVersion();
            Log.i(TAG,"KMS2 Lib Version = "+version);
            system.checkAllKey();
            byte[] keyResult = system.getAllKeyResult();
            String resultString = new String(keyResult, StandardCharsets.US_ASCII);
            Log.i(TAG,"keyResult = "+keyResult +"\n"+ resultString + "\n"+ThreeDES.byteArrayToHexString(keyResult));

            /* Set related parameters */
            int KeySet = 0xC001;
            int KeyIndex = 0x0000;
           // system.checkKey(KeySet, KeyIndex);
            byte CipherMethod = MAC_METHOD_CBC;

            CtKMS2Dukpt dukpt = new CtKMS2Dukpt(KeySet, KeyIndex);
            //dukpt.selectKey(KeySet, KeyIndex);
            dukpt.setCipherMethod(CipherMethod);
            dukpt.useCurrentKey(false);

//            dukpt.setICV(bKeyValue, 0, bKeyValue.length);
//            dukpt.setInputData(bKsnValue, 0, bKsnValue.length);
            dukpt.setICV(icv, 0, icv.length);
            dukpt.setInputData(bKsnValue, 0, bKeyValue.length);
            byte[] output = dukpt.generateMAC();
            byte[] ksn = dukpt.getKSN();
            Log.i(TAG,"KSN: "+ ThreeDES.byteArrayToHexString(ksn) +"MAC "+ ThreeDES.byteArrayToHexString(output));


        }catch (CtKMS2Exception e){
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
        return 0;
    }

    @Override
    public int resetKey() {
        return 0;
    }

    @Override
    public void deleteKey() {
        try {
            system.deleteAllKey();
        }catch (CtKMS2Exception e){
            e.getStackTrace();
        }
    }

    @Override
    public int deleteKeys() {
        return 0;
    }

    @Override
    public void deviceClose() {

    }

    private byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.length() % 2 != 0) {
            return null;
        }

        try {
            int len = hexString.length();
            byte[] data = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                        + Character.digit(hexString.charAt(i + 1), 16));
            }
            return data;
        } catch (Exception e) {
            Log.e(TAG, "Error converting hex string to bytes: " + e.getMessage(), e);
            return null;
        }
    }
}
