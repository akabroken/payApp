package com.isw.payapp.devices.dspread;

import android.content.Context;

import com.isw.payapp.devices.interfaces.IPrinterProcessor;
import com.isw.payapp.model.Receipt;

public class DSpreadPrinterAdapter implements IPrinterProcessor {

    private DSpreadPrinterService printerService;
    private Context context;

    public DSpreadPrinterAdapter(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void initializePrinter() {
        if (printerService == null) {
            printerService = DSpreadPrinterService.getInstance(context);
        }
        printerService.initializePrinter();
    }

    @Override
    public void printText(String text) throws Exception {
        try {
            printerService.printText(text);
        } catch (android.os.RemoteException e) {
            throw new Exception(e);
        }
    }

    @Override
    public void printReceipt(Receipt receipt) throws Exception {
        try {
            printerService.printReceipt(receipt);
        } catch (android.os.RemoteException e) {
            throw new Exception(e);
        }
    }

    // Optional: Add a method to check initialization status
    public boolean isInitialized() {
        return printerService != null && printerService.isInitialized();
    }

    // Optional: Add a method to get the underlying service
    public DSpreadPrinterService getPrinterService() {
        return printerService;
    }
}
