package com.isw.payapp.model;

import com.isw.payapp.terminal.accessors.KeyModelAccessor;

import lombok.Data;


public class KeyModel implements KeyModelAccessor {

    private int id;
    private String keySetId;
    private String desKey;
    private int dukptIndex;
    private int dukptMkeyIndex;
    private String dukptbdk;
    private String dukptksn;
    private int mode;
    private String ipek;
    private String keyLabel;

    @Override
    public int id() {
        return id;
    }

    @Override
    public String getKeySetId() {
        return keySetId;
    }

    @Override
    public String getDesKey() {
        return desKey;
    }

    @Override
    public int getDukptIndex() {
        return dukptIndex;
    }

    @Override
    public int getDukptMkeyIndex() {
        return dukptMkeyIndex;
    }

    @Override
    public String getDukptbdk() {
        return dukptbdk;
    }

    @Override
    public String getDukptksn() {
        return dukptksn;
    }

    @Override
    public int getMode() {
        return mode;
    }

    @Override
    public String getIpek() {
        return ipek;
    }

    @Override
    public String getKeyLabel() {
        return keyLabel;
    }
}
