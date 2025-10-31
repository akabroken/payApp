package com.isw.payapp.payments;

import com.isw.payapp.model.CardModel;
import com.isw.payapp.model.EmvModel;
import com.isw.payapp.utils.CommonUtil;

import java.io.StringWriter;
import java.text.SimpleDateFormat;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class KimonoReversal {
    private EmvModel emvModel;
    private String Amount;
    private CardModel cardModel;
    private String originalStan;
    private String originalAuth;

    String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());

    public KimonoReversal(EmvModel emvModel, String Amount, CardModel cardModel, String originalAuth, String originalStan){
        this.emvModel = emvModel;
        this.Amount = Amount;
        this.cardModel = cardModel;
        this.originalAuth = originalAuth;
        this.originalStan = originalStan;
    }

    public String Payload(){
        String out_data = "";
        CommonUtil common  = new CommonUtil();
        int amt_int  = (int) (Double.parseDouble(Amount) * 100);
        Amount = Integer.toString(amt_int);
        try {
            // getPKVal = rsaUtil.GetRsaEnc();
            // Create StringWriter and XMLStreamWriter
            StringWriter stringWriter = new StringWriter();
            XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
            XMLStreamWriter xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(stringWriter);

            // Start writing XML
            xmlStreamWriter.writeStartDocument();
            xmlStreamWriter.writeStartElement("reversalRequestWithoutOriginalDate");

            // Write elements inside <kmsg>
           // writeElement(xmlStreamWriter, "scheme", "standard");
          //  writeElement(xmlStreamWriter, "app", "PurchaseRequest");
            //Terminal Information
            // Write <terminalInformation>
            xmlStreamWriter.writeStartElement("terminalInformation");
            writeElement(xmlStreamWriter, "batteryInformation", "100");
            writeElement(xmlStreamWriter, "cellStationId", "");
            writeElement(xmlStreamWriter, "currencyCode", "404");
            writeElement(xmlStreamWriter, "languageInfo", "EN");
            writeElement(xmlStreamWriter, "merchantId", "CBLKE0000000001");
            writeElement(xmlStreamWriter, "merchantLocation", "TEST ANDROID ISW");
            writeElement(xmlStreamWriter, "posConditionCode", "00");
            writeElement(xmlStreamWriter, "posDataCode", "510101501344101");
            writeElement(xmlStreamWriter, "merchantType", "4722");
            writeElement(xmlStreamWriter, "posEntryMode", "051");
            writeElement(xmlStreamWriter, "posGeoCode", "00255000000000254");
            writeElement(xmlStreamWriter, "printerStatus", "1");
            writeElement(xmlStreamWriter, "terminalId", "CBLKE001");
            writeElement(xmlStreamWriter, "terminalType", "TELPO");
            writeElement(xmlStreamWriter, "transmissionDate", timeStamp.replace(" ","T"));
            writeElement(xmlStreamWriter, "uniqueId", "5F095339");
            xmlStreamWriter.writeEndElement(); // </terminalInformation>

            // Write <cardData>
            xmlStreamWriter.writeStartElement("cardData");
            writeElement(xmlStreamWriter, "cardSequenceNumber", emvModel.getCarSeqNo());
            // Write <emvData>
            xmlStreamWriter.writeStartElement("emvData");
            writeElement(xmlStreamWriter, "AmountAuthorized", emvModel.getAmountAuthorized());
            writeElement(xmlStreamWriter, "AmountOther", emvModel.getAmountOther());
            writeElement(xmlStreamWriter, "ApplicationInterchangeProfile", emvModel.getApplicationInterchangeProfile());
            writeElement(xmlStreamWriter, "atc", emvModel.getAtc());
            writeElement(xmlStreamWriter, "Cryptogram", emvModel.getCryptogram());
            writeElement(xmlStreamWriter, "CryptogramInformationData", emvModel.getCryptogramInformationData());
            writeElement(xmlStreamWriter, "CvmResults", emvModel.getCvmResults());
            writeElement(xmlStreamWriter, "iad", emvModel.getIssuerApplicationData());
            writeElement(xmlStreamWriter, "TransactionCurrencyCode", emvModel.getTransactionCurrencyCode());
            writeElement(xmlStreamWriter, "TerminalVerificationResult", emvModel.getTerminalVerificationResult());
            writeElement(xmlStreamWriter, "TerminalCountryCode", emvModel.getTerminalCountryCode());
            writeElement(xmlStreamWriter, "TerminalType", emvModel.getTerminalType());
            writeElement(xmlStreamWriter, "TerminalCapabilities", emvModel.getTerminalCapabilities());
            writeElement(xmlStreamWriter, "TransactionDate", emvModel.getTransactionDate());
            writeElement(xmlStreamWriter, "TransactionType", emvModel.getTransactionType());
            writeElement(xmlStreamWriter, "UnpredictableNumber", emvModel.getUnpredictableNumber());
            writeElement(xmlStreamWriter, "DedicatedFileName", emvModel.getDedicatedFileName());
            xmlStreamWriter.writeEndElement(); // </emvData>
            // Write <track2>
            xmlStreamWriter.writeStartElement("track2");
            writeElement(xmlStreamWriter, "pan", emvModel.getPan());
            writeElement(xmlStreamWriter, "expiryMonth", emvModel.getExMonth());
            writeElement(xmlStreamWriter, "expiryYear", emvModel.getExpYear());
//            writeElement(xmlStreamWriter, "track2", String.format("%-38s", emvModel.getTrack2data()).replace(' ', '0'));
            writeElement(xmlStreamWriter, "track2",  emvModel.getTrack2data());
            writeElement(xmlStreamWriter, "serviceRestrictionCode", emvModel.getServiceCode());
            xmlStreamWriter.writeEndElement(); // </track2>
            writeElement(xmlStreamWriter, "wasFallback", "");
            xmlStreamWriter.writeEndElement(); // </cardData>

            writeElement(xmlStreamWriter, "fromAccount", "default");
            writeElement(xmlStreamWriter, "stan", common.goRundom(6));
            writeElement(xmlStreamWriter, "minorAmount", Amount);
            writeElement(xmlStreamWriter, "rate", "");
            writeElement(xmlStreamWriter, "settlementFee", "");
            writeElement(xmlStreamWriter, "settlementCurrencyCode", "");
            writeElement(xmlStreamWriter, "amountSettlement", "");
            writeElement(xmlStreamWriter, "surcharge", "");
            writeElement(xmlStreamWriter, "tmsConfiguredTerminalLocation", "");
            writeElement(xmlStreamWriter, "acquiringInstitutionId", "420400");
            writeElement(xmlStreamWriter, "terminalOwner", "420400");

            // Write <pinData>
            xmlStreamWriter.writeStartElement("pinData");
            writeElement(xmlStreamWriter, "ksn", cardModel.getKsn());
            writeElement(xmlStreamWriter, "ksnd", cardModel.getKsnd());
            writeElement(xmlStreamWriter, "pinBlock", cardModel.getPinBlock());
            writeElement(xmlStreamWriter, "pinType", cardModel.getPinType());
            xmlStreamWriter.writeEndElement(); // </pinData>

            writeElement(xmlStreamWriter, "keyLabel", cardModel.getKSNTag());
            writeElement(xmlStreamWriter, "reversalType", "Reservation");
            writeElement(xmlStreamWriter, "originalTransmissionDateTime", timeStamp.replace(" ","T"));
            writeElement(xmlStreamWriter, "originalAuthId", originalAuth);
            writeElement(xmlStreamWriter, "originalStan", originalStan);
            writeElement(xmlStreamWriter, "notDisposable", "false");

            xmlStreamWriter.writeEndElement(); // </purchaseRequest>

            xmlStreamWriter.writeEndDocument();

            out_data = stringWriter.toString().replace("<?xml version='1.0' encoding='UTF-8'?>", "");
            // Close the XMLStreamWriter
            xmlStreamWriter.close();
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
        return out_data;
    }

    private  void writeElement(XMLStreamWriter xmlStreamWriter, String name, String value) throws XMLStreamException {
        xmlStreamWriter.writeStartElement(name);
        xmlStreamWriter.writeCharacters(value);
        xmlStreamWriter.writeEndElement();
    }
}
