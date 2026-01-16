package com.isw.payapp.utils;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class XMLUtils {

    public static String isErrorResponse(String xmlResponse) {
        if (xmlResponse == null || xmlResponse.trim().isEmpty()) {
            Log.w("XMLUtils", "Received null or empty XML response");
            return null;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Secure configuration
//            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
//            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
//            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
//            factory.setAttribute("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
//            factory.setNamespaceAware(true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlResponse)));

            NodeList labelNodes = doc.getElementsByTagName("label");
            for (int i = 0; i < labelNodes.getLength(); i++) {
                Node labelNode = labelNodes.item(i);
                if (labelNode != null && labelNode.getTextContent() != null) {
                    String text = labelNode.getTextContent().trim().toLowerCase();
                    Log.d("XMLUtils", "Checking text: " + text);
                        return labelNode.getTextContent().trim(); // Return original case
                }
            }

        } catch (Exception e) {
            Log.e("XMLUtils", "Error parsing XML response: " + xmlResponse, e);
        }

        return null; // No error label found
    }

    public static String isErrorResponse(String xmlResponse, String test) {
            String errorLabel = "";
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlResponse)));

            NodeList labelNodes = doc.getElementsByTagName("label");
            for (int i = 0; i < labelNodes.getLength(); i++) {
                Node labelNode = labelNodes.item(i);
                if (labelNode.getTextContent().toLowerCase().contains("error")) {
                    return labelNode.getTextContent();
                }
            }
        } catch (Exception e) {
            Log.e("TAG", "Error parsing XML response", e);
        }
        return null;
    }

    public static String getTransactionResult(String xmlResponse) {
        if (xmlResponse == null || xmlResponse.trim().isEmpty()) {
            Log.w("XMLUtils", "Received null or empty XML response");
            return null;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlResponse)));

            // Check response code first
            NodeList varNodes = doc.getElementsByTagName("var");
            if(varNodes.getLength() == 0){
                return isErrorResponse(xmlResponse);
            }
            String responseCode = null;
            String responseMessage = null;

            for (int i = 0; i < varNodes.getLength(); i++) {
                Node varNode = varNodes.item(i);
                if (varNode instanceof Element) {
                    Element varElement = (Element) varNode;
                    String name = varElement.getAttribute("name");

                    if ("responsecode".equals(name)) {
                        responseCode = varElement.getTextContent().trim();
                    } else if ("responsemessage".equals(name)) {
                        responseMessage = varElement.getTextContent().trim();
                    }
                }
            }

            Log.d("XMLUtils", "Response code: " + responseCode + ", Message: " + responseMessage);

            // If response code is "00", it's success
            if ("00".equals(responseCode)) {
               // return null; // Success - no error
                return responseMessage == null ? responseMessage : "Transaction Approved";
            } else {
                // For failure, return the message
                return responseMessage != null ? responseMessage : "Transaction failed";
            }

        } catch (Exception e) {
            Log.e("XMLUtils", "Error parsing XML response", e);
            return "Error parsing response";
        }
    }
}
