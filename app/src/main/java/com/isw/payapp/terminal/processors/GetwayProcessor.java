package com.isw.payapp.terminal.processors;

import android.os.AsyncTask;
import android.util.Log;

import com.isw.payapp.commonActions.Communication;
import com.isw.payapp.commonActions.TerminalXmlParser;
import com.isw.payapp.terminal.accessors.TerminalModelAccessor;
import com.isw.payapp.terminal.factory.PEDFactory;
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
    private PEDFactory pedFac;
    private Communication comms;
    private String url;
    private KeyDownloadSrv keyDownloadSrv;
    private List<String> pkModExp;
    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;
    private KeyPair keyPair;
    private KeyPairGenerator keyPairGenerator;
    private String in;

    public GetwayProcessor(){
        this.in = in;
    }

    public String process(String in) throws Exception {
        String postUrl = "https://apps.qa.interswitch-ke.com:7075/kmw/kimonoservice/kenya";

        comms = new Communication();
        parser = new TerminalXmlParser();
        Log.i("DOKEYDOWNLOADLIST", "Start\n"+in);
//        AsyncTask<Object, Void, String> asyncTask = new Communication.HttpPostTask().
//                execute(postUrl, in);
//        String out = asyncTask.get(); // This will wait for AsyncTask to complete
        String out = comms.KimonoPost(postUrl,in);

        Log.i("DOKEYDOWNLOADLIST", out);
        return out;
    }
}
