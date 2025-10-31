package com.isw.payapp.devices.dspread.utils;

public class DukptKeys extends Poskeys{


    public   static String trackipek ;//= "C4259D858624327B6D89047D86252006";//"CDC3DAEC46821398173A3D6B7CCCEF23";//"236601E25380FDEC974E8616DC1433AC"; "C4259D858624327B6D89047D86252006"
    public  static String emvipek ;//= "C4259D858624327B6D89047D86252006";//"236601E25380FDEC974E8616DC1433AC";
    public   static String pinipek ;//= "C4259D858624327B6D89047D86252006";//"236601E25380FDEC974E8616DC1433AC";
    public  static String trackksn ;// = "FFFF000002DDDDE00000";
    public  static String emvksn ;//= "FFFF000002DDDDE00000";
    public   static String pinksn;// = "FFFF000002DDDDE00000";
    public  static String tmk ;//="B30D16EAE5372C9457326464E62C5E61";// "CD97A54A646612C9E92F79F33EC89A8E" D8DE53632DE273D3EF3D2AA35253F2DC";//"E92628BFF599820E6AD70DB7C3B574AE";"B30D16EAE5372C9457326464E62C5E61"

    public DukptKeys(){

    }
    public DukptKeys(String tmk,String trackipek, String emvipek ,String pinipek,  String trackksn,
                     String emvksn , String pinksn){
        this.tmk = tmk;
        this.trackipek = trackipek;
        this.emvipek = emvipek;
        this.pinipek = pinipek;
        this.trackksn = trackksn;
        this.emvksn = emvksn;
        this.pinksn = pinksn;
    }


//    public   String trackipek = "CDC3DAEC468213986A491CEF9992AF00";
//    public   String emvipek = "CDC3DAEC468213986A491CEF9992AF00";
//    public   String pinipek = "CDC3DAEC468213986A491CEF9992AF00";
//    public   String trackksn = "FFFF000002DDDDE00000";
//    public   String emvksn = "FFFF000002DDDDE00000";
//    public   String pinksn = "FFFF000002DDDDE00000";
//    public   String tmk = "D8DE53632DE273D3EF3D2AA35253F2DC";
//EADA67BA8373765B2FCE9E9BD3D3CEE6
    // https://www.celersms.com/KCV.htm
    public String getTrackipek() {
        return trackipek;
    }

    public void setTrackipek(String trackipek) {
        this.trackipek = trackipek;
    }

    public String getEmvipek() {
        return emvipek;
    }

    public void setEmvipek(String emvipek) {
        this.emvipek = emvipek;
    }

    public String getPinipek() {
        return pinipek;
    }

    public void setPinipek(String pinipek) {
        this.pinipek = pinipek;
    }

    public String getTrackksn() {
        return trackksn;
    }

    public void setTrackksn(String trackksn) {
        this.trackksn = trackksn;
    }

    public String getEmvksn() {
        return emvksn;
    }

    public void setEmvksn(String emvksn) {
        this.emvksn = emvksn;
    }

    public String getPinksn() {
        return pinksn;
    }

    public void setPinksn(String pinksn) {
        this.pinksn = pinksn;
    }

    public String getTmk() {
        return tmk;
    }

    public void setTmk(String tmk) {
        this.tmk = tmk;
    }


}
