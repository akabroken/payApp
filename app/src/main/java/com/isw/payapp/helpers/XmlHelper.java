package com.isw.payapp.helpers;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class XmlHelper {
    public static String getValueByTagNameLegacy(String xmlString, String tagName) throws Exception {
        Document document = parseXml(xmlString);
        NodeList nodeList = document.getElementsByTagName(tagName);
        return nodeList.getLength() > 0 ? nodeList.item(0).getTextContent() : null;
    }

    public static String getNestedValueLegacy(String xmlString, String parentTag, String childTag) throws Exception {
        Document document = parseXml(xmlString);
        NodeList parentNodes = document.getElementsByTagName(parentTag);

        for (int i = 0; i < parentNodes.getLength(); i++) {
            Node parentNode = parentNodes.item(i);
            if (parentNode.getNodeType() == Node.ELEMENT_NODE) {
                NodeList childNodes = ((Element) parentNode).getElementsByTagName(childTag);
                if (childNodes.getLength() > 0) {
                    return childNodes.item(0).getTextContent();
                }
            }
        }
        return null;
    }

    /**
     * Extracts text content of the first occurrence of a given tag name.
     */
    public static String getValueByTagName(String xmlString, String tagName) throws Exception {
        Document document = parseXml(xmlString);
        NodeList nodeList = document.getElementsByTagName(tagName);
        return nodeList.getLength() > 0 ? nodeList.item(0).getTextContent().trim() : null;
    }

    /**
     * Extracts text content of a child tag within a parent tag.
     */
    public static String getNestedValue(String xmlString, String parentTag, String childTag) throws Exception {
        Document document = parseXml(xmlString);
        NodeList parentNodes = document.getElementsByTagName(parentTag);

        for (int i = 0; i < parentNodes.getLength(); i++) {
            Node parentNode = parentNodes.item(i);
            if (parentNode.getNodeType() == Node.ELEMENT_NODE) {
                Element parentElement = (Element) parentNode;
                NodeList childNodes = parentElement.getElementsByTagName(childTag);
                if (childNodes.getLength() > 0) {
                    return childNodes.item(0).getTextContent().trim();
                }
            }
        }
        return null;
    }

    /**
     * Extracts attribute value from the first element matching the tag name.
     */
    public static String getAttributeValue(String xmlString, String tagName, String attributeName) throws Exception {
        Document document = parseXml(xmlString);
        NodeList nodeList = document.getElementsByTagName(tagName);

        if (nodeList.getLength() > 0 && nodeList.item(0).getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) nodeList.item(0);
            return element.getAttribute(attributeName);
        }
        return null;
    }

    /**
     * Extracts all attributes from the first element matching the tag name.
     */
    public static Map<String, String> getAllAttributes(String xmlString, String tagName) throws Exception {
        Document document = parseXml(xmlString);
        NodeList nodeList = document.getElementsByTagName(tagName);
        Map<String, String> attributes = new HashMap<>();

        if (nodeList.getLength() > 0 && nodeList.item(0).getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) nodeList.item(0);
            NamedNodeMap attrMap = element.getAttributes();

            for (int i = 0; i < attrMap.getLength(); i++) {
                Node attr = attrMap.item(i);
                attributes.put(attr.getNodeName(), attr.getNodeValue());
            }
        }
        return attributes;
    }

    /**
     * Extracts all <var> elements and returns them as a Map<name, value>.
     */
    public static Map<String, String> extractAllVars(String xmlString) throws Exception {
        Document document = parseXml(xmlString);
        NodeList varList = document.getElementsByTagName("var");
        Map<String, String> varsMap = new HashMap<>();

        for (int i = 0; i < varList.getLength(); i++) {
            Node node = varList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element varElement = (Element) node;
                String name = varElement.getAttribute("name");
                String value = varElement.getTextContent().trim();
                varsMap.put(name, value);
            }
        }
        return varsMap;
    }

    /**
     * Extracts all <card> elements and returns their attributes + script content.
     */
    public static Map<String, Object> extractCardInfo(String xmlString) throws Exception {
        Document document = parseXml(xmlString);
        NodeList cardList = document.getElementsByTagName("card");
        Map<String, Object> cardData = new HashMap<>();

        if (cardList.getLength() > 0) {
            Element card = (Element) cardList.item(0);

            // Extract card attributes
            NamedNodeMap attrs = card.getAttributes();
            Map<String, String> cardAttrs = new HashMap<>();
            for (int i = 0; i < attrs.getLength(); i++) {
                Node attr = attrs.item(i);
                cardAttrs.put(attr.getNodeName(), attr.getNodeValue());
            }
            cardData.put("attributes", cardAttrs);

            // Extract script info
            NodeList scriptList = card.getElementsByTagName("script");
            if (scriptList.getLength() > 0) {
                Element script = (Element) scriptList.item(0);
                Map<String, String> scriptMap = new HashMap<>();
                scriptMap.put("name", script.getAttribute("name"));
                scriptMap.put("content", script.getTextContent().trim());
                cardData.put("script", scriptMap);
            }
        }

        return cardData;
    }

    /**
     * Internal helper to parse XML string into DOM Document.
     */
    private static Document parseXml(String xmlString) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xmlString.getBytes()));
    }
}

