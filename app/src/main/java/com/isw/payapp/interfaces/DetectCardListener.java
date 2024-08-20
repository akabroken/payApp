package com.isw.payapp.interfaces;

public interface DetectCardListener {
    void openDevice();
    void deviceClose();
    int detectCard();
    int detectNFC();
    int detectCardKernel();
    byte[] getBooleanArray(byte b);
}
