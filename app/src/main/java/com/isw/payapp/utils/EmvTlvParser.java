package com.isw.payapp.utils;

import java.util.*;
import java.nio.charset.StandardCharsets;

public class EmvTlvParser {
    public static class Tlv {
        public final String tag;
        public final int length;
        public final byte[] value;
        public Tlv(String tag, int length, byte[] value) {
            this.tag = tag;
            this.length = length;
            this.value = value;
        }
        public String hexValue() {
            StringBuilder sb = new StringBuilder();
            for (byte b : value) sb.append(String.format("%02X", b));
            return sb.toString();
        }
        public String asciiValue() {
            // return readable ASCII when printable; otherwise return null
            boolean printable = true;
            for (byte b : value) {
                int v = b & 0xFF;
                if (v < 0x20 || v > 0x7E) { printable = false; break; }
            }
            return printable ? new String(value, StandardCharsets.US_ASCII) : null;
        }
        @Override public String toString() {
            String ascii = asciiValue();
            return String.format("Tag=%s Len=%d Hex=%s%s",
                    tag, length, hexValue(),
                    (ascii!=null ? " ASCII=\"" + ascii + "\"" : ""));
        }
    }

    public static List<Tlv> parse(byte[] data) {
        List<Tlv> out = new ArrayList<>();
        int i = 0;
        while (i < data.length) {
            // --- parse tag (BER-TLV rules) ---
            if (i >= data.length) break;
            int first = data[i++] & 0xFF;
            List<Byte> tagBytes = new ArrayList<>();
            tagBytes.add((byte)first);
            // if tag number in first byte is 0x1F => subsequent bytes (with continuation bit) belong to tag
            if ((first & 0x1F) == 0x1F) {
                while (i < data.length) {
                    int b = data[i++] & 0xFF;
                    tagBytes.add((byte)b);
                    // last tag byte has bit 8 == 0
                    if ((b & 0x80) == 0) break;
                }
            }
            // convert tag bytes to hex string
            StringBuilder tagSb = new StringBuilder();
            for (byte tb : tagBytes) tagSb.append(String.format("%02X", tb));
            String tag = tagSb.toString();

            // --- parse length ---
            if (i >= data.length) break;
            int lb = data[i++] & 0xFF;
            int length;
            if ((lb & 0x80) != 0) {
                int num = lb & 0x7F; // number of subsequent length bytes
                if (num == 0 || num > 4) throw new IllegalArgumentException("Unsupported length size: " + num);
                if (i + num > data.length) throw new IllegalArgumentException("Truncated TLV length");
                length = 0;
                for (int k = 0; k < num; ++k) {
                    length = (length << 8) | (data[i++] & 0xFF);
                }
            } else {
                length = lb;
            }

            // --- value ---
            if (i + length > data.length) {
                // Instead of throwing an error, use available data
                length = data.length - i;
            }
            byte[] value = Arrays.copyOfRange(data, i, i + length);
            i += length;

            out.add(new Tlv(tag, length, value));
        }
        return out;
    }

    // helper hex->bytes
    public static byte[] hexToBytes(String hex) {
        hex = hex.replaceAll("\\s+",""); // remove whitespace
        int len = hex.length();
        if ((len & 1) != 0) throw new IllegalArgumentException("Invalid hex length");
        byte[] out = new byte[len/2];
        for (int i=0;i<len;i+=2) out[i/2] = (byte) Integer.parseInt(hex.substring(i,i+2),16);
        return out;
    }

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

                // Extract the value - handle truncated data gracefully
                int availableLength = Math.min(valueLength * 2, hexData.length() - index);
                if (availableLength < 0) {
                    break; // No more data available
                }

                String value = hexData.substring(index, index + availableLength);
                index += availableLength;

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
            // Log the error but don't break the application
            System.err.println("Warning: Error extracting tag " + targetTag + ": " + e.getMessage());
            return null;
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
                // Instead of throwing error, try to read available bytes
                int availableBytes = (hexData.length() - index - 2) / 2;
                if (availableBytes <= 0) {
                    throw new IllegalArgumentException("Invalid TLV: incomplete length field");
                }
                lengthBytes = Math.min(lengthBytes, availableBytes);
            }

            // Parse multi-byte length
            String lengthHex = hexData.substring(index + 2, index + 2 + lengthBytes * 2);
            int valueLength;
            try {
                valueLength = Integer.parseInt(lengthHex, 16);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid length value: " + lengthHex);
            }

            return new int[]{1 + lengthBytes, valueLength};
        }
    }

    /**
     * Checks if a tag indicates constructed data (bit 6 = 1)
     */
    private  boolean isConstructedTag(String tag) {
        try {
            // For multi-byte tags, check the first byte
            String firstByte = tag.length() > 2 ? tag.substring(0, 2) : tag;
            int firstByteValue = Integer.parseInt(firstByte, 16);
            return (firstByteValue & 0x20) != 0; // Bit 6 = 1 indicates constructed
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Extracts all tags from a TLV string and returns them as a map
     */
    public  Map<String, String> extractAllTags(String tlvHexString) {
        Map<String, String> tags = new HashMap<>();
        try {
            parseAllTags(tlvHexString.toUpperCase(), tags, 0);
        } catch (Exception e) {
            // Log error but return whatever tags we could parse
            System.err.println("Warning: Error parsing some TLV data: " + e.getMessage());
        }
        return tags;
    }

    private  int parseAllTags(String hexData, Map<String, String> tags, int startIndex) {
        int index = startIndex;

        while (index < hexData.length()) {
            // Parse the tag
            String tag;
            try {
                tag = parseTag(hexData, index);
            } catch (Exception e) {
                break; // Cannot parse tag, stop processing
            }
            index += tag.length();

            // Parse the length
            int[] lengthInfo;
            try {
                lengthInfo = parseLength(hexData, index);
            } catch (Exception e) {
                break; // Cannot parse length, stop processing
            }
            int lengthBytes = lengthInfo[0];
            int valueLength = lengthInfo[1];
            index += lengthBytes * 2;

            // Extract the value - handle truncated data
            int availableValueLength = Math.min(valueLength * 2, hexData.length() - index);
            if (availableValueLength < 0) {
                break;
            }

            String value = hexData.substring(index, index + availableValueLength);
            index += availableValueLength;

            // Store the tag and value
            tags.put(tag, value);

            // Recursively parse constructed tags
            if (isConstructedTag(tag)) {
                try {
                    parseAllTags(value, tags, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw  new RuntimeException(e.getMessage());
                    // Continue with next tag even if nested parsing fails
                }
            }
        }

        return index;
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