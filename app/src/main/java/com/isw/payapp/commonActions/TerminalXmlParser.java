package com.isw.payapp.commonActions;

/*
*  Author Kennedy Amahaya
*
* */

import android.content.Context;

import com.isw.payapp.terminal.config.TerminalConfig;
import com.isw.payapp.utils.RSAUtil;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TerminalXmlParser {

    private RSAUtil rsaUtil;
    public void buildXml(){

    }

    public void parseXml(){
        
    }

    public static String KeyDownload(Context context, List<Object>param) {
        String xmlKeyPayload ="";
        //List<String>getPKVal = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        String formattedDateTime = now.format(formatter);
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
            writeElement(xmlStreamWriter, "mid", TerminalConfig.loadTerminalDataFromJson(context,"__mid"));
            writeElement(xmlStreamWriter, "ttype", "POS");
            writeElement(xmlStreamWriter, "tmanu", "DSPREAD");
            writeElement(xmlStreamWriter, "tid", TerminalConfig.loadTerminalDataFromJson(context,"__tid"));
            writeElement(xmlStreamWriter, "uid", "5C809201");
            writeElement(xmlStreamWriter, "mloc", TerminalConfig.loadTerminalDataFromJson(context,"__merchantloc"));
            writeElement(xmlStreamWriter, "batt", "0");
            writeElement(xmlStreamWriter, "tim", formattedDateTime);
            writeElement(xmlStreamWriter, "csid", "SS:0");
            writeElement(xmlStreamWriter, "pstat", "67");
            writeElement(xmlStreamWriter, "lang", "EN");
            writeElement(xmlStreamWriter, "poscondcode", "00");
            writeElement(xmlStreamWriter, "posgeocode", "00254000000000404");
            writeElement(xmlStreamWriter, "currencycode", "404");
            writeElement(xmlStreamWriter, "tmodel", "DSPREAD");
            writeElement(xmlStreamWriter, "comms", "GPRS");
            writeElement(xmlStreamWriter, "cstat", "0");
            writeElement(xmlStreamWriter, "sversion", "kimono-v3.15.4");
            writeElement(xmlStreamWriter, "hasbattery", "1");
            writeElement(xmlStreamWriter, "lasttranstime", formattedDateTime);
            // ... add more elements as needed
            xmlStreamWriter.writeEndElement(); // </terminfo>

            // Write <request>
            xmlStreamWriter.writeStartElement("request");
            writeElement(xmlStreamWriter, "ttid", "000004");
            writeElement(xmlStreamWriter, "type", "trans");
            writeElement(xmlStreamWriter, "keysetid", "000002");
            writeElement(xmlStreamWriter, "hook", "C:s_keyhk.kxml");
            // ... add more elements as needed

            // Write <addinfo> inside <request>
            xmlStreamWriter.writeStartElement("addinfo");

            // Write <keyinfo> inside <addinfo>
            xmlStreamWriter.writeStartElement("keyinfo");
            writeElement(xmlStreamWriter, "pkmod", (String) param.get(0));
            writeElement(xmlStreamWriter, "pkex", (String) param.get(1));
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

    private static void writeElement(XMLStreamWriter xmlStreamWriter, String name, String value) throws XMLStreamException {
        xmlStreamWriter.writeStartElement(name);
        xmlStreamWriter.writeCharacters(value);
        xmlStreamWriter.writeEndElement();
    }
}
