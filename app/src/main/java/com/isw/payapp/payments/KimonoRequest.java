package com.isw.payapp.payments;

import com.isw.payapp.model.CardModel;
import com.isw.payapp.model.EmvModel;
import com.isw.payapp.model.PayData;

import java.io.StringWriter;
import java.text.SimpleDateFormat;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class KimonoRequest {
    private EmvModel emvModel;
    private PayData payData;
    private CardModel cardModel;
    private String Amount,auth,stan;

    String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());

    public KimonoRequest(EmvModel emvModel, PayData payData, CardModel cardModel) {
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
                    // Create a new Document
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document doc = builder.newDocument();

                    // Root element <kmsg>
                    Element kmsg = doc.createElement("kmsg");
                    doc.appendChild(kmsg);

                    // Child elements under <kmsg>
                    createElement(doc, kmsg, "scheme", "standard");
                    createElement(doc, kmsg, "app", "selectpin");

                    // <terminfo> element
                    Element terminfo = doc.createElement("terminfo");
                    kmsg.appendChild(terminfo);
                    createElement(doc, terminfo, "mid", payData.getMid());
                    createElement(doc, terminfo, "ttype", payData.getTtype());
                    createElement(doc, terminfo, "tmanu", payData.getTmanu());
                    createElement(doc, terminfo, "tid", payData.getTid());
                    createElement(doc, terminfo, "uid", payData.getUid());
                    createElement(doc, terminfo, "mloc", payData.getMloc());
                    createElement(doc, terminfo, "batt", payData.getBatt());
                    createElement(doc, terminfo, "tim", payData.getTim());
                    createElement(doc, terminfo, "csid", payData.getCsid());
                    createElement(doc, terminfo, "pstat", payData.getPstat());
                    createElement(doc, terminfo, "lang", payData.getLang());
                    createElement(doc, terminfo, "poscondcode", payData.getPoscondcode());
                    createElement(doc, terminfo, "posgeocode", payData.getPosgeocode());
                    createElement(doc, terminfo, "currencycode", payData.getCurrencycode());
                    createElement(doc, terminfo, "tmodel", payData.getTmodel());
                    createElement(doc, terminfo, "comms", payData.getComms());
                    createElement(doc, terminfo, "cstat", payData.getCstat());
                    createElement(doc, terminfo, "sversion", payData.getSversion());
                    createElement(doc, terminfo, "hasbattery", payData.getHasbattery());
                    createElement(doc, terminfo, "lasttranstime", payData.getLasttranstime());

                    // <request> element
                    Element request = doc.createElement("request");
                    kmsg.appendChild(request);
                    createElement(doc, request, "ttid", payData.getTtid());
                    createElement(doc, request, "type", payData.getType());
                    createElement(doc, request, "amt", "0.0");
                    createElement(doc, request, "hook", "C:selHook.kxml");
                    createElement(doc, request, "selacctype", "default");
                    createElement(doc, request, "track2", emvModel.getTrack2data());
                    createElement(doc, request, "pindata", cardModel.getPinBlock());
                    createElement(doc, request, "ksn", cardModel.getKsn());
                    createElement(doc, request, "ksnd", cardModel.getKsnd());
                    createElement(doc, request, "chvm", "OnlinePin");

                    // <icd> element
                    Element icd = doc.createElement("icd");
                    request.appendChild(icd);
                    createElement(doc, icd, "isfallback", "false");
                    createElement(doc, icd, "aa", "000000000000");
                    createElement(doc, icd, "ao", "000000000000");
                    createElement(doc, icd, "aip", emvModel.getApplicationInterchangeProfile());
                    createElement(doc, icd, "atc", emvModel.getAtc());
                    createElement(doc, icd, "cg", emvModel.getCryptogram());
                    createElement(doc, icd, "cid", emvModel.getCryptogramInformationData());
                    createElement(doc, icd, "cr", emvModel.getCvmResults());
                    createElement(doc, icd, "iad", emvModel.getIssuerApplicationData());
                    createElement(doc, icd, "trc", emvModel.getTransactionCurrencyCode().substring(1));
                    createElement(doc, icd, "tvr", emvModel.getTerminalVerificationResult());
                    createElement(doc, icd, "tcc", emvModel.getTerminalCountryCode().substring(1));
                    createElement(doc, icd, "tty", emvModel.getTerminalType());
                    createElement(doc, icd, "tck", "R");
                    createElement(doc, icd, "td", emvModel.getTransactionDate());
                    createElement(doc, icd, "trt", emvModel.getTransactionType());
                    createElement(doc, icd, "un", emvModel.getUnpredictableNumber());
                    createElement(doc, icd, "dfn", emvModel.getDedicatedFileName());
                    createElement(doc, icd, "tcp", emvModel.getTerminalCapabilities());

                    // Remaining elements under <request>
                    createElement(doc, request, "posdatacode", payData.getPosdatacode());
                    createElement(doc, request, "posentrymode", payData.getPosEntryMode());
                    createElement(doc, request, "cardseqnum", emvModel.getCarSeqNo());

                    // <addinfo> element
                    Element addinfoOuter = doc.createElement("addinfo");
                    request.appendChild(addinfoOuter);
                    Element addinfoInner = doc.createElement("addinfo");
                    addinfoOuter.appendChild(addinfoInner);
                    createElement(doc, addinfoInner, "tellerdetail", "networkid:"+payData.getMloc()+" Teller1::productcode:"+ payData.getTellerdetail());
                    createElement(doc, addinfoInner, "selacctype", "default");

                    // Convert Document to String
                    TransformerFactory transformerFactory = TransformerFactory.newInstance();
                    Transformer transformer = transformerFactory.newTransformer();
                    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                    DOMSource source = new DOMSource(doc);
                    StringWriter writer = new StringWriter();
                    StreamResult result = new StreamResult(writer);
                    transformer.transform(source, result);

                    // Print the generated XML
                    System.out.println(writer.toString());
                    out_data[0] = writer.toString();

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }).start();
        Thread.sleep(1000);
        return out_data[0];
    }

    // Helper method to create and append elements
    private static void createElement(Document doc, Element parent, String tagName, String value) {
        Element element = doc.createElement(tagName);
        element.appendChild(doc.createTextNode(value));
        parent.appendChild(element);
    }

}
