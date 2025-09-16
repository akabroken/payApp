package com.isw.payapp.callbacks;

import android.content.Context;
import android.util.Log;

import com.isw.payapp.paymentsRequests.KsmgRequest;
import com.isw.payapp.paymentsRequests.KxmlRequest;
import com.isw.payapp.tasks.EmvTLVExtractor;
import com.isw.payapp.tasks.PinPadTasks;
import com.isw.payapp.model.CardModel;
import com.isw.payapp.model.TransactionData;
import com.isw.payapp.terminal.processors.GetwayProcessor;
import com.isw.payapp.utils.StringUtils;
import com.telpo.emv.EmvAmountData;
import com.telpo.emv.EmvCandidateApp;
import com.telpo.emv.EmvOnlineData;
import com.telpo.emv.EmvPinData;
import com.telpo.emv.EmvService;
import com.telpo.emv.EmvServiceListener;
import com.telpo.pinpad.PinpadService;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.parsers.DocumentBuilderFactory;

public class IccCardReaderCallBack extends EmvServiceListener {
    private static final String TAG = "IccCardReadImpl";
    public static int Mag = 0;
    public static int IC = 1;
    public static int Nfc = 2;
    private static final short CURRENCY_CODE = 404;
    private static final byte CURRENCY_EXPONENT = 2;
    private static final Charset ASCII_CHARSET = Charset.forName("US-ASCII");

    private final Context context;
    private final EmvService emvService;
    private final TransactionData payData;
    private final int event;

    private CardModel cardModel;
    private String kimonoData;
    private String pan;
    private String rspCode;
    private String rspMsg;
    private final AtomicBoolean isUiThreadRunning = new AtomicBoolean(false);

    public IccCardReaderCallBack(Context context, EmvService emvService,
                                 TransactionData payData, int event) {
        this.context = context;
        this.emvService = emvService;
        this.payData = payData;
        this.event = event;
    }

    @Override
    public int onInputAmount(EmvAmountData emvAmountData) {
        try {
            long amount = (long) (Double.parseDouble(payData.getAmount()) * 100);
            emvAmountData.Amount = amount;
            emvAmountData.TransCurrCode = CURRENCY_CODE;
            emvAmountData.ReferCurrCode = CURRENCY_CODE;
            emvAmountData.TransCurrExp = CURRENCY_EXPONENT;
            emvAmountData.ReferCurrExp = CURRENCY_EXPONENT;
            return EmvService.EMV_TRUE;
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid amount format", e);
            return EmvService.EMV_FALSE;
        }
    }

    @Override
    public int onInputPin(EmvPinData emvPinData) {
        Log.d(TAG, "onInputPin callback type: " + emvPinData.type);

        isUiThreadRunning.set(true);
        int result = processPinInput(emvPinData);
        isUiThreadRunning.set(false);

        Log.d(TAG, "onInputPIN callback result: " + result);
        return result;
    }

    private int processPinInput(EmvPinData emvPinData) {
        try {
            PinpadService.Open(context);
            PinPadTasks pinPadTask = new PinPadTasks(context, emvPinData, payData.getAmount(), event);
            cardModel = pinPadTask.extractCardData();

            if (cardModel == null) {
                Log.e(TAG, "Card model extraction failed");
                return EmvService.EMV_FALSE;
            }

            pan = cardModel.getPan();
            Log.d(TAG, "Card data extracted - PIN Block: " + cardModel.getPinBlock() +
                    ", KSN: " + cardModel.getKsn());

            return EmvService.EMV_TRUE;
        } catch (Exception e) {
            Log.e(TAG, "PIN input processing failed", e);
            return EmvService.EMV_FALSE;
        }
    }

    @Override
    public int onSelectApp(EmvCandidateApp[] emvCandidateApps) {
        return emvCandidateApps.length > 0 ? emvCandidateApps[0].index : EmvService.EMV_FALSE;
    }

    @Override
    public int onOnlineProcess(EmvOnlineData emvOnlineData) {
        if (event != IC) {
            return EmvService.ONLINE_FAILED;
        }

        try {
            GetwayProcessor gatewayProcessor = new GetwayProcessor(context);
            EmvTLVExtractor emvTLVExtractor = new EmvTLVExtractor(emvService, payData);
            String transactionPayload = "";
            if(payData.getPaymentApp().equals("selectpin")){
                KsmgRequest pinchangeRequest = new KsmgRequest(emvTLVExtractor.extractEmvData(), payData, cardModel);
                transactionPayload = pinchangeRequest.Payload();
            }else{
                KxmlRequest purchaseData = new KxmlRequest(emvTLVExtractor.extractEmvData(), payData, cardModel);
                transactionPayload = purchaseData.Payload();
            }

            String gatewayResponse = gatewayProcessor.process(transactionPayload);
            setKimonoData(gatewayResponse);

            Document doc = parseXmlResponse(gatewayResponse);
            String responseCode = getAttributeValue(doc, "var", "name", "responsecode");
            System.out.println("Tets" + responseCode);
            if(responseCode != null){
                String responseMessage = getAttributeValue(doc, "var", "name", "responsemessage");
                rspCode = responseCode;
                rspMsg =  responseMessage;
            }
            if ("00".equals(responseCode)) {
                configureApprovedTransaction(emvOnlineData, doc);
                return EmvService.ONLINE_APPROVE;
            }
        } catch (Exception e) {
            Log.e(TAG, "Online processing failed", e);
        }
        return EmvService.ONLINE_FAILED;
    }

