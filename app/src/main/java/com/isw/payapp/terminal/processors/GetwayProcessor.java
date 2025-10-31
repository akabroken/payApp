package com.isw.payapp.terminal.processors;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.isw.payapp.commonActions.Communication;
import com.isw.payapp.commonActions.TerminalXmlParser;
import com.isw.payapp.terminal.accessors.TerminalModelAccessor;
import com.isw.payapp.terminal.config.TerminalConfig;
//import com.isw.payapp.terminal.factory.PEDFactory;
import com.isw.payapp.terminal.services.KeyDownloadSrv;
import com.isw.payapp.utils.HexConverter;
import com.isw.payapp.utils.RSAUtil;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetwayProcessor {

    private TerminalModelAccessor terminalModelAccessor;
    private TerminalXmlParser parser;
    private RSAUtil rsaUtil;
//    private PEDFactory pedFac;
    private Communication comms;
    private String url;
    private KeyDownloadSrv keyDownloadSrv;
    private List<String> pkModExp;
    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;
    private KeyPair keyPair;
    private KeyPairGenerator keyPairGenerator;
    private String in;
    private Context context;
    public GetwayProcessor(Context context){
        this.in = in;
        this.context = context;
    }

    public String process(String in) throws Exception {
        TerminalConfig terminalConfig = new TerminalConfig();
        //String postUrl = "https://apps.qa.interswitch-ke.com:7075/kmw/kimonoservice/kenya";
       String postUrl = "https://kimono.interswitch-ke.com:455/kmw/kimonoservice/kenya";
       StringBuilder sb = new StringBuilder();
       sb.append("https://")
               .append(terminalConfig.loadTerminalDataFromJson(context,"__transip"))
               .append(":")
               .append(terminalConfig.loadTerminalDataFromJson(context, "__transport"))
               .append(terminalConfig.loadTerminalDataFromJson(context,"__acturl"));

        postUrl = sb.toString();
        comms = new Communication();
        parser = new TerminalXmlParser();
        Log.i("DOKEYDOWNLOADLIST", "Start\n"+in);
        String out = comms.KimonoPost(postUrl,in);
        Log.i("DOKEYDOWNLOADLIST", out);
        return out;
    }

    //smarttrans.interswitch-ke.com
    public String processLogin(String in) throws Exception{
        String out  = null;
        String url = "https://smarttrans.interswitch-ke.com:81/SmartControlSvc.svc/pos/user/request";
        comms = new Communication();
        out = comms.KimonoPost(url,in);
        return out;
    }
}
