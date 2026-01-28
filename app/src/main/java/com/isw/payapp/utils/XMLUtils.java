package com.isw.payapp.utils;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

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

    public static String getTransactionResult(String xmlResponse, String t) {
        if (xmlResponse == null || xmlResponse.trim().isEmpty()) {
            Log.w("XMLUtils", "Received null or empty XML response");
            return null;
        }

        // Try different parsing strategies based on response format
        try {
            // Strategy 1: Check if it's the new format first
            if (xmlResponse.contains("<response>") || xmlResponse.contains("<field39>")) {
                return parseNewXmlFormat(xmlResponse);
            }
            // Strategy 2: Check if it's the var format
            else if (xmlResponse.contains("<var")) {
                return parseVarXmlFormat(xmlResponse);
            }
            // Strategy 3: Try generic parsing
            else {
                return parseGenericXml(xmlResponse);
            }

        } catch (Exception e) {
            Log.e("XMLUtils", "Error parsing XML response", e);
            return "Error parsing response: " + e.getMessage();
        }
    }

    // Original method renamed for clarity
    private static String parseVarXmlFormat(String xmlResponse) throws Exception {
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
            return responseMessage == null ? responseMessage : "Transaction Approved";
        } else {
            // For failure, return the message
            return responseMessage != null ? responseMessage : "Transaction failed";
        }
    }

    // New method to handle the new XML format
    private static String parseNewXmlFormat(String xmlResponse) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xmlResponse)));

        String description = null;
        String field39 = null;
        String stan = null;

        // Parse description
        NodeList descNodes = doc.getElementsByTagName("description");
        if (descNodes.getLength() > 0) {
            description = descNodes.item(0).getTextContent().trim();
        }

        // Parse field39 (response code)
        NodeList field39Nodes = doc.getElementsByTagName("field39");
        if (field39Nodes.getLength() > 0) {
            field39 = field39Nodes.item(0).getTextContent().trim();
        }

        // Parse STAN
        NodeList stanNodes = doc.getElementsByTagName("stan");
        if (stanNodes.getLength() > 0) {
            stan = stanNodes.item(0).getTextContent().trim();
        }

        Log.d("XMLUtils", "New Format - Description: " + description +
                ", Field39: " + field39 + ", STAN: " + stan);

        // Check if successful (field39 might be "00" or empty for success)
        if ("00".equals(field39) || field39 == null || field39.isEmpty()) {
            return description != null ? description : "Transaction Approved";
        } else {
            // For specific error codes, you might want to map them to user-friendly messages
            String errorMessage = getErrorMessageFromCode(field39);
            if (errorMessage != null) {
                return errorMessage;
            }
            return description != null ? description : "Transaction failed with code: " + field39;
        }
    }

    // Method to handle generic XML parsing
    private static String parseGenericXml(String xmlResponse) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xmlResponse)));

        // Try to find common error/response elements
        Map<String, String> responseData = new HashMap<>();

        // Look for common tags
        String[] tagsToCheck = {"responseCode", "responsecode", "code", "field39",
                "responseMessage", "responsemessage", "message",
                "description", "error", "errorMessage"};

        for (String tag : tagsToCheck) {
            NodeList nodes = doc.getElementsByTagName(tag);
            if (nodes.getLength() > 0) {
                responseData.put(tag.toLowerCase(), nodes.item(0).getTextContent().trim());
            }
        }

        // Determine result based on found data
        String responseCode = responseData.get("responsecode") != null ?
                responseData.get("responsecode") : responseData.get("field39");

        String responseMessage = responseData.get("responsemessage") != null ?
                responseData.get("responsemessage") :
                (responseData.get("description") != null ?
                        responseData.get("description") : responseData.get("message"));

        Log.d("XMLUtils", "Generic parse - Code: " + responseCode + ", Message: " + responseMessage);

        if (responseCode == null && responseMessage == null) {
            return "Unknown response format";
        }

        if ("00".equals(responseCode) || responseCode == null) {
            return responseMessage != null ? responseMessage : "Transaction Approved";
        } else {
            String errorMessage = getErrorMessageFromCode(responseCode);
            if (errorMessage != null) {
                return errorMessage;
            }
            return responseMessage != null ? responseMessage : "Transaction failed";
        }
    }

    // Method to map error codes to user-friendly messages
    private static String getErrorMessageFromCode(String errorCode) {
        if (errorCode == null) return null;

        Map<String, String> errorCodeMap = new HashMap<>();
        errorCodeMap.put("01", "Refer to issuer");
        errorCodeMap.put("03", "Invalid merchant");
        errorCodeMap.put("04", "Pick-up card");
        errorCodeMap.put("05", "Do not honor");
        errorCodeMap.put("12", "Invalid transaction");
        errorCodeMap.put("13", "Invalid amount");
        errorCodeMap.put("14", "Invalid card number");
        errorCodeMap.put("15", "No such issuer");
        errorCodeMap.put("19", "Re-enter transaction");
        errorCodeMap.put("30", "Format error");
        errorCodeMap.put("41", "Lost card");
        errorCodeMap.put("43", "Stolen card");
        errorCodeMap.put("51", "Insufficient funds");
        errorCodeMap.put("54", "Expired card");
        errorCodeMap.put("55", "Incorrect PIN");
        errorCodeMap.put("57", "Transaction not permitted to cardholder");
        errorCodeMap.put("58", "Transaction not permitted on terminal");
        errorCodeMap.put("61", "Exceeds withdrawal amount limit");
        errorCodeMap.put("62", "Restricted card");
        errorCodeMap.put("65", "Exceeds withdrawal frequency limit");
        errorCodeMap.put("75", "Allowable number of PIN tries exceeded");
        errorCodeMap.put("76", "Invalid account");
        errorCodeMap.put("91", "Issuer or switch inoperative");
        errorCodeMap.put("94", "Duplicate transaction");
        errorCodeMap.put("96", "System malfunction");

        return errorCodeMap.get(errorCode);
    }

    // Method overload that accepts Document object directly
    public static String getTransactionResult(Document xmlDoc, String test) {
        if (xmlDoc == null) {
            return "Invalid XML document";
        }

        try {
            // Convert Document to String and use the main method
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(xmlDoc), new StreamResult(writer));
            return getTransactionResult(writer.toString(),null);
        } catch (Exception e) {
            Log.e("XMLUtils", "Error converting Document to String", e);
            return "Error processing response";
        }
    }

    // Method overload that accepts InputStream
    public static String getTransactionResult(InputStream xmlStream, String test) {
        if (xmlStream == null) {
            return "Invalid XML stream";
        }

        try {
            StringBuilder xmlString = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(xmlStream));
            String line;
            while ((line = reader.readLine()) != null) {
                xmlString.append(line);
            }
            reader.close();
            return getTransactionResult(xmlString.toString(),null);
        } catch (Exception e) {
            Log.e("XMLUtils", "Error reading XML from stream", e);
            return "Error reading response";
        }
    }

    // Method that returns structured result instead of just string
    public static TransactionResult getTransactionResultDetailed(String xmlResponse) {
        TransactionResult result = new TransactionResult();

        if (xmlResponse == null || xmlResponse.trim().isEmpty()) {
            result.setSuccess(false);
            result.setMessage("Empty response");
            return result;
        }

        try {
            String message = getTransactionResult(xmlResponse,null);
            result.setMessage(message);

            // Determine success based on message content
            boolean isSuccess = message != null &&
                    (message.contains("Approved") ||
                            message.contains("Success") ||
                            !message.contains("failed") &&
                                    !message.contains("Error") &&
                                    !message.contains("not permitted"));

            result.setSuccess(isSuccess);
            result.setRawResponse(xmlResponse);

            // Extract additional details
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(new InputSource(new StringReader(xmlResponse)));

                // Extract STAN if available
                NodeList stanNodes = doc.getElementsByTagName("stan");
                if (stanNodes.getLength() > 0) {
                    result.setStan(stanNodes.item(0).getTextContent().trim());
                }

                // Extract response code
                NodeList codeNodes = doc.getElementsByTagName("field39");
                if (codeNodes.getLength() > 0) {
                    result.setResponseCode(codeNodes.item(0).getTextContent().trim());
                }

            } catch (Exception e) {
                // Ignore extraction errors
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error parsing response: " + e.getMessage());
        }

        return result;
    }

    // Helper class for structured result
    public static class TransactionResult {
        private boolean success;
        private String message;
        private String rawResponse;
        private String responseCode;
        private String stan;

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getRawResponse() { return rawResponse; }
        public void setRawResponse(String rawResponse) { this.rawResponse = rawResponse; }

        public String getResponseCode() { return responseCode; }
        public void setResponseCode(String responseCode) { this.responseCode = responseCode; }

        public String getStan() { return stan; }
        public void setStan(String stan) { this.stan = stan; }

        @Override
        public String toString() {
            return "TransactionResult{" +
                    "success=" + success +
                    ", message='" + message + '\'' +
                    ", responseCode='" + responseCode + '\'' +
                    ", stan='" + stan + '\'' +
                    '}';
        }
    }

    // Original isErrorResponse method (assuming it exists)
    private static String isErrorResponse(String xmlResponse, String test, String test2 ) {
        // Your existing implementation
        // This should handle cases where no <var> tags are found
        return "Error: Invalid response format";
    }
}
