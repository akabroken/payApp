package com.isw.payapp.terminal.accessors;

public interface KeyModelAccessor {

    int id();
    String getKeySetId();
    String getDesKey();
    int getDukptIndex();
    int getDukptMkeyIndex();
    String getDukptbdk();
    String getDukptksn();
    int getMode();
    String getIpek();
    String getKeyLabel();
}
