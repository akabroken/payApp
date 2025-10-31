package com.isw.payapp.devices.dspread.Activity.pinkeyboard;

import android.content.Context;

import com.dspread.xpos.QPOSService;
import com.dspread.xpos.Util;
import com.isw.payapp.devices.interfaces.IPosService;

public class DSpreadPinPadServiceImpl implements IPosService {
    private QPOSService qposService;
    private Context context;

    public DSpreadPinPadServiceImpl(Context context) {
       // this.qposService;
        this.context =context;
        initialize();
    }

    @Override
    public void initialize() {
        try {

            qposService = QPOSService.getInstance(context, QPOSService.CommunicationMode.UART);
            // Additional DSpread specific initialization
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getCvmKeyList() {
        if (qposService != null) {
            return qposService.getCvmKeyList();
        }
        return "";
    }

    @Override
    public String convertHexToString(String hexData) {
        return Util.convertHexToString(hexData);
    }

    @Override
    public String encryptPinData(String pinBlock, String cardNumber) {
        // DSpread specific PIN encryption logic
        if (qposService != null) {
            // Use DSpread encryption methods
            // return qposService.encryptPinData(pinBlock, cardNumber);
        }
        return pinBlock; // Fallback
    }

    @Override
    public boolean isDeviceConnected() {
        return qposService != null; // Add proper connection check
    }

    @Override
    public void processPinEntry(String pinData) {
        // DSpread specific PIN processing
        if (qposService != null) {
            // Process PIN using DSpread SDK
            // qposService.processPin(pinData);
        }
    }

    // DSpread specific methods
    public QPOSService getQposService() {
        return qposService;
    }
}