    private Document parseXmlResponse(String xml) throws Exception {
        return DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
    }

    private void configureApprovedTransaction(EmvOnlineData emvOnlineData, Document doc) {
        String script = getValue(doc, "script");
        String st2 = getValue(doc, "st2");
        String iad = getValue(doc, "iad");
        String rc = getValue(doc, "rc");

        String st1 = script !=null && !script.isEmpty() ?
                "71"+padStart(Integer.toHexString(script.length() / 2), 2,'0')+script:""; // Initialize with empty scripts
        st2 = st2 != null && !st2.isEmpty() ?
                "72" + padStart(Integer.toHexString(st2.length() / 2), 2, '0') + st2 : "";

        emvOnlineData.ScriptData71 = StringUtils.hexStringToByte(st1);
        emvOnlineData.ScriptData72 = StringUtils.hexStringToByte(st2);
        emvOnlineData.IssuAuthenData = StringUtils.hexStringToByte(iad);
        emvOnlineData.AuthenCode = "000000".getBytes(ASCII_CHARSET);
        emvOnlineData.ResponeCode = rc.getBytes(ASCII_CHARSET);
    }

    @Override
    public int onRequireDatetime(byte[] datetime) {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            byte[] timeBytes = timestamp.getBytes(ASCII_CHARSET);
            System.arraycopy(timeBytes, 0, datetime, 0, datetime.length);
            return EmvService.EMV_TRUE;
        } catch (Exception e) {
            Log.e(TAG, "Failed to set datetime", e);
            return EmvService.EMV_FALSE;
        }
    }

    // Other required overrides with default implementations
    @Override public int onSelectAppFail(int i) { return EmvService.EMV_TRUE; }
    @Override public int onFinishReadAppData() { return EmvService.EMV_TRUE; }
    @Override public int onVerifyCert() { return EmvService.EMV_TRUE; }
    @Override public int onRequireTagValue(int i, int i1, byte[] bytes) { return EmvService.EMV_TRUE; }
    @Override public int onReferProc() { return EmvService.EMV_TRUE; }
    @Override public int OnCheckException(String s) { return EmvService.EMV_TRUE; }
    @Override public int OnCheckException_qvsdc(int i, String s) { return EmvService.EMV_TRUE; }
    @Override public int onMir_FinishReadAppData() { return EmvService.EMV_TRUE; }
    @Override public int onMir_DataExchange() { return EmvService.EMV_TRUE; }
    @Override public int onMir_Hint() { return EmvService.EMV_TRUE; }

    public void preProcessDataRequest() {
        try {
            EmvTLVExtractor extractor = new EmvTLVExtractor(emvService, payData);
            KxmlRequest purchaseData = new KxmlRequest(extractor.extractEmvData(), payData, cardModel);
            setKimonoData(purchaseData.Payload());
            payData.setKimonoData(kimonoData);
            Log.d(TAG, "Transaction request data: " + kimonoData);
        } catch (Exception e) {
            Log.e(TAG, "Pre-process data request failed", e);
        }
    }

    public String getKimonoData() {
        return kimonoData;
    }

    public void setKimonoData(String kimonoData) {
        this.kimonoData = kimonoData;
    }



    public String getPan() {
        return pan;
    }

    public String getRspCode(){return  rspCode;}
    public String getRspMsg() {return  rspMsg; }



    private static String getValue(Document doc, String tagName) {
        NodeList nodeList = doc.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                return node.getTextContent().trim();
            }
        }
        return null;
    }

    private static String getAttributeValue(Document doc, String tagName,
                                            String attributeName, String attributeValue) {
        NodeList nodeList = doc.getElementsByTagName(tagName);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                if (attributeValue.equals(element.getAttribute(attributeName))) {
                    return element.getTextContent().trim();
                }
            }
        }
        return null;
    }

    private static String padStart(String input, int minLength, char padChar) {
        if (input == null) input = "";
        StringBuilder sb = new StringBuilder(input);
        while (sb.length() < minLength) sb.insert(0, padChar);
        return sb.toString();
    }
}