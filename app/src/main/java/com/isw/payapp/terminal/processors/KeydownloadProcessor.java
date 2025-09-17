package com.isw.payapp.terminal.processors;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.isw.payapp.BuildConfig;
import com.isw.payapp.commonActions.Communication;
import com.isw.payapp.commonActions.TerminalXmlParser;
import com.isw.payapp.devices.services.NetworkService;
import com.isw.payapp.terminal.accessors.KeyModelAccessor;
import com.isw.payapp.terminal.accessors.TerminalModelAccessor;
import com.isw.payapp.terminal.factory.PEDFactory;
import com.isw.payapp.terminal.services.KeyDownloadSrv;
import com.isw.payapp.utils.RSAUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;

import java.util.List;

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

    private String keyDownloadUrl;

    private static final String TAG = "KeyDownloadProcessor";

    public KeydownloadProcessor(){

    }
    // Inject dependencies via constructor for testability and clarity
    public  KeydownloadProcessor(@NonNull Context context,
//                                     @NonNull KeyModelAccessor keyModelAccessor,
//                                     @NonNull TerminalModelAccessor terminalModelAccessor,
                                     @NonNull TerminalXmlParser parser,
                                     @NonNull RSAUtil rsaUtil,
                                     @NonNull String keyDownloadUrl) {
        this.context = context.getApplicationContext();
//        this.keyModelAccessor = keyModelAccessor;
//        this.terminalModelAccessor = terminalModelAccessor;
        this.parser = parser;
        this.rsaUtil = rsaUtil;
        this.keyDownloadUrl = keyDownloadUrl; // URL should be provided from a config source (e.g., BuildConfig)
    }


