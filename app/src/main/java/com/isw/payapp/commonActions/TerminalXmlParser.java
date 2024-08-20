package com.isw.payapp.commonActions;

/*
*  Author Kennedy Amahaya
*
* */

import com.isw.payapp.utils.RSAUtil;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class TerminalXmlParser {

    private RSAUtil rsaUtil;
    public void buildXml(){

    }

    public void parseXml(){
        
    }

    public String TestKeyDownload( List<String>param) {
        String xmlKeyPayload ="";
        //List<String>getPKVal = new ArrayList<>();
        try {
           // getPKVal = rsaUtil.GetRsaEnc();
            // Create StringWriter and XMLStreamWriter
            StringWriter stringWriter = new StringWriter();
            XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
            XMLStreamWriter xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(stringWriter);

            // Start writing XML
            xmlStreamWriter.writeStartDocument();
            xmlStreamWriter.writeStartElement("kmsg");

            // Write elements inside <kmsg>
            writeElement(xmlStreamWriter, "scheme", "standard");
            writeElement(xmlStreamWriter, "app", "keydownload");

            // Write <terminfo>
            xmlStreamWriter.writeStartElement("terminfo");
            writeElement(xmlStreamWriter, "mid", "CBLKE0000000001");
            writeElement(xmlStreamWriter, "ttype", "POS");
            writeElement(xmlStreamWriter, "tmanu", "PAX");
            writeElement(xmlStreamWriter, "tid", "CBLKE001");
            writeElement(xmlStreamWriter, "uid", "5C809201");
            writeElement(xmlStreamWriter, "mloc", "CREDIT BANK");
            writeElement(xmlStreamWriter, "batt", "0");
            writeElement(xmlStreamWriter, "tim", "2023/10/03 09:34:11");
            writeElement(xmlStreamWriter, "csid", "SS:0");
            writeElement(xmlStreamWriter, "pstat", "67");
            writeElement(xmlStreamWriter, "lang", "EN");
            writeElement(xmlStreamWriter, "poscondcode", "00");
            writeElement(xmlStreamWriter, "posgeocode", "00255000000000834");
            writeElement(xmlStreamWriter, "currencycode", "404");
            writeElement(xmlStreamWriter, "tmodel", "Telpo");
            writeElement(xmlStreamWriter, "comms", "GPRS");
            writeElement(xmlStreamWriter, "cstat", "0");
            writeElement(xmlStreamWriter, "sversion", "kimono-v3.15.4");
            writeElement(xmlStreamWriter, "hasbattery", "1");
            writeElement(xmlStreamWriter, "lasttranstime", "2023/09/22 17:06:43");
            // ... add more elements as needed
            xmlStreamWriter.writeEndElement(); // </terminfo>

            // Write <request>
            xmlStreamWriter.writeStartElement("request");
            writeElement(xmlStreamWriter, "ttid", "000004");
            writeElement(xmlStreamWriter, "type", "trans");
            writeElement(xmlStreamWriter, "keysetid", "000006");
            writeElement(xmlStreamWriter, "hook", "C:s_keyhk.kxml");
            // ... add more elements as needed

            // Write <addinfo> inside <request>
            xmlStreamWriter.writeStartElement("addinfo");

            // Write <keyinfo> inside <addinfo>
            xmlStreamWriter.writeStartElement("keyinfo");
//            writeElement(xmlStreamWriter, "pkmod", "rdOJscsvOv9quO8OmW0sN99rR6faq7BVyaQQ3ttZAyXnREpLg43q9763ZQyEK7+esCAKU8ogiQM6MjCrg/N5dYVUuk6jucMDbQZXf5O3rGSdzl6Xq6gVeclW77YuBxYuFeLLLVyw6PF4WnxZ2vGGLq3IwhDvK757JpdKfUXV7TU=");
//            writeElement(xmlStreamWriter, "pkex", "AAEAAQ==");
            writeElement(xmlStreamWriter, "pkmod", param.get(0));
            writeElement(xmlStreamWriter, "pkex", param.get(1));
            writeElement(xmlStreamWriter, "der", "1");
            xmlStreamWriter.writeEndElement(); // </keyinfo>

            xmlStreamWriter.writeEndElement(); // </addinfo>

            xmlStreamWriter.writeEndElement(); // </request>

            xmlStreamWriter.writeEndElement(); // </kmsg>
            xmlStreamWriter.writeEndDocument();

            // Print the generated XML
           // System.out.println(stringWriter.toString());

            xmlKeyPayload = stringWriter.toString();
            // Close the XMLStreamWriter
            xmlStreamWriter.close();
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
        return xmlKeyPayload;
    }

    private  void writeElement(XMLStreamWriter xmlStreamWriter, String name, String value) throws XMLStreamException {
        xmlStreamWriter.writeStartElement(name);
        xmlStreamWriter.writeCharacters(value);
        xmlStreamWriter.writeEndElement();
    }
}
