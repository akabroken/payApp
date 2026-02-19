package com.isw.payapp.devices.interfaces;


import android.content.Context;

import com.isw.payapp.model.Receipt;

public interface IPrinterProcessor {

//    getInstance(Context context);
    void initializePrinter();
    void printText(String text) throws Exception;
    void printReceipt(Receipt receipt) throws Exception;
}
