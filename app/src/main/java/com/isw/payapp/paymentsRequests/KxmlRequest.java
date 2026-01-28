package com.isw.payapp.paymentsRequests;

import com.isw.payapp.helpers.ConfigManager;
import com.isw.payapp.model.TerminalConfigModel;
import com.isw.payapp.terminal.config.TerminalConfig;
import com.isw.payapp.model.CardModel;
import com.isw.payapp.model.EmvModel;
import com.isw.payapp.model.TransactionData;
import com.isw.payapp.utils.CommonUtil;

import org.xmlpull.v1.XmlSerializer;

import android.util.Xml;

import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class KxmlRequest {
    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String ENCODING = "UTF-8";

    private final EmvModel emvModel;
    private final TransactionData payData;
    private final CardModel cardModel;
    private final String timeStamp;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public KxmlRequest(EmvModel emvModel, TransactionData payData, CardModel cardModel) {
        this.emvModel = emvModel;
        this.payData = payData;
        this.cardModel = cardModel;
        this.timeStamp = new SimpleDateFormat(TIMESTAMP_FORMAT).format(new Date());
    }

    public String Payload() throws Exception {
        Future<String> future = executorService.submit(this::generatePayload);
        try {
            return future.get();
        } finally {
            executorService.shutdown();
        }
    }

    public String generatePayload() throws IOException {
        StringWriter stringWriter = new StringWriter();
        XmlSerializer serializer = Xml.newSerializer();

        serializer.setOutput(stringWriter);
        serializer.startDocument(ENCODING, true);

        writeXmlDocument(serializer);

        serializer.endDocument();
        return stringWriter.toString();
    }

    private void writeXmlDocument(XmlSerializer serializer) throws IOException {
        serializer.startTag(null, payData.getPaymentApp());
        if(payData.getPaymentApp().equals("purchaseRequest")){
            serializer.startTag(null,"app");
            serializer.text("PurchaseRequest");
            serializer.endTag(null,"app");
        }
        else {

            serializer.startTag(null,"app");
            serializer.text("ReversalRequestWithoutOriginalDate");
            serializer.endTag(null,"app");
        }

        writeTerminalInformation(serializer);
        writeCardData(serializer);
        writeTransactionDetails(serializer);
        writePinData(serializer);

        serializer.endTag(null, payData.getPaymentApp());
    }

    private void writeTerminalInformation(XmlSerializer serializer) throws IOException {
        serializer.startTag(null, "terminalInformation");

        writeElement(serializer, "batteryInformation", "100");
        writeElement(serializer, "cellStationId", "");
        writeElement(serializer, "currencyCode", "404");
        writeElement(serializer, "languageInfo", "EN");
        writeElement(serializer, "merchantId", payData.getMid());
        writeElement(serializer, "merchantLocation", payData.getMloc());
        writeElement(serializer, "posConditionCode", "00");
        writeElement(serializer, "posDataCode", payData.getPosdatacode());
        writeElement(serializer, "merchantType", "4722");
        writeElement(serializer, "posEntryMode", payData.getPosEntryMode());
        writeElement(serializer, "posGeoCode", payData.getPosgeocode());
        writeElement(serializer, "printerStatus", "1");
        writeElement(serializer, "terminalId", payData.getTid());
        writeElement(serializer, "terminalType", "TELPO");
        writeElement(serializer, "transmissionDate", timeStamp.replace(" ", "T"));
        writeElement(serializer, "uniqueId", "5F095339");

        serializer.endTag(null, "terminalInformation");
    }

    private void writeCardData(XmlSerializer serializer) throws IOException {
        serializer.startTag(null, "cardData");

        writeElement(serializer, "cardSequenceNumber", emvModel.getCarSeqNo());
        writeEmvData(serializer);
        writeTrack2Data(serializer);
        writeElement(serializer, "wasFallback", "false");

        serializer.endTag(null, "cardData");
    }

    private void writeEmvData(XmlSerializer serializer) throws IOException {
        serializer.startTag(null, "emvData");

        writeElement(serializer, "AmountAuthorized", emvModel.getAmountAuthorized());
        writeElement(serializer, "AmountOther", emvModel.getAmountOther());
        writeElement(serializer, "ApplicationInterchangeProfile", emvModel.getApplicationInterchangeProfile());
        writeElement(serializer, "atc", emvModel.getAtc());
        writeElement(serializer, "Cryptogram", emvModel.getCryptogram());
        writeElement(serializer, "CryptogramInformationData", emvModel.getCryptogramInformationData());
        writeElement(serializer, "CvmResults", emvModel.getCvmResults());
        writeElement(serializer, "iad", emvModel.getIssuerApplicationData());
        writeElement(serializer, "TransactionCurrencyCode",
                safeSubstring(emvModel.getTransactionCurrencyCode(), 1));
        writeElement(serializer, "TerminalVerificationResult", emvModel.getTerminalVerificationResult());
        writeElement(serializer, "TerminalCountryCode",
                safeSubstring(emvModel.getTerminalCountryCode(), 1));
        writeElement(serializer, "TerminalType", emvModel.getTerminalType());
        writeElement(serializer, "TerminalCapabilities", emvModel.getTerminalCapabilities());
        writeElement(serializer, "TransactionDate", emvModel.getTransactionDate());
        writeElement(serializer, "TransactionType", emvModel.getTransactionType());
        writeElement(serializer, "UnpredictableNumber", emvModel.getUnpredictableNumber());
        writeElement(serializer, "DedicatedFileName", emvModel.getDedicatedFileName());

        serializer.endTag(null, "emvData");
    }

    private void writeTrack2Data(XmlSerializer serializer) throws IOException {
        serializer.startTag(null, "track2");

        writeElement(serializer, "pan", cardModel.getPan());
        writeElement(serializer, "expiryMonth", emvModel.getExMonth());
        writeElement(serializer, "expiryYear", emvModel.getExpYear());
        writeElement(serializer, "track2", emvModel.getTrack2data());
        writeElement(serializer, "serviceRestrictionCode", emvModel.getServiceCode());

        serializer.endTag(null, "track2");
    }

    private void writeTransactionDetails(XmlSerializer serializer) throws IOException {
        int amount = 0;
        try {
            amount = (int) (Double.parseDouble(payData.getAmount()) * 100);
        } catch (NumberFormatException e) {
            amount = 0;
        }

        writeElement(serializer, "fromAccount", "default");
        writeElement(serializer, "stan", new CommonUtil().goRundom(6));
        writeElement(serializer, "minorAmount", String.valueOf(amount));
        writeElement(serializer, "track1Data", emvModel.getTrack1data());
        writeElement(serializer, "rate", "");
        writeElement(serializer, "settlementFee", "");
        writeElement(serializer, "settlementCurrencyCode", "");
        writeElement(serializer, "amountSettlement", "");
        writeElement(serializer, "surcharge", "");
        writeElement(serializer, "tmsConfiguredTerminalLocation", "");
//        writeElement(serializer, "acquiringInstitutionId", "420400");
//        writeElement(serializer, "terminalOwner", "420400");
        if(payData.getAuthCode() != null){
            writeElement(serializer, "originalTransmissionDateTime", timeStamp.replace(" ", "T"));
            writeElement(serializer, "notDisposable", "false");
            writeElement(serializer, "originalAuthId", payData.getAuthCode());
            writeElement(serializer, "authId", payData.getAuthCode());
            writeElement(serializer, "reversalType", "Reservation");
        }
        if(payData.getTransCnt() !=null)
            writeElement(serializer, "originalStan", payData.getTransCnt());

    }

    private void writePinData(XmlSerializer serializer) throws IOException {

        serializer.startTag(null, "pinData");

        writeElement(serializer, "ksn", cardModel.getKsn().substring(4));
        writeElement(serializer, "ksnd", cardModel.getKsnd());
        writeElement(serializer, "pinBlock", cardModel.getPinBlock());
        writeElement(serializer, "pinType", cardModel.getPinType());

        serializer.endTag(null, "pinData");
        writeElement(serializer, "keyLabel", cardModel.getKSNTag());
    }

    private void writeElement(XmlSerializer serializer, String name, String value) throws IOException {
        if (value == null) value = "";
        serializer.startTag(null, name);
        serializer.text(value);
        serializer.endTag(null, name);
    }

    private String safeSubstring(String value, int beginIndex) {
        if (value == null || value.length() <= beginIndex) {
            return "";
        }
        return value.substring(beginIndex);
    }
}