package com.isw.payapp.devices.dspread.utils;

public class QPOSSessionKeys {
    private String enDataCardKey;
    private String enKcvDataCardKey;
    private String enPinKcvKey;
    private String enPinKey;
    private String rsaReginLen;
    private String rsaReginString;

    public QPOSSessionKeys(final String enDataCardKey, final String enPinKcvKey, final String enPinKey, final String rsaReginLen, final String enKcvDataCardKey, final String rsaReginString) {
        this.enDataCardKey = enDataCardKey;
        this.enPinKcvKey = enPinKcvKey;
        this.enPinKey = enPinKey;
        this.rsaReginLen = rsaReginLen;
        this.enKcvDataCardKey = enKcvDataCardKey;
        this.rsaReginString = rsaReginString;
    }

    public String getEnDataCardKey() {
        return this.enDataCardKey;
    }

    public String getEnKcvDataCardKey() {
        return this.enKcvDataCardKey;
    }

    public String getEnPinKcvKey() {
        return this.enPinKcvKey;
    }

    public String getEnPinKey() {
        return this.enPinKey;
    }

    public String getRsaReginLen() {
        return this.rsaReginLen;
    }

    public String getRsaReginString() {
        return this.rsaReginString;
    }

    public void setEnDataCardKey(final String enDataCardKey) {
        this.enDataCardKey = enDataCardKey;
    }

    public void setEnKcvDataCardKey(final String enKcvDataCardKey) {
        this.enKcvDataCardKey = enKcvDataCardKey;
    }

    public void setEnPinKcvKey(final String enPinKcvKey) {
        this.enPinKcvKey = enPinKcvKey;
    }

    public void setEnPinKey(final String enPinKey) {
        this.enPinKey = enPinKey;
    }

    public void setRsaReginLen(final String rsaReginLen) {
        this.rsaReginLen = rsaReginLen;
    }

    public void setRsaReginString(final String rsaReginString) {
        this.rsaReginString = rsaReginString;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
