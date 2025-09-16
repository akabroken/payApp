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
            if (i + length > data.length) throw new IllegalArgumentException("Truncated TLV value");
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
}
