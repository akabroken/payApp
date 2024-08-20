package com.isw.payapp.payments;

import com.isw.payapp.terminal.model.CardModel;
import com.isw.payapp.terminal.model.EmvModel;
import com.isw.payapp.terminal.model.PayData;
import com.isw.payapp.utils.CommonUtil;

import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class KmsgRequest {

    private EmvModel emvModel;
    private PayData payData;
    private CardModel cardModel;
    private String Amount, auth, stan;

    String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());

    public KmsgRequest(EmvModel emvModel, PayData payData, CardModel cardModel) {
        this.emvModel = emvModel;
        this.payData = payData;
        this.cardModel = cardModel;
    }

    public String Payload() throws InterruptedException {
        final String[] out_data = {null};
        int amt_int = (int) (Double.parseDouble(payData.getAmount()) * 100);
        Amount = Integer.toString(amt_int);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    CommonUtil common = new CommonUtil();
                    // getPKVal = rsaUtil.GetRsaEnc();
                    // Create StringWriter and XMLStreamWriter
                    StringWriter stringWriter = new StringWriter();
                    XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
                    XMLStreamWriter xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(stringWriter);

                    // Start writing XML
                    xmlStreamWriter.writeStartDocument();
                    xmlStreamWriter.writeStartElement(payData.getPaymentReqTag());

                    // Write elements inside <kmsg>
                    // writeElement(xmlStreamWriter, "scheme", "standard");
                    writeElement(xmlStreamWriter, "scheme", "standard");
                    writeElement(xmlStreamWriter, "app", payData.getPaymentApp());
                    //Terminal Information
                    // Write <terminalInformation>
                    xmlStreamWriter.writeStartElement("terminfo");
                    writeElement(xmlStreamWriter, "mid", "CBLKE0000000001");
                    writeElement(xmlStreamWriter, "ttype", "POS");
                    writeElement(xmlStreamWriter, "tmanu", "TELPO");
                    writeElement(xmlStreamWriter, "tid", "CBLKE001");
                    writeElement(xmlStreamWriter, "uid", "328-519-332");
                    writeElement(xmlStreamWriter, "mloc", "TEST ANDROID ISW");
                    writeElement(xmlStreamWriter, "batt", "100");
                    writeElement(xmlStreamWriter, "tim", timeStamp.replace(" ", "T"));
                    writeElement(xmlStreamWriter, "csid", "SS:0");
                    writeElement(xmlStreamWriter, "pstat", "1");
                    writeElement(xmlStreamWriter, "lang", "EN");
                    writeElement(xmlStreamWriter, "poscondcode", "00");
                    writeElement(xmlStreamWriter, "posgeocode", "00255000000000254");
                    writeElement(xmlStreamWriter, "currencycode", "404");
                    writeElement(xmlStreamWriter, "tmodel", "TELPO");
                    writeElement(xmlStreamWriter, "comms", "WIFI");
                    writeElement(xmlStreamWriter, "cstat", "1");
                    writeElement(xmlStreamWriter, "sversion", "kimonoKE_V1-v3.14.18");
                    writeElement(xmlStreamWriter, "hasbattery", "1");
                    writeElement(xmlStreamWriter, "lasttranstime", "2000/01/01 04:11:29");
                    xmlStreamWriter.writeEndElement();
                    // Write <Request>
                    xmlStreamWriter.writeStartElement("request");
                    writeElement(xmlStreamWriter, "ttid", cardModel.getKSNTag());
                    writeElement(xmlStreamWriter, "type", "trans");
                    writeElement(xmlStreamWriter, "amt", "0.0");
                    writeElement(xmlStreamWriter, "hook", "C:selHook.kxml");
                    writeElement(xmlStreamWriter, "selacctype", "default");
                    writeElement(xmlStreamWriter, "track2", emvModel.getTrack2data());
                    writeElement(xmlStreamWriter, "pindata", cardModel.getPinBlock());
                    writeElement(xmlStreamWriter, "ksn", cardModel.getKsn());
                    writeElement(xmlStreamWriter, "ksnd", cardModel.getKsnd());
                    writeElement(xmlStreamWriter, "chvm", "OnlinePin");

                    xmlStreamWriter.writeStartElement("icd");
                    writeElement(xmlStreamWriter, "isfallback", "false");
                    writeElement(xmlStreamWriter, "aa", emvModel.getAmountAuthorized());
                    writeElement(xmlStreamWriter, "ao", emvModel.getAmountOther());
                    writeElement(xmlStreamWriter, "aip", emvModel.getApplicationInterchangeProfile());
                    writeElement(xmlStreamWriter, "atc", emvModel.getAtc());
                    writeElement(xmlStreamWriter, "cg", emvModel.getCryptogram());
                    writeElement(xmlStreamWriter, "cid", emvModel.getCryptogramInformationData());
                    writeElement(xmlStreamWriter, "cr", emvModel.getCvmResults());
                    writeElement(xmlStreamWriter, "iad", emvModel.getIssuerApplicationData());
                    writeElement(xmlStreamWriter, "trc", emvModel.getTransactionCurrencyCode());
                    writeElement(xmlStreamWriter, "tvr", emvModel.getTerminalVerificationResult());
                    writeElement(xmlStreamWriter, "tcc", emvModel.getTerminalCountryCode());
                    writeElement(xmlStreamWriter, "tty", emvModel.getTerminalType());
                    writeElement(xmlStreamWriter, "tck", "R");
                    writeElement(xmlStreamWriter, "tcp", emvModel.getTerminalCapabilities());
                    writeElement(xmlStreamWriter, "td", emvModel.getTransactionDate());
                    writeElement(xmlStreamWriter, "trt", emvModel.getTransactionType());
                    writeElement(xmlStreamWriter, "un", emvModel.getUnpredictableNumber());
                    writeElement(xmlStreamWriter, "dfn", emvModel.getDedicatedFileName());
                    xmlStreamWriter.writeEndElement(); // </icd>
                    writeElement(xmlStreamWriter, "posdatacode", "510101513344101");
                    writeElement(xmlStreamWriter, "posentrymode", "051");
                    writeElement(xmlStreamWriter, "cardseqnum", emvModel.getCarSeqNo());
                    xmlStreamWriter.writeEndElement();

                    // Write <addinfo>
                    xmlStreamWriter.writeStartElement("addinfo");
                    xmlStreamWriter.writeStartElement("pinchangeinfo");
                    writeElement(xmlStreamWriter,"addinfo1","");
                    xmlStreamWriter.writeEndElement();
                    xmlStreamWriter.writeEndElement();

                    xmlStreamWriter.writeEndElement(); // </kmsg>

                    xmlStreamWriter.writeEndDocument();

                    out_data[0] = stringWriter.toString().replace("<?xml version='1.0' encoding='UTF-8'?>", "");
                    // Close the XMLStreamWriter
                    xmlStreamWriter.close();
                } catch (XMLStreamException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        Thread.sleep(1000);
        return out_data[0];
    }

    private void writeElement(XMLStreamWriter xmlStreamWriter, String name, String value) throws XMLStreamException {
        xmlStreamWriter.writeStartElement(name);
        xmlStreamWriter.writeCharacters(value);
        xmlStreamWriter.writeEndElement();
    }
}
