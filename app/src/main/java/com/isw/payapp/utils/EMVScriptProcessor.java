package com.isw.payapp.utils;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class EMVScriptProcessor {

    private static final String TAG = "EMVScriptProcessor";

    private void configureApprovedTransaction(Document document) {
        // updateProgress("Finalizing transaction...");

        String script = getValue(document, "script");
        String st2 = getValue(document, "st2");
        String iad = getValue(document, "iad");
        String rc = getValue(document, "rc");

        String st1 = buildScriptData("71", script);
        st2 = buildScriptData("72", st2);

        Log.d(TAG, "st1: " + st1);
        Log.d(TAG, "st2: " + st2);
        Log.d(TAG, "script: " + script);
        Log.d(TAG, "iad: " + iad);
        Log.d(TAG, "rc: " + rc);
    }

    /**
     * Builds EMV TLV (Tag-Length-Value) data for script processing
     * @param tag EMV tag (e.g., "71", "72")
     * @param data Hexadecimal string data
     * @return TLV formatted string
     */
    public static String buildScriptData(String tag, String data) {
        if (data == null || data.isEmpty()) {
            return "";
        }

        // Clean the data (remove spaces, colons, etc.)
        String cleanData = cleanHexString(data);

        // Get length in bytes (2 hex chars = 1 byte)
        int dataLength = cleanData.length() / 2;

        // Format length as hexadecimal
        String lengthHex = String.format("%02X", dataLength);

        // Build TLV: Tag + Length + Value
        return tag + lengthHex + cleanData;
    }

    /**
     * Cleans a hexadecimal string by removing non-hex characters
     * @param hexString Input hexadecimal string
     * @return Clean hexadecimal string
     */
    public static String cleanHexString(String hexString) {
        if (hexString == null) return "";
        return hexString.replaceAll("[^0-9A-Fa-f]", "").toUpperCase();
    }

    /**
     * Parses EMV TLV data
     * @param tlvData TLV formatted string
     * @return List of TLV objects
     */
    public static List<TLVObject> parseTLV(String tlvData) {
        List<TLVObject> result = new ArrayList<>();
        String cleanData = cleanHexString(tlvData);

        int index = 0;
        while (index < cleanData.length()) {
            // Extract tag (1-2 bytes)
            String tag = cleanData.substring(index, index + 2);
            index += 2;

            // Check for multi-byte tag (if first byte is 0xDF or 0x9F)
            if ((tag.startsWith("9F") || tag.startsWith("DF")) && index + 2 <= cleanData.length()) {
                tag += cleanData.substring(index, index + 2);
                index += 2;
            }

            // Extract length
            if (index + 2 > cleanData.length()) break;
            String lengthHex = cleanData.substring(index, index + 2);
            int length = Integer.parseInt(lengthHex, 16);
            index += 2;

            // Check for multi-byte length (if first byte is 0x81, 0x82, etc.)
            if (lengthHex.equals("81")) {
                if (index + 2 > cleanData.length()) break;
                lengthHex = cleanData.substring(index, index + 2);
                length = Integer.parseInt(lengthHex, 16);
                index += 2;
            } else if (lengthHex.equals("82")) {
                if (index + 4 > cleanData.length()) break;
                lengthHex = cleanData.substring(index, index + 4);
                length = Integer.parseInt(lengthHex, 16);
                index += 4;
            }

            // Extract value
            if (index + (length * 2) > cleanData.length()) break;
            String value = cleanData.substring(index, index + (length * 2));
            index += length * 2;

            result.add(new TLVObject(tag, length, value));
        }

        return result;
    }

    /**
     * Generates IAD (Issuer Application Data) TLV
     * @param iadData IAD data
     * @return TLV formatted IAD
     */
    public static String generateIAD(String iadData) {
        if (iadData == null || iadData.isEmpty()) {
            return "";
        }

        String cleanData = cleanHexString(iadData);

        // IAD typically uses tag 0x9F10
        return buildScriptData("9F10", cleanData);
    }

    /**
     * Generates response code TLV
     * @param rc Response code (typically 2 hex digits)
     * @return TLV formatted response code (tag 0x9F4D or similar)
     */
    public static String generateResponseCode(String rc) {
        if (rc == null || rc.isEmpty()) {
            return "";
        }

        String cleanRC = cleanHexString(rc);

        // Response code might use different tags depending on context
        // Common tags: 0x9F4D (Log Entry), 0x9F08 (Application Version Number)
        return buildScriptData("9F4D", cleanRC);
    }

    /**
     * Generates complete script message with multiple TLVs
     * @param tlvs List of TLV strings
     * @return Concatenated TLV data
     */
    public static String generateScriptMessage(List<String> tlvs) {
        StringBuilder result = new StringBuilder();
        for (String tlv : tlvs) {
            if (tlv != null && !tlv.isEmpty()) {
                result.append(tlv);
            }
        }
        return result.toString();
    }

    /**
     * Validates if a string is valid TLV
     * @param tlvData TLV string to validate
     * @return true if valid TLV structure
     */
    public static boolean isValidTLV(String tlvData) {
        if (tlvData == null || tlvData.isEmpty()) {
            return false;
        }

        String cleanData = cleanHexString(tlvData);

        try {
            List<TLVObject> parsed = parseTLV(cleanData);
            return !parsed.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Helper method to get value from Document (placeholder)
     */
    private String getValue(Document document, String key) {
        // Implement your document parsing logic here
        return "";
    }

    /**
     * TLV Object representation
     */
    public static class TLVObject {
        private final String tag;
        private final int length;
        private final String value;

        public TLVObject(String tag, int length, String value) {
            this.tag = tag;
            this.length = length;
            this.value = value;
        }

        public String getTag() {
            return tag;
        }

        public int getLength() {
            return length;
        }

        public String getValue() {
            return value;
        }

        public String getTLV() {
            return tag + String.format("%02X", length) + value;
        }

        @Override
        public String toString() {
            return "Tag: " + tag + ", Length: " + length + ", Value: " + value;
        }
    }

    /**
     * Example usage
     */
    public static void main(String[] args) {
        // Example 1: Build script data
        String scriptData = "8503000000";
        String st1 = buildScriptData("71", scriptData);
        System.out.println("ST1 TLV: " + st1);

        // Example 2: Build issuer script data
        String issuerScript = "8603000000";
        String st2 = buildScriptData("72", issuerScript);
        System.out.println("ST2 TLV: " + st2);

        // Example 3: Generate IAD
        String iadData = "06011203A000000101";
        String iadTLV = generateIAD(iadData);
        System.out.println("IAD TLV: " + iadTLV);

        // Example 4: Parse TLV
        String testTLV = "71118503000000720A860300000000000000";
        List<TLVObject> parsed = parseTLV(testTLV);
        for (TLVObject tlv : parsed) {
            System.out.println("Parsed: " + tlv);
        }

        // Example 5: Generate complete script message
        List<String> scriptParts = new ArrayList<>();
        scriptParts.add(st1);
        scriptParts.add(st2);
        scriptParts.add(iadTLV);
        String completeScript = generateScriptMessage(scriptParts);
        System.out.println("Complete Script: " + completeScript);
    }
}

// Placeholder classes for your existing code
class Document {
    // Your document implementation
}

class Log {
    public static void d(String tag, String message) {
        System.out.println(tag + ": " + message);
    }
}