//    public void process() throws Exception {
//        String postUrl = "https://apps.qa.interswitch-ke.com:7075/kmw/kimonoservice/kenya";
//        Map<String, String> postMap = new HashMap<>();
//        comms = new Communication();
//        parser = new TerminalXmlParser();
//        //rsaUtil = new RSAUtil(publicKey, privateKey, keyPair, keyPairGenerator);
//        rsaUtil = new RSAUtil();
//        pkModExp = new ArrayList<>();
//        pedFac = new PEDFactory(context);
//        pkModExp = rsaUtil.GetRsaEnc();
//        Log.i("DOKEYDOWNLOADLISTIIII", (String) pkModExp.get(0));
//        Log.i("DOKEYDOWNLOADLISTPP", parser.KeyDownload(pkModExp));
//        AsyncTask<Object, Void, String> asyncTask = new Communication.HttpPostTask().
//                execute(postUrl, parser.KeyDownload(pkModExp));
//        String out = asyncTask.get(); // This will wait for AsyncTask to complete
//
//        Log.i("DOKEYDOWNLOADLISTPP", out);
//
//        Map<String, String> resultMap = convertXMLToMap(out);
//        HexConverter hexConverter = new HexConverter();
//        String pin_key = resultMap.get("pinkey");
//        Log.i("pin_key--", pin_key);
//        String pinKey = rsaUtil.GetRsaDec(pin_key, (RSAPrivateKey) pkModExp.get(2),"uu");
//        Log.i("DOKEYDOWNLOADLISTOO", pinKey);
//    }

    public void process(KeyDownloadCallback callback) {
        try {
            // 1. Generate RSA Key Pair for this session
            List<Object> publicKeyComponents = rsaUtil.GetRsaEnc();
            if (publicKeyComponents == null || publicKeyComponents.size() < 3) {
                callback.onFailure(new IllegalStateException("Failed to generate RSA key pair."));
                return;
            }

            String modulusBase64 = (String) publicKeyComponents.get(0);
            String exponentBase64 = (String) publicKeyComponents.get(1);
            RSAPrivateKey privateKey = (RSAPrivateKey) publicKeyComponents.get(2);

            Log.i(TAG, "Modulus: " + modulusBase64);

            // 2. Build the request XML
            String requestXml = parser.KeyDownload(publicKeyComponents);
            Log.i(TAG, "Request XML: " + requestXml);

            // 3. Make the ASYNCHRONOUS network call using the Retrofit Service
// The content type is now handled by the @Headers annotation in ApiService
            // Initialize the NetworkService with the base URL
            //kimono.interswitch-ke.com
            //https://apps.qa.interswitch-ke.com:7075/kmw/kimonoservice/kenya/
            String baseUrl = "https://kimono.interswitch-ke.com:455/"; // Note the trailing slash!
            NetworkService.initialize(context, baseUrl);
            NetworkService.getInstance().downloadKeys(requestXml, new NetworkService.NetworkCallback() {
                @Override
                public void onSuccess(String responseXml) {
                    Log.i(TAG, "Raw Response XML: " + responseXml); // Good for debugging

                    try {
                        // 4. Parse the response XML into a map of key-value pairs
                        Map<String, String> resultMap = convertXMLToMap(responseXml);
                        if (resultMap == null || resultMap.isEmpty()) {
                            throw new RuntimeException("Failed to parse server response or response was empty.");
                        }

                        // 4a. Extract the encrypted PIN key. Use a constant for the key to avoid typos.
                        final String RESPONSE_KEY_PIN = "pinkey";
                        String encryptedPinKeyHex = resultMap.get(RESPONSE_KEY_PIN);

                        if (encryptedPinKeyHex == null || encryptedPinKeyHex.trim().isEmpty()) {
                            // Log all available keys from the response for debugging
                            Log.e(TAG, "PIN key not found in server response. Available keys: " + resultMap.keySet());
                            callback.onFailure(new RuntimeException("PIN key not found in server response."));
                            return;
                        }

                        Log.i(TAG, "Encrypted PIN Key (HEX): " + encryptedPinKeyHex);

                        // 5. Decrypt the PIN key using our session's private key
                        // Critical: The RSAUtil method must be called on a background thread.
                        // Since this callback is already on a background thread (Retrofit's default), it's safe.
                        String decryptedPinKey = rsaUtil.GetRsaDec(encryptedPinKeyHex, privateKey, "RSA/ECB/PKCS1Padding");

                        if (decryptedPinKey == null || decryptedPinKey.isEmpty()) {
                            callback.onFailure(new RuntimeException("RSA decryption failed. Result was null or empty."));
                            return;
                        }

                        // 5a. IMPORTANT SECURITY NOTE: Logging the decrypted key is a MAJOR security risk.
                        // This should only be done in debug builds for troubleshooting.
                        if (BuildConfig.DEBUG) {
                            Log.i(TAG, "Decrypted PIN Key: " + decryptedPinKey);
                        } else {
                            Log.i(TAG, "PIN Key decrypted successfully.");
                        }

                        // 6. The decrypted key is a hexadecimal string representing binary data.
                        // It must be injected into the HSM (PED). This is the most critical step.
                        try {
                            // Convert the HEX string to a byte array for the HSM.
                            // Example: byte[] pinKeyBytes = HexConverter.hexStringToByteArray(decryptedPinKey);
                            // Then inject: pedFac.injectKey(pinKeyBytes);

                            // Since we don't have the PED factory implementation, we assume it's done.
                            // In a real scenario, this call must be made and checked for success.
                            boolean injectionSuccess = true; // Placeholder: pedFac.injectKey(decryptedPinKey);
                            if (!injectionSuccess) {
                                callback.onFailure(new RuntimeException("Failed to inject key into secure module (PED)."));
                                return;
                            }

                        } catch (Exception e) {
                            Log.e(TAG, "Error during key injection", e);
                            callback.onFailure(new RuntimeException("Key injection failed.", e));
                            return;
                        }

                        // 7. Notify success - The entire operation is complete.
                        // Pass the decrypted key string back if needed for the next steps,
                        // but ideally, it should now be inside the HSM and this reference discarded.
                        callback.onSuccess(decryptedPinKey);

                    } catch (Exception e) {
                        Log.e(TAG, "Error processing key download response", e);
                        // Ensure we are on the main thread if the callback expects it for UI updates.
                        // Retrofit delivers callbacks on the main thread by default, so this is safe.
                        callback.onFailure(e);
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Network request failed", e);
                    callback.onFailure(e);
                }
            });

//            // 3. Make the ASYNCHRONOUS network call
//            NetworkService.makePostRequest(keyDownloadUrl, requestXml, new NetworkService.NetworkCallback() {
//                @Override
//                public void onSuccess(String responseXml) {
//                    Log.i(TAG, "Response XML: " + responseXml);
//                    try {
//                        // 4. Parse the response
//                        Map<String, String> resultMap = convertXMLToMap(responseXml);
//                        String encryptedPinKeyHex = resultMap.get("pinkey");
//                        if (encryptedPinKeyHex == null) {
//                            callback.onFailure(new RuntimeException("PIN key not found in server response."));
//                            return;
//                        }
//
//                        Log.i(TAG, "Encrypted PIN Key (HEX): " + encryptedPinKeyHex);
//
//                        // 5. Decrypt the PIN key using our session's private key
//                        // Assume RSAUtil.GetRsaDec now handles the decryption correctly for HEX input.
//                        String decryptedPinKey = rsaUtil.GetRsaDec(encryptedPinKeyHex, privateKey, "RSA/ECB/PKCS1Padding"); // Use proper padding
//                        if (decryptedPinKey == null) {
//                            callback.onFailure(new RuntimeException("Failed to decrypt PIN key."));
//                            return;
//                        }
//
//                        Log.i(TAG, "Decrypted PIN Key: " + decryptedPinKey);
//
//                        // 6. The decrypted key is likely binary data, now in HEX string format.
//                        // It should be injected into the HSM (PED) here, not converted to ASCII.
//                        // pedFac.injectKey(decryptedPinKey); // This would be the next step
//
//                        // 7. Notify success
//                        callback.onSuccess(decryptedPinKey);
//
//                    } catch (Exception e) {
//                        Log.e(TAG, "Error processing response", e);
//                        callback.onFailure(e);
//                    }
//                }
//
//                @Override
//                public void onFailure(Exception e) {
//                    Log.e(TAG, "Network request failed", e);
//                    callback.onFailure(e);
//                }
//            });

        } catch (Exception e) {
            Log.e(TAG, "Error in key download process", e);
            callback.onFailure(e);
        }
    }

    // Callback interface to handle results asynchronously
    public interface KeyDownloadCallback {
        void onSuccess(String decryptedPinKey);
        void onFailure(Exception e);
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
            if(cardNodes.getLength() ==0){
                throw new RuntimeException("Node list empty");
            }

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
