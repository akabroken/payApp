package com.isw.payapp.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class IccTLVDataDecoder {

    private static final Set<String> CONSTRUCTED_TAGS = Set.of("70", "77", "A5", "6F");

    public static void main(String[] args) {
        String iccdata = "9F2608EF0AF159665ED1DB9F2701809F100706001203A0E8059F3704CFEBEFC79F3602008B950508400488009A032410139C01319F02060000010000005F2A020643820238009F1A0204849F03060000000000009F3303E0F8C89F34034203009F3501229F1E0830393238303437368407A00000000310109F090200969F4104000010279F120A566973612044656269744F07A00000000310105F280208009F4E0F313132323333343435353636373738";

        Map<String, String> result = decodeIccData(iccdata);
        System.out.println("Decoded map: " + result);
    }

    /**
     * Main entrypoint: decodes TLV string into a map of raw tag -> value.
     */
    public static Map<String, String> decodeIccData(String iccdata) {
        Map<String, String> tagsResponse = new HashMap<>();
        decodeIccDataRecursive(iccdata, 0, iccdata.length(), tagsResponse);
        return tagsResponse;
    }

    /**
     * Recursive TLV decoder with extended length and constructed tag handling.
     */
    public static void decodeIccDataRecursive(String iccdata, int start, int end, Map<String, String> tagsResponse) {
        int index = start;

        while (index < end) {
            // Avoid out-of-bounds
            if (index + 2 > end) break;

            String tag = iccdata.substring(index, index + 2);
            index += 2;

            // Multi-byte tag
            if ((Integer.parseInt(tag, 16) & 0x1F) == 0x1F && index + 2 <= end) {
                tag += iccdata.substring(index, index + 2);
                index += 2;
            }

            // Skip padding/null
            if (tag.equals("00")) {
                continue;
            }

            // Avoid out-of-bounds
            if (index + 2 > end) break;

            // Length
            int lengthByte = Integer.parseInt(iccdata.substring(index, index + 2), 16);
            index += 2;

            int length;
            if ((lengthByte & 0x80) != 0) { // extended length
                int numBytes = lengthByte & 0x7F;
                if (index + numBytes * 2 > end) break;
                String lenHex = iccdata.substring(index, index + numBytes * 2);
                length = Integer.parseInt(lenHex, 16);
                index += numBytes * 2;
            } else {
                length = lengthByte;
            }

            // Avoid out-of-bounds for value
            if (index + length * 2 > end) {
                System.out.println("Truncated TLV value for tag " + tag + ", skipping...");
                return;
            }

            String value = iccdata.substring(index, index + length * 2);
            index += length * 2;

            if (CONSTRUCTED_TAGS.contains(tag)) {
                // Recursively decode inside constructed tag
                decodeIccDataRecursive(value, 0, value.length(), tagsResponse);
            } else {
                String description = EmvTLVTags.decodeTag(tag);
                System.out.printf("Tag: %s (%s) = %s%n", tag, description, value);
                tagsResponse.put(tag, value); // key = raw tag
                // optionally also store description
                tagsResponse.put(description, value);
            }
        }
    }
}
