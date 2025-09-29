package com.isw.payapp.paymentsRequests;


import com.isw.payapp.model.CardModel;
import com.isw.payapp.model.EmvModel;
import com.isw.payapp.model.TransactionData;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class KsmgRequest {
    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private final EmvModel emvModel;
    private final TransactionData payData;
    private final CardModel cardModel;
    private final String timeStamp;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public KsmgRequest(EmvModel emvModel, TransactionData payData, CardModel cardModel) {
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

    public String generatePayload() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        // Create root element
        Element kmsg = doc.createElement("kmsg");
        doc.appendChild(kmsg);

        // Add basic elements
        createElement(doc, kmsg, "scheme", "standard");
        createElement(doc, kmsg, "app", "selectpin");

        // Add terminal information
        addTerminalInfo(doc, kmsg);

        // Add request data
        addRequestData(doc, kmsg);

        // Convert to XML string
        return transformDocumentToString(doc);
    }

    private void addTerminalInfo(Document doc, Element parent) {
        Element terminfo = doc.createElement("terminfo");
        parent.appendChild(terminfo);

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
    }

    private void addRequestData(Document doc, Element parent) {
        Element request = doc.createElement("request");
        parent.appendChild(request);

        // Basic request info
        createElement(doc, request, "ttid", payData.getTtid());
        createElement(doc, request, "type", payData.getType());
        createElement(doc, request, "amt", "0.0");
        createElement(doc, request, "hook", "C:selHook.kxml");
        createElement(doc, request, "selacctype", "default");
        createElement(doc, request, "track2", emvModel.getTrack2data());
        createElement(doc, request, "pindata", cardModel.getPinBlock()); //3C9F313A12DB7614
        createElement(doc, request, "ksn", cardModel.getKsn());
        createElement(doc, request, "ksnd", cardModel.getKsnd());
        createElement(doc, request, "chvm", "OnlinePin");

        // Add ICD data
        addIcdData(doc, request);

        // Additional request info
        createElement(doc, request, "posdatacode", payData.getPosdatacode());
        createElement(doc, request, "posentrymode", payData.getPosEntryMode());
        createElement(doc, request, "cardseqnum", emvModel.getCarSeqNo());

        // Add additional info
        addAdditionalInfo(doc, request);
    }

    private void addIcdData(Document doc, Element parent) {
        Element icd = doc.createElement("icd");
        parent.appendChild(icd);

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
    }

    private void addAdditionalInfo(Document doc, Element parent) {
        Element addinfoOuter = doc.createElement("addinfo");
        parent.appendChild(addinfoOuter);

        Element addinfoInner = doc.createElement("addinfo");
        addinfoOuter.appendChild(addinfoInner);

        createElement(doc, addinfoInner, "tellerdetail",
                "networkid:" + payData.getMloc() + " Teller1::productcode:" + payData.getTellerdetail());
        createElement(doc, addinfoInner, "selacctype", "default");
    }

    private String transformDocumentToString(Document doc) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        DOMSource source = new DOMSource(doc);
        StringWriter writer = new StringWriter();
        transformer.transform(source, new StreamResult(writer));

        return writer.toString();
    }

    private static void createElement(Document doc, Element parent, String tagName, String value) {
        if (value == null) value = "";
        Element element = doc.createElement(tagName);
        element.appendChild(doc.createTextNode(value));
        parent.appendChild(element);
    }
}