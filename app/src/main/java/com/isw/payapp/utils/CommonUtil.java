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

    /**
     * Extracts expiry date from track2 data
     * Track2 format: Primary Account Number + Separator (D or =) + Expiry Date (YYMM) + ...
     * @param track2Data The track2 data string
     * @return ExpiryDate object containing month and year, or null if not found
     */

    public static String[] extractExpiryFromTrack2(String track2Data) {
        if (track2Data == null || track2Data.length() < 12) { // Need at least PAN(16) + sep(1) + expiry(4) + service(3) = 24 chars
            return null;
        }

        // Find separator (D or =)
        int sepIndex = -1;
        for (int i = 0; i < track2Data.length(); i++) {
            char c = track2Data.charAt(i);
            if (c == 'D' || c == '=') {
                sepIndex = i;
                break;
            }
        }

        if (sepIndex == -1 || track2Data.length() <= sepIndex + 7) {
            // Need at least 7 characters after separator (YYMM + 3-digit service code)
            return null;
        }

        // Get YYMM + Service Code (7 digits after separator)
        String expiryAndService = track2Data.substring(sepIndex + 1, sepIndex + 8);

        // Validate it's 7 digits
        if (!expiryAndService.matches("\\d{7}")) {
            return null;
        }

        String year = expiryAndService.substring(0, 2);      // First 2 digits = YY (year)
        String month = expiryAndService.substring(2, 4);     // Next 2 digits = MM (month)
        String serviceCode = expiryAndService.substring(4, 7); // Last 3 digits = Service Code

        return new String[]{month, year, serviceCode};
    }

    public static ExpiryDate extractExpiryDate(String track2Data) {
        if (track2Data == null || track2Data.trim().isEmpty()) {
            return null;
        }

        // Find the separator position
        int separatorIndex = -1;
        for (int i = 0; i < track2Data.length(); i++) {
            char c = track2Data.charAt(i);
            if (c == 'D' || c == '=') {
                separatorIndex = i;
                break;
            }
        }

        if (separatorIndex == -1) {
            System.out.println("No separator (D or =) found in track2 data");
            return null;
        }

        // Check if there are enough characters after separator for YYMM (4 digits)
        if (track2Data.length() < separatorIndex + 5) {
            System.out.println("Track2 data too short after separator");
            return null;
        }

        try {
            // Extract the 4 characters after separator (YYMM)
            String expiryDigits = track2Data.substring(separatorIndex + 1, separatorIndex + 5);

            // Validate that these are digits
            if (!expiryDigits.matches("\\d{4}")) {
                System.out.println("Expiry digits are not valid: " + expiryDigits);
                return null;
            }

            // Parse year (first 2 digits) and month (last 2 digits)
            String yearStr = expiryDigits.substring(0, 2);  // YY
            String monthStr = expiryDigits.substring(2, 4); // MM

            int year = Integer.parseInt(yearStr);
            int month = Integer.parseInt(monthStr);

            // Validate month (1-12)
            if (month < 1 || month > 12) {
                System.out.println("Invalid month: " + month);
                return null;
            }

            // Validate year (00-99)
            if (year < 0 || year > 99) {
                System.out.println("Invalid year: " + year);
                return null;
            }

            return new ExpiryDate(month, year);

        } catch (NumberFormatException e) {
            System.out.println("Error parsing expiry digits: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.out.println("Unexpected error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Alternative method using regex for more robust parsing
     */
    public static ExpiryDate extractExpiryDateWithRegex(String track2Data) {
        if (track2Data == null || track2Data.trim().isEmpty()) {
            return null;
        }

        // Regex pattern: PAN digits + separator (D or =) + expiry (YYMM) + rest
        String pattern = "\\d+[D=](\\d{2})(\\d{2})";
        java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher matcher = regex.matcher(track2Data);

        if (matcher.find()) {
            try {
                String yearStr = matcher.group(1);  // YY
                String monthStr = matcher.group(2); // MM

                int year = Integer.parseInt(yearStr);
                int month = Integer.parseInt(monthStr);

                // Validate
                if (month >= 1 && month <= 12 && year >= 0 && year <= 99) {
                    return new ExpiryDate(month, year);
                }
            } catch (NumberFormatException e) {
                System.out.println("Error parsing expiry: " + e.getMessage());
            }
        }

        return null;
    }


    static class ExpiryDate {
        private final int month;
        private final int year;  // 2-digit year (YY)

        public ExpiryDate(int month, int year) {
            this.month = month;
            this.year = year;
        }

        public int getMonth() {
            return month;
        }

        public int getYear() {
            return year;
        }

        // Get 4-digit year (assuming 2000s)
        public int getFourDigitYear() {
            return 2000 + year;
        }

        // Get formatted month (with leading zero if needed)
        public String getFormattedMonth() {
            return String.format("%02d", month);
        }

        // Get formatted year (2-digit)
        public String getFormattedYear() {
            return String.format("%02d", year);
        }

        @Override
        public String toString() {
            return String.format("%02d/%02d", month, year);
        }
    }
}
