package com.isw.payapp.devices.dspread.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class EMVTLVParser {

    /**
     * Extracts a specific EMV tag from a TLV hex string
     * @param tlvHexString The complete TLV data as hex string
     * @param targetTag The tag to extract (e.g., "9F03")
     * @return The value of the requested tag as hex string, or null if not found
     */
    public  String extractTag(String tlvHexString, String targetTag) {
        try {
            int index = 0;
            String hexData = tlvHexString.toUpperCase();

            while (index < hexData.length()) {
                // Parse the tag
                String tag = parseTag(hexData, index);
                index += tag.length();

                // Parse the length
                int[] lengthInfo = parseLength(hexData, index);
                int lengthBytes = lengthInfo[0];
                int valueLength = lengthInfo[1];
                index += lengthBytes * 2; // Each byte is 2 hex characters

                // Extract the value
                if (index + valueLength * 2 > hexData.length()) {
                    throw new IllegalArgumentException("Invalid TLV structure: value extends beyond data");
                }

                String value = hexData.substring(index, index + valueLength * 2);
                index += valueLength * 2;

                // Check if this is the target tag
                if (tag.equalsIgnoreCase(targetTag)) {
                    return value;
                }

                // If the tag indicates constructed data, recursively parse it
                if (isConstructedTag(tag)) {
                    String nestedValue = extractTag(value, targetTag);
                    if (nestedValue != null) {
                        return nestedValue;
                    }
                }
            }

            return null; // Tag not found

        } catch (Exception e) {
            throw new RuntimeException("Failed to extract tag: " + e.getMessage(), e);
        }
    }

    /**
     * Parses the tag from the TLV data
     */
    private  String parseTag(String hexData, int index) {
        if (index >= hexData.length()) {
            throw new IllegalArgumentException("Invalid TLV: incomplete tag");
        }

        // Get first byte of tag
        String firstByte = hexData.substring(index, index + 2);
        int firstByteValue = Integer.parseInt(firstByte, 16);

        // Check if tag is multi-byte (bit 5 of first byte = 1)
        if ((firstByteValue & 0x1F) == 0x1F) {
            // Multi-byte tag - continue until byte where bit 7 = 0
            int tagEnd = index + 2;
            while (tagEnd < hexData.length()) {
                String currentByte = hexData.substring(tagEnd, tagEnd + 2);
                int currentValue = Integer.parseInt(currentByte, 16);
                tagEnd += 2;

                // Check if this is the last byte of tag (bit 7 = 0)
                if ((currentValue & 0x80) == 0) {
                    break;
                }
            }
            return hexData.substring(index, tagEnd);
        } else {
            // Single byte tag
            return firstByte;
        }
    }

    /**
     * Parses the length from the TLV data
     * @return array [lengthBytes, valueLength]
     */
    private  int[] parseLength(String hexData, int index) {
        if (index >= hexData.length()) {
            throw new IllegalArgumentException("Invalid TLV: incomplete length");
        }

        String firstLengthByte = hexData.substring(index, index + 2);
        int firstByteValue = Integer.parseInt(firstLengthByte, 16);

        if (firstByteValue == 0x80) {
            throw new IllegalArgumentException("Indefinite length not supported in EMV");
        }

        if (firstByteValue <= 0x7F) {
            // Short form: single byte length
            return new int[]{1, firstByteValue};
        } else {
            // Long form: first byte indicates number of length bytes
            int lengthBytes = firstByteValue & 0x7F;
            if (index + 2 + lengthBytes * 2 > hexData.length()) {
                throw new IllegalArgumentException("Invalid TLV: incomplete length field");
            }

            // Parse multi-byte length
            String lengthHex = hexData.substring(index + 2, index + 2 + lengthBytes * 2);
            int valueLength = Integer.parseInt(lengthHex, 16);

            return new int[]{1 + lengthBytes, valueLength};
        }
    }

    /**
     * Checks if a tag indicates constructed data (bit 6 = 1)
     */
    private  boolean isConstructedTag(String tag) {
        // For multi-byte tags, check the first byte
        String firstByte = tag.length() > 2 ? tag.substring(0, 2) : tag;
        int firstByteValue = Integer.parseInt(firstByte, 16);
        return (firstByteValue & 0x20) != 0; // Bit 6 = 1 indicates constructed
    }

    /**
     * Extracts all tags from a TLV string and returns them as a map
     */
    public  Map<String, String> extractAllTags(String tlvHexString) {
        Map<String, String> tags = new HashMap<>();
        parseAllTags(tlvHexString.toUpperCase(), tags);
        return tags;
    }

    private  void parseAllTags(String hexData, Map<String, String> tags) {
        int index = 0;

        while (index < hexData.length()) {
            // Parse the tag
            String tag = parseTag(hexData, index);
            index += tag.length();

            // Parse the length
            int[] lengthInfo = parseLength(hexData, index);
            int lengthBytes = lengthInfo[0];
            int valueLength = lengthInfo[1];
            index += lengthBytes * 2;

            // Extract the value
            if (index + valueLength * 2 > hexData.length()) {
                throw new IllegalArgumentException("Invalid TLV structure");
            }

            String value = hexData.substring(index, index + valueLength * 2);
            index += valueLength * 2;

            // Store the tag and value
            tags.put(tag, value);

            // Recursively parse constructed tags
            if (isConstructedTag(tag)) {
                parseAllTags(value, tags);
            }
        }
    }

    /**
     * Pretty prints all tags with their descriptions
     */
    public  void printAllTags(String tlvHexString) {
        Map<String, String> tags = extractAllTags(tlvHexString);
        Map<String, String> tagDescriptions = getCommonEMVTagDescriptions();

        System.out.println("Found " + tags.size() + " tags:");
        System.out.println("Tag\tLength\tValue\tDescription");
        System.out.println("----\t------\t-----\t-----------");

        for (Map.Entry<String, String> entry : tags.entrySet()) {
            String tag = entry.getKey();
            String value = entry.getValue();
            String description = tagDescriptions.getOrDefault(tag, "Unknown tag");
            System.out.printf("%s\t%d\t%s...\t%s%n",
                    tag,
                    value.length() / 2,
                    value.substring(0, Math.min(20, value.length())),
                    description);
        }
    }

    /**
     * Common EMV tag descriptions
     */
    private  Map<String, String> getCommonEMVTagDescriptions() {
        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("9F03", "Amount, Other (Numeric)");
        descriptions.put("9F16", "Merchant Identifier");
        descriptions.put("9F4E", "Merchant Name and Location");
        descriptions.put("8E", "Cardholder Verification Method (CVM) List");
        descriptions.put("5F20", "Cardholder Name");
        descriptions.put("9F4C", "ICC Dynamic Number");
        descriptions.put("50", "Application Label");
        descriptions.put("9F06", "Application Identifier (AID) - Terminal");
        descriptions.put("9F21", "Transaction Time");
        descriptions.put("9F12", "Application Preferred Name");
        descriptions.put("9F11", "Issuer Code Table Index");
        descriptions.put("5F24", "Application Expiration Date");
        descriptions.put("5F28", "Issuer Country Code");
        descriptions.put("9F39", "Point-of-Service (POS) Entry Mode");
        descriptions.put("9B", "Transaction Status Information");
        descriptions.put("9F0D", "Issuer Action Code - Default");
        descriptions.put("9F0E", "Issuer Action Code - Denial");
        descriptions.put("9F0F", "Issuer Action Code - Online");
        descriptions.put("9F4C", "ICC Dynamic Number");
        descriptions.put("9F02", "Amount, Authorised (Numeric)");
        descriptions.put("9F03", "Amount, Other (Numeric)");
        descriptions.put("9F34", "Cardholder Verification Method (CVM) Results");
        descriptions.put("5F30", "Service Code");
        return descriptions;
    }

}
