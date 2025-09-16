package com.isw.payapp.terminal.processors;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import com.isw.payapp.commonActions.Communication;
import com.isw.payapp.commonActions.TerminalXmlParser;
import com.isw.payapp.terminal.accessors.KeyModelAccessor;
import com.isw.payapp.terminal.accessors.TerminalModelAccessor;
import com.isw.payapp.terminal.factory.PEDFactory;
import com.isw.payapp.terminal.services.KeyDownloadSrv;
import com.isw.payapp.utils.HexConverter;
import com.isw.payapp.utils.RSAUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.crypto.Cipher;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPrivateKeySpec;
import java.util.HashMap;
import java.util.Map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeydownloadProcessor {

    private Context context;
    private KeyModelAccessor keyModelAccessor;

    private TerminalModelAccessor terminalModelAccessor;
    private TerminalXmlParser parser;
    private RSAUtil rsaUtil;
    private PEDFactory pedFac;
    private Communication comms;
    private String url;
    private KeyDownloadSrv keyDownloadSrv;
    private List<Object> pkModExp;
    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;
    private KeyPair keyPair;
    private KeyPairGenerator keyPairGenerator;

    public KeydownloadProcessor(){

    }


    public void process() throws Exception {
        String postUrl = "https://apps.qa.interswitch-ke.com:7075/kmw/kimonoservice/kenya";
        Map<String, String> postMap = new HashMap<>();
        comms = new Communication();
        parser = new TerminalXmlParser();
        //rsaUtil = new RSAUtil(publicKey, privateKey, keyPair, keyPairGenerator);
        rsaUtil = new RSAUtil();
        pkModExp = new ArrayList<>();
        pedFac = new PEDFactory(context);
        pkModExp = rsaUtil.GetRsaEnc();
        Log.i("DOKEYDOWNLOADLISTIIII", (String) pkModExp.get(0));
        Log.i("DOKEYDOWNLOADLISTPP", parser.KeyDownload(pkModExp));
        AsyncTask<Object, Void, String> asyncTask = new Communication.HttpPostTask().
                execute(postUrl, parser.KeyDownload(pkModExp));
        String out = asyncTask.get(); // This will wait for AsyncTask to complete

        Log.i("DOKEYDOWNLOADLISTPP", out);

        Map<String, String> resultMap = convertXMLToMap(out);
        HexConverter hexConverter = new HexConverter();
        String pin_key = resultMap.get("pinkey");
        Log.i("pin_key--", pin_key);
        String pinKey = rsaUtil.GetRsaDec(pin_key, (RSAPrivateKey) pkModExp.get(2),"uu");
        Log.i("DOKEYDOWNLOADLISTOO", pinKey);
    }

    public  String decryptHexMessage(String hexMessage, BigInteger modulus, BigInteger exponent) {
        // Convert HEX-encoded message to BigInteger
        BigInteger encryptedMessage = new BigInteger(hexMessage, 16);

        // Decrypt the message using private key
        BigInteger decryptedMessage = encryptedMessage.modPow(exponent, modulus);

        // Convert the decrypted BigInteger back to a string
        byte[] bytes = decryptedMessage.toByteArray();
        return new String(bytes);
    }



    public static Map<String, String> convertXMLToMap(String xmlString) {
        Map<String, String> resultMap = new HashMap<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(xmlString.getBytes()));

            NodeList cardNodes = document.getElementsByTagName("card");

            for (int i = 0; i < cardNodes.getLength(); i++) {
                Element cardElement = (Element) cardNodes.item(i);

                if ("CSetvars".equals(cardElement.getAttribute("name")) && "script".equals(cardElement.getAttribute("type"))) {
                    NodeList scriptNodes = cardElement.getElementsByTagName("script");
                    for (int j = 0; j < scriptNodes.getLength(); j++) {
                        Node scriptNode = scriptNodes.item(j);
                        if (scriptNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element scriptElement = (Element) scriptNode;
                            String scriptContent = scriptElement.getTextContent();

                            // Extract values from the script content
                            extractValuesFromScript(scriptContent, resultMap);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultMap;
    }

    private static void extractValuesFromScript(String scriptContent, Map<String, String> resultMap) {
        String[] lines = scriptContent.split(";");
        for (String line : lines) {
            if (line.contains("SessionAdd")) {
                // Extract variable name and value from SessionAdd calls
                String[] parts = line.split("'");
                if (parts.length >= 4) {
                    String variableName = parts[1];
                    String variableValue = parts[3];
                    resultMap.put(variableName, variableValue);
                }
            }
        }
    }

    public  String hexToAscii(String hexString) {
        StringBuilder asciiStringBuilder = new StringBuilder();

        // Iterate over pairs of characters in the hexadecimal string
        for (int i = 0; i < hexString.length(); i += 2) {
            // Extract two characters from the hexadecimal string
            String hexPair = hexString.substring(i, i + 2);

            // Convert the hexadecimal pair to an integer
            int decimalValue = Integer.parseInt(hexPair, 16);

            // Convert the integer value to a char and append to the result
            asciiStringBuilder.append((char) decimalValue);
        }

        return asciiStringBuilder.toString();
    }
}
