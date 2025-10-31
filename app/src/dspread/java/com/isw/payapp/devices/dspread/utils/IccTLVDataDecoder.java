package com.isw.payapp.devices.dspread.utils;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;
public class IccTLVDataDecoder {

    public static void main(String[] args) {
        String iccdata = "9F2608EF0AF159665ED1DB9F2701809F100706001203A0E8059F3704CFEBEFC79F3602008B950508400488009A032410139C01319F02060000010000005F2A020643820238009F1A0204849F03060000000000009F3303E0F8C89F34034203009F3501229F1E0830393238303437368407A00000000310109F090200969F4104000010279F120A566973612044656269744F07A00000000310105F280208009F4E0F313132323333343435353636373738";

        Map<String, String> result = decodeIccData(iccdata);
        System.out.println("Decoded map: " + result);
    }

    public static HashMap<String, String> decodeIccData2(String iccdata) {
        HashMap<String, String> tagsResponse = new HashMap<>();
        int index = 0;

        while (index < iccdata.length()) {
            String tag = iccdata.substring(index, index + 2);
            index += 2;

            if ((Integer.parseInt(tag, 16) & 0x1F) == 0x1F) {
                tag += iccdata.substring(index, index + 2);
                index += 2;
            }

            int length = Integer.parseInt(iccdata.substring(index, index + 2), 16);
            index += 2;

            String value = iccdata.substring(index, index + length * 2);
            index += length * 2;

            String description = EmvTLVTags.decodeTag(tag.toUpperCase());
            /*System.out.println("Tag: " + tag + " (" + description + ")");
            System.out.println("Length: " + length);
            System.out.println("Value: " + value);
            System.out.println();*/

            tagsResponse.put(description, value);
        }

        return tagsResponse;
    }


    private static final Set<String> CONSTRUCTED_TAGS = Set.of("70", "77", "A5", "6F");


    public static void decodeIccDataRecursive(String iccdata, int start, int end, Map<String, String> tagsResponse, String te) {
        int index = start;

        while (index < end) {
            // Validate we have at least 2 characters for tag
            if (index + 2 > end) {
                throw new IllegalArgumentException("Insufficient data for tag at index: " + index);
            }

            // Extract tag
            String tag = iccdata.substring(index, index + 2);
            index += 2;

            try {
                if ((Integer.parseInt(tag, 16) & 0x1F) == 0x1F) {
                    if (index + 2 > end) {
                        throw new IllegalArgumentException("Insufficient data for multi-byte tag at index: " + index);
                    }
                    tag += iccdata.substring(index, index + 2);
                    index += 2;
                }

                // Validate we have at least 2 characters for length
                if (index + 2 > end) {
                    throw new IllegalArgumentException("Insufficient data for length at index: " + index);
                }

                // Extract length (supporting extended length)
                int lengthByte = Integer.parseInt(iccdata.substring(index, index + 2), 16);
                index += 2;

                int length = parseLength(iccdata, index, end, lengthByte);

                // Update index based on extended length bytes consumed
                if ((lengthByte & 0x80) != 0) {
                    int numBytes = lengthByte & 0x7F;
                    index += numBytes * 2;
                }

                // Validate we have enough data for the value
                if (index + length * 2 > end) {
                    throw new IllegalArgumentException("Insufficient data for value of tag " + tag +
                            ", expected " + (length * 2) + " bytes, got " + (end - index));
                }

                // Extract value
                String value = iccdata.substring(index, index + length * 2);
                index += length * 2;

                String description = EmvTLVTags.decodeTag(tag);

                if (CONSTRUCTED_TAGS.contains(tag)) {
                    // Recursively decode inside
                    decodeIccDataRecursive(value, 0, value.length(), tagsResponse);
                } else {
                    tagsResponse.put(tag, value);
                }
            } catch (NumberFormatException e) {
                throw new NumberFormatException("Failed to parse TLV data at index " + index +
                        ", tag: " + tag + ", error: " + e.getMessage());
            }
        }
    }

    private static int parseLength(String iccdata, int index, int end, int lengthByte) {
        if ((lengthByte & 0x80) != 0) { // extended length
            int numBytes = lengthByte & 0x7F;

            // Validate numBytes
            if (numBytes > 4) {
                throw new IllegalArgumentException("Extended length too long: " + numBytes + " bytes");
            }

            if (index + numBytes * 2 > end) {
                throw new IllegalArgumentException("Insufficient data for extended length");
            }

            String lenHex = iccdata.substring(index, index + numBytes * 2);
            long longLength = Long.parseLong(lenHex, 16);

            if (longLength > Integer.MAX_VALUE || longLength < 0) {
                throw new IllegalArgumentException("Invalid length: " + longLength);
            }

            return (int) longLength;
        } else {
            return lengthByte;
        }
    }
    public static void decodeIccDataRecursive(String iccdata, int start, int end, Map<String, String> tagsResponse) {
        int index = start;

        while (index < end) {
            // Extract tag
            String tag = iccdata.substring(index, index + 2);
            index += 2;
            if ((Integer.parseInt(tag, 16) & 0x1F) == 0x1F) {
                tag += iccdata.substring(index, index + 2);
                index += 2;
            }

            // Extract length (supporting extended length)
            int lengthByte = Integer.parseInt(iccdata.substring(index, index + 2), 16);
            index += 2;
            int length;
            if ((lengthByte & 0x80) != 0) { // extended length
                int numBytes = lengthByte & 0x7F;
                String lenHex = iccdata.substring(index, index + numBytes * 2);
                length = Integer.parseInt(lenHex, 16);
                index += numBytes * 2;
            } else {
                length = lengthByte;
            }

            // Extract value
            String value = iccdata.substring(index, index + length * 2);
            index += length * 2;

            String description = EmvTLVTags.decodeTag(tag);

            if (CONSTRUCTED_TAGS.contains(tag)) {
                // Recursively decode inside
                decodeIccDataRecursive(value, 0, value.length(), tagsResponse);
            } else {
                /*System.out.println("Tag: " + tag + " (" + description + ")");
                System.out.println("Length: " + length);
                System.out.println("Value: " + value);*/
                tagsResponse.put(tag, value);
            }
        }
    }



    public static Map<String, String> decodeIccData(String iccdata) {
        Map<String, String> tagsResponse = new HashMap<>();
        decodeIccDataRecursive(iccdata, 0, iccdata.length(), tagsResponse);
        return tagsResponse;
    }
}
