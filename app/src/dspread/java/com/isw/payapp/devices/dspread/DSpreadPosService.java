package com.isw.payapp.devices.dspread;

import com.isw.payapp.devices.interfaces.IPosService;

import com.dspread.xpos.QPOSService;
import com.dspread.xpos.Util;

public class DSpreadPosService implements IPosService {
    private QPOSService qposService;

    @Override
    public void initialize() {
       // qposService = QPOSService.getInstance();
        // DSpread specific initialization
    }

    @Override
    public String getCvmKeyList() {
        return "";
    }

    @Override
    public String convertHexToString(String hexData) {
        return "";
    }

    @Override
    public String encryptPinData(String pinBlock, String cardNumber) {
        return "";
    }

    @Override
    public boolean isDeviceConnected() {
        return false;
    }

    @Override
    public void processPinEntry(String pinData) {

    }

   // @Override
    public void processPayment(double amount) {
        // DSpread specific payment processing
        //Util.processTransaction(amount);
    }

  //  @Override
    public String getDeviceInfo() {
        return "DSpread Device";
    }
}
