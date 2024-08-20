package com.isw.payapp.terminal.services;

import android.content.Context;
import android.util.Log;

import com.isw.payapp.terminal.factory.PEDFactory;
import com.isw.payapp.utils.RSAUtil;
import com.telpo.emv.EmvService;
import com.telpo.pinpad.PinpadService;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class KeyDownloadSrv {

    private Context context;
    private RSAUtil rsaUtil;
    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;
    private KeyPair keyPair;
    private KeyPairGenerator keyPairGenerator;
    private PEDFactory pedFac;
    private PinpadService pinpadService;
    private EmvService emvService;
    int ret;
    private static final String Algorithm = "DESede";
    private SecretKey deskey;
    Map<String,Object> pk ;
    List<String> pk_ ;

    public KeyDownloadSrv(Context context) {
        this.context = context;
        this.pedFac = pedFac;
        //this.rsaUtil = new RSAUtil(publicKey, privateKey, keyPair, keyPairGenerator);

    }

    public void DoKeyDownloaderProcess(){
        String dataval = "Test Telpo RSA";
        rsaUtil = new RSAUtil(publicKey, privateKey, keyPair, keyPairGenerator);
        pk_ = new ArrayList<>();
        try{

            pedFac = new PEDFactory(context);

            pk_ = rsaUtil.GetRsaEnc();
            Log.i("DOKEYDOWNLOADLIST", "pkMod : "+pk_.get(0) +"\n pkExp : "+pk_.get(1));

        }catch (Exception e){
            Log.e("DOKEYDOWNLOAD", String.valueOf(e.getStackTrace()));
        }

    }

    public List<String>PKMod(){
        rsaUtil = new RSAUtil(publicKey, privateKey, keyPair, keyPairGenerator);
        List<String> publicKeyList = new ArrayList<>();
        try{

            pedFac = new PEDFactory(context);

            publicKeyList = rsaUtil.GetRsaEnc();
            //Log.i("DOKEYDOWNLOADLIST", "pkMod : "+publicKeyList.get(0) +"\n pkExp : "+publicKeyList.get(1));

        }catch (Exception e){
            Log.e("DOKEYDOWNLOAD", String.valueOf(e.getStackTrace()));
        }
        return publicKeyList;
    }

    public String getDecreaptedValue(String in){
       // rsaUtil = new RSAUtil(publicKey, privateKey, keyPair, keyPairGenerator);
        String dec_out = "";
        try{
            dec_out = rsaUtil.GetRsaDec(in,"Test");
//            deskey = new SecretKeySpec(dec_out.getBytes(), Algorithm);
//            Cipher cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
//            cipher.init(Cipher.DECRYPT_MODE,deskey);
//            byte[] out = cipher.doFinal(dec_out.getBytes());
//            dec_out = new String(out, java.nio.charset.StandardCharsets.UTF_8);
        }catch (Exception e){
            e.printStackTrace();
            Log.e("ERROR S", String.valueOf(e.getStackTrace()));
        }

        return  dec_out;
    }



}
