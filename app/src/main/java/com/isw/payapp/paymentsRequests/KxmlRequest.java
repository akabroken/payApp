package com.isw.payapp.paymentsRequests;


import com.isw.payapp.terminal.config.TerminalConfig;
import com.isw.payapp.model.CardModel;
import com.isw.payapp.model.EmvModel;
import com.isw.payapp.model.TransactionData;
import com.isw.payapp.utils.CommonUtil;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class KxmlRequest {
    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String XML_VERSION = "1.0";
    private static final String ENCODING = "UTF-8";

    private final EmvModel emvModel;
    private final TransactionData payData;
    private final CardModel cardModel;
    private final String timeStamp;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private TerminalConfig terminalConfig;

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

    private String generatePayload() throws XMLStreamException {
        StringWriter stringWriter = new StringWriter();
        XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
        XMLStreamWriter xmlWriter = xmlOutputFactory.createXMLStreamWriter(stringWriter);

        try {
            writeXmlDocument(xmlWriter);
            return stringWriter.toString()
                    .replace("<?xml version='" + XML_VERSION + "' encoding='" + ENCODING + "'?>", "");
        } finally {
            xmlWriter.close();
        }
    }

    private void writeXmlDocument(XMLStreamWriter xmlWriter) throws XMLStreamException {
        xmlWriter.writeStartDocument(XML_VERSION, ENCODING);
        xmlWriter.writeStartElement(payData.getPaymentApp());

        writeTerminalInformation(xmlWriter);
        writeCardData(xmlWriter);
        writeTransactionDetails(xmlWriter);
        writePinData(xmlWriter);

        xmlWriter.writeEndElement(); // </purchaseRequest>
        xmlWriter.writeEndDocument();
    }

    private void writeTerminalInformation(XMLStreamWriter xmlWriter) throws XMLStreamException {
        xmlWriter.writeStartElement("terminalInformation");
        writeElement(xmlWriter, "batteryInformation", "100");
        writeElement(xmlWriter, "cellStationId", "");
        writeElement(xmlWriter, "currencyCode", "404");
        writeElement(xmlWriter, "languageInfo", "EN");
        writeElement(xmlWriter, "merchantId", payData.getMid());
        writeElement(xmlWriter, "merchantLocation", payData.getMloc());
        writeElement(xmlWriter, "posConditionCode", "00");
        writeElement(xmlWriter, "posDataCode", payData.getPosdatacode());
        writeElement(xmlWriter, "merchantType", "4722");
        writeElement(xmlWriter, "posEntryMode", payData.getPosEntryMode());
        writeElement(xmlWriter, "posGeoCode", payData.getPosgeocode());
        writeElement(xmlWriter, "printerStatus", "1");
        writeElement(xmlWriter, "terminalId", payData.getTid());
        writeElement(xmlWriter, "terminalType", "TELPO");
        writeElement(xmlWriter, "transmissionDate", timeStamp.replace(" ", "T"));
        writeElement(xmlWriter, "uniqueId", "5F095339");
        xmlWriter.writeEndElement(); // </terminalInformation>
    }

    private void writeCardData(XMLStreamWriter xmlWriter) throws XMLStreamException {
        xmlWriter.writeStartElement("cardData");
        writeElement(xmlWriter, "cardSequenceNumber", emvModel.getCarSeqNo());

        writeEmvData(xmlWriter);
        writeTrack2Data(xmlWriter);

        writeElement(xmlWriter, "wasFallback", "");
        xmlWriter.writeEndElement(); // </cardData>
    }

    private void writeEmvData(XMLStreamWriter xmlWriter) throws XMLStreamException {
        xmlWriter.writeStartElement("emvData");
        writeElement(xmlWriter, "AmountAuthorized", emvModel.getAmountAuthorized());
        writeElement(xmlWriter, "AmountOther", emvModel.getAmountOther());
        writeElement(xmlWriter, "ApplicationInterchangeProfile", emvModel.getApplicationInterchangeProfile());
        writeElement(xmlWriter, "atc", emvModel.getAtc());
        writeElement(xmlWriter, "Cryptogram", emvModel.getCryptogram());
        writeElement(xmlWriter, "CryptogramInformationData", emvModel.getCryptogramInformationData());
        writeElement(xmlWriter, "CvmResults", emvModel.getCvmResults());
        writeElement(xmlWriter, "iad", emvModel.getIssuerApplicationData());
        writeElement(xmlWriter, "TransactionCurrencyCode", emvModel.getTransactionCurrencyCode().substring(1));
        writeElement(xmlWriter, "TerminalVerificationResult", emvModel.getTerminalVerificationResult());
        writeElement(xmlWriter, "TerminalCountryCode", emvModel.getTerminalCountryCode().substring(1));
        writeElement(xmlWriter, "TerminalType", emvModel.getTerminalType());
        writeElement(xmlWriter, "TerminalCapabilities", emvModel.getTerminalCapabilities());
        writeElement(xmlWriter, "TransactionDate", emvModel.getTransactionDate());
        writeElement(xmlWriter, "TransactionType", emvModel.getTransactionType());
        writeElement(xmlWriter, "UnpredictableNumber", emvModel.getUnpredictableNumber());
        writeElement(xmlWriter, "DedicatedFileName", emvModel.getDedicatedFileName());
        xmlWriter.writeEndElement(); // </emvData>
    }

    private void writeTrack2Data(XMLStreamWriter xmlWriter) throws XMLStreamException {
        xmlWriter.writeStartElement("track2");
        writeElement(xmlWriter, "pan", emvModel.getPan());
        writeElement(xmlWriter, "expiryMonth", emvModel.getExMonth());
        writeElement(xmlWriter, "expiryYear", emvModel.getExpYear());
        writeElement(xmlWriter, "track2", emvModel.getTrack2data());
        writeElement(xmlWriter, "serviceRestrictionCode", emvModel.getServiceCode());
        xmlWriter.writeEndElement(); // </track2>
    }

    private void writeTransactionDetails(XMLStreamWriter xmlWriter) throws XMLStreamException {
        int amount = (int) (Double.parseDouble(payData.getAmount()) * 100);

        writeElement(xmlWriter, "fromAccount", "default");
        writeElement(xmlWriter, "stan", new CommonUtil().goRundom(6));
        writeElement(xmlWriter, "minorAmount", String.valueOf(amount));
        writeElement(xmlWriter, "track1Data", emvModel.getTrack1data());
        writeElement(xmlWriter, "rate", "");
        writeElement(xmlWriter, "settlementFee", "");
        writeElement(xmlWriter, "settlementCurrencyCode", "");
        writeElement(xmlWriter, "amountSettlement", "");
        writeElement(xmlWriter, "surcharge", "");
        writeElement(xmlWriter, "tmsConfiguredTerminalLocation", "");
        writeElement(xmlWriter, "acquiringInstitutionId", "420400");
        writeElement(xmlWriter, "terminalOwner", "420400");
    }

    private void writePinData(XMLStreamWriter xmlWriter) throws XMLStreamException {
        xmlWriter.writeStartElement("pinData");
        writeElement(xmlWriter, "ksn", cardModel.getKsn());
        writeElement(xmlWriter, "ksnd", cardModel.getKsnd());
        writeElement(xmlWriter, "pinBlock", cardModel.getPinBlock());
        writeElement(xmlWriter, "pinType", cardModel.getPinType());
        xmlWriter.writeEndElement(); // </pinData>

        writeElement(xmlWriter, "keyLabel", cardModel.getKSNTag());
    }

    private void writeElement(XMLStreamWriter xmlWriter, String name, String value) throws XMLStreamException {
        if (value == null) value = "";
        xmlWriter.writeStartElement(name);
        xmlWriter.writeCharacters(value);
        xmlWriter.writeEndElement();
    }
}