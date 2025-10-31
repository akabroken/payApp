package com.isw.payapp.devices.interfaces;


import com.isw.payapp.model.Receipt;

public interface IPrinterProcessor {

    void initializePrinter();
    void printText(String text) throws Exception;
    void printReceipt(Receipt receipt) throws Exception;
}
