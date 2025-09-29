package com.isw.payapp.devices.dspread.utils;

public class TMKKey extends Poskeys {
    private String TMKKEY = "0123456789ABCDEFFEDCBA9876543210";

    //private String TMKKEY = "D8DE53632DE273D3EF3D2AA35253F2DC";

    public String getTMKKEY() {
        return this.TMKKEY;
    }

    public void setTMKKEY(String str) {
        this.TMKKEY = str;
    }
}

