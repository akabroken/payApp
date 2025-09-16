package com.isw.payapp.devices.dspread.utils;

import android.annotation.SuppressLint;
import android.util.*;
public class GeneratePemFileUtil {
    @SuppressLint("NewApi")
    public static String generatePemFileText(final byte[] array) {
        int i = 0;
        final String[] splitString = splitString(Base64.encodeToString(array, 0), 64);
        final StringBuilder sb = new StringBuilder(512);
        sb.append("-----BEGIN PUBLIC KEY-----\n");
        while (i < splitString.length) {
            sb.append(splitString[i]);
            sb.append('\n');
            ++i;
        }
        sb.append("-----END PUBLIC KEY-----");
        return sb.toString();
    }

    public static String[] splitString(final String s, final int n) {
        return s.split("(?<=\\G.{" + n + "})");
    }
}
