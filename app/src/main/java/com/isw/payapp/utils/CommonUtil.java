package com.isw.payapp.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class CommonUtil {

    public static String goRundom(int length){
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be greater than zero");
        }
        // Generate a random number with the specified length
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10)); // Generate random digit (0-9)
        }
        return sb.toString();
    }

    public static Map<String, String> convertXMLToMap(String xmlString) {
        Map<String, String> resultMap = new HashMap<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(xmlString.getBytes()));

            NodeList cardNodes = document.getElementsByTagName("card");
            if(cardNodes.getLength() ==0){
                throw new RuntimeException("Node list empty");
            }

            for (int i = 0; i < cardNodes.getLength(); i++) {
                Element cardElement = (Element) cardNodes.item(i);

                if ("CSetvars".equals(cardElement.getAttribute("name")) && "script".equals(cardElement.getAttribute("type"))) {
                    NodeList scriptNodes = cardElement.getElementsByTagName("script");
                    for (int j = 0; j < scriptNodes.getLength(); j++) {
                        Node scriptNode = scriptNodes.item(j);
                        if (scriptNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element scriptElement = (Element) scriptNode;
                            String scriptContent = scriptElement.getTextContent();

                            // Extract values from the script content
                            extractValuesFromScript(scriptContent, resultMap);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultMap;
    }

    private static void extractValuesFromScript(String scriptContent, Map<String, String> resultMap) {
        String[] lines = scriptContent.split(";");
        for (String line : lines) {
            if (line.contains("SessionAdd")) {
                // Extract variable name and value from SessionAdd calls
                String[] parts = line.split("'");
                if (parts.length >= 4) {
                    String variableName = parts[1];
                    String variableValue = parts[3];
                    resultMap.put(variableName, variableValue);
                }
            }
        }
    }
}
