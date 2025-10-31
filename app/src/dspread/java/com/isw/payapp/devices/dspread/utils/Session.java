package com.isw.payapp.devices.dspread.utils;

public class Session {
    private static String TMKKEY = null;
    private static String TPKKEY=null;

    private static boolean IS_KEY_RESET=false;
    public static String getTMKKEY() {
        return TMKKEY;
    }

    public static  void setTMKKEY(String str) {
        TMKKEY = str;
    }

    public static boolean isIsKeyReset() {
        return IS_KEY_RESET;
    }

    public static void setIsKeyReset(boolean isKeyReset) {
        IS_KEY_RESET = isKeyReset;
    }

    public static String getTPKKEY() {
        return TPKKEY;
    }

    public static void setTPKKEY(String TPKKEY) {
        Session.TPKKEY = TPKKEY;
    }
}
