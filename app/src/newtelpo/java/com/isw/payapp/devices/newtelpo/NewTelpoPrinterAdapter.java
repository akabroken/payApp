package com.isw.payapp.devices.newtelpo;

import android.content.Context;
import android.util.Log;

import com.isw.payapp.devices.interfaces.IPrinterProcessor;
import com.isw.payapp.model.Receipt;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class NewTelpoPrinterAdapter implements IPrinterProcessor {

    private static final String TAG = "NewTelpoPrinterAdapter";
    private static final int PRINTER_INIT_TIMEOUT = 5000; // 5 seconds timeout

    private final Context mContext;
    private NewTelpoPrinterService newTelpoPrinterService;
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    private final AtomicBoolean isInitializing = new AtomicBoolean(false);
    private int printerStatus = -1;

    public NewTelpoPrinterAdapter(Context mContext) {
        this.mContext = mContext.getApplicationContext();
    }

    @Override
    public void initializePrinter() {
        if (isInitialized.get() || isInitializing.get()) {
            Log.d(TAG, "Printer already initialized or initializing");
            return;
        }

        isInitializing.set(true);

        final CountDownLatch latch = new CountDownLatch(1);

        newTelpoPrinterService = new NewTelpoPrinterService(mContext, new NewTelpoPrinterService.OnTPRINTERSuccessListener() {
            @Override
            public void onTPRINTERSuccess(int data) {
                printerStatus = data;
                // Consider printer initialized if status is 0 (success)
                // Adjust this condition based on your actual status codes
                isInitialized.set(data == 0);
                isInitializing.set(false);
                latch.countDown();
                Log.d(TAG, "Printer initialization completed with status: " + data);
            }
        });

        // Wait for initialization to complete with timeout
        try {
            if (!latch.await(PRINTER_INIT_TIMEOUT, TimeUnit.MILLISECONDS)) {
                isInitializing.set(false);
                isInitialized.set(false);
                Log.e(TAG, "Printer initialization timeout");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            isInitializing.set(false);
            isInitialized.set(false);
            Log.e(TAG, "Printer initialization interrupted", e);
        }
    }

    @Override
    public void printText(String text) throws Exception {
        checkPrinterInitialized();

        // Since NewTelpoPrinterService doesn't have a direct printText method,
        // we need to create a minimal Receipt
        Receipt textReceipt = new Receipt();
        textReceipt.setTransactionType("TEXT");
        textReceipt.setResponse(text);
        // Set other necessary fields with default values

        newTelpoPrinterService.Print(textReceipt);
    }

    @Override
    public void printReceipt(Receipt receipt) throws Exception {
        checkPrinterInitialized();

        // Print customer receipt
        newTelpoPrinterService.printReceipt(receipt);
    }

    public void printMerchantReceipt(Receipt receipt) throws Exception {
        checkPrinterInitialized();

        // Print merchant receipt
        newTelpoPrinterService.printReceiptMerchant(receipt);
    }

    public void printBothReceipts(Receipt receipt) throws Exception {
        checkPrinterInitialized();

        // Print customer receipt first
        newTelpoPrinterService.printReceipt(receipt);

        // Small delay between prints if needed
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Print merchant receipt
        newTelpoPrinterService.printReceiptMerchant(receipt);
    }

    public void closePrinter() {
        if (newTelpoPrinterService != null) {
            newTelpoPrinterService.closePrinter();
        }
        isInitialized.set(false);
        isInitializing.set(false);
    }

    public int getPrinterStatus() {
        return printerStatus;
    }

    public boolean isPrinterReady() {
        return isInitialized.get() && newTelpoPrinterService != null;
    }

    private void checkPrinterInitialized() throws Exception {
        if (!isPrinterReady()) {
            throw new Exception("Printer not initialized or not ready. Call initializePrinter() first and ensure it completes successfully.");
        }
    }
}