package com.isw.payapp.devices.interfaces;

public interface IPinPadProcessor {

    void initPinPad();
    int injectDukptKey(String key, String iKsn, String kcv);

    int resetKey();

    void deleteKey();

    int deleteKeys();

    void deviceClose();

}
