package com.isw.payapp.devices.interfaces;

public interface IPosService {

    void initialize();
    String getCvmKeyList();
    String convertHexToString(String hexData);
    String encryptPinData(String pinBlock, String cardNumber);
    boolean isDeviceConnected();
    void processPinEntry(String pinData);
}
