package com.isw.payapp.devices.dspread;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.RemoteException;
import android.util.Log;

import com.action.printerservice.PrintStyle;
import com.dspread.print.device.PrinterDevice;
import com.dspread.print.device.PrinterInitListener;
import com.dspread.print.device.PrinterManager;
import com.dspread.print.device.bean.PrintLineStyle;
import com.dspread.print.util.TRACE;
import com.dspread.print.widget.PrintLine;
import com.isw.payapp.R;
import com.isw.payapp.devices.dspread.utils.DeviceUtils;
import com.isw.payapp.devices.dspread.utils.QRCodeUtil;
import com.isw.payapp.model.Receipt;

public class DSpreadPrinterService {

    private static DSpreadPrinterService instance;
    private Context context;
    private PrinterDevice mPrinter;
    private boolean isInitialized = false;

    // Private constructor
    private DSpreadPrinterService(Context context) {
        this.context = context.getApplicationContext();
        initializePrinter();
    }

    public static synchronized DSpreadPrinterService getInstance(Context context) {
        if (instance == null) {
            instance = new DSpreadPrinterService(context);
        }
        return instance;
    }

    public void initializePrinter() {
        try {
            mPrinter = PrinterManager.getInstance().getPrinter();
            if (mPrinter == null) {
                TRACE.e("Failed to get printer instance from PrinterManager");
                isInitialized = false;
                return;
            }

            if ("D30".equalsIgnoreCase(Build.MODEL) || DeviceUtils.isAppInstalled(context, DeviceUtils.UART_AIDL_SERVICE_APP_PACKAGE_NAME)) {
                TRACE.i("init printer with callback==");
                mPrinter.initPrinter(context, new PrinterInitListener() {
                    @Override
                    public void connected() {
                        TRACE.i("init printer with callback success==");
                        mPrinter.setPrinterTerminatedState(PrinterDevice.PrintTerminationState.PRINT_STOP);
                        isInitialized = true;
                    }

                    @Override
                    public void disconnected() {
                        TRACE.e("Printer disconnected during initialization");
                        isInitialized = false;
                    }
                });
            } else {
                TRACE.i("init printer ==");
                mPrinter.initPrinter(context);
                isInitialized = true;
            }
        } catch (Exception e) {
            TRACE.e("Error initializing printer: " + e.getMessage());
            isInitialized = false;
        }
    }

    private void ensurePrinterInitialized() throws RemoteException {
        if (mPrinter == null || !isInitialized) {
            throw new RemoteException("Printer not initialized properly");
        }
    }

    public void printText(String text) throws RemoteException {
        ensurePrinterInitialized();
        mPrinter.printText(text);
    }

    public void printText(String alignText, String fontStyle, String textSize, String printContent) throws RemoteException {
        ensurePrinterInitialized();
        PrintLineStyle style = new PrintLineStyle();
        // Set alignment method
        switch (alignText) {
            case "LEFT":
                style.setAlign(PrintLine.LEFT);
                break;
            case "RIGHT":
                style.setAlign(PrintLine.RIGHT);
                break;
            case "CENTER":
                style.setAlign(PrintLine.CENTER);
                break;
        }

        // Set font style
        switch (fontStyle) {
            case "NORMAL":
                style.setFontStyle(PrintStyle.FontStyle.NORMAL);
                break;
            case "BOLD":
                style.setFontStyle(PrintStyle.FontStyle.BOLD);
                break;
            case "ITALIC":
                style.setFontStyle(PrintStyle.FontStyle.ITALIC);
                break;
            case "BOLD_ITALIC":
                style.setFontStyle(PrintStyle.FontStyle.BOLD_ITALIC);
                break;
        }

        style.setFontSize(Integer.parseInt(textSize));
        mPrinter.setPrintStyle(style);
        mPrinter.setFooter(30);
        mPrinter.printText(printContent);
    }

    public Bitmap printQRcode(String align, String size, String content, String errorLevel) throws RemoteException {
        ensurePrinterInitialized();
        PrintLineStyle style = new PrintLineStyle();
        int printLineAlign = PrintLine.CENTER;
        switch (align) {
            case "LEFT":
                printLineAlign = PrintLine.LEFT;
                break;
            case "RIGHT":
                printLineAlign = PrintLine.RIGHT;
                break;
        }

        int qrSize = Integer.parseInt(size);
        Bitmap bitmap = QRCodeUtil.getQrcodeBM(content, qrSize);

        mPrinter.setPrintStyle(style);
        mPrinter.setFooter(30);
        mPrinter.printQRCode(context, errorLevel, qrSize, content, printLineAlign);
        return bitmap;
    }

    public Bitmap printBarCode(String align, String width, String height, String content, String speedLevel, String densityLevel, String symbology) throws RemoteException {
        ensurePrinterInitialized();
        PrintLineStyle style = new PrintLineStyle();
        int printLineAlign = 0;
        switch (align) {
            case "LEFT":
                printLineAlign = PrintLine.LEFT;
                break;
            case "RIGHT":
                printLineAlign = PrintLine.RIGHT;
                break;
            case "CENTER":
                printLineAlign = PrintLine.CENTER;
                break;
        }

        Bitmap bitmap = QRCodeUtil.getBarCodeBM(content, Integer.parseInt(width), Integer.parseInt(height));
        if ("mp600".equals(Build.MODEL)) {
            mPrinter.setPrinterSpeed(Integer.parseInt(speedLevel));
            mPrinter.setPrinterDensity(Integer.parseInt(densityLevel));
        }
        mPrinter.setPrintStyle(style);
        mPrinter.setFooter(30);
        mPrinter.printBarCode(context, symbology, Integer.parseInt(width), Integer.parseInt(height), content, printLineAlign);
        return bitmap;
    }

    public Bitmap printPicture() throws RemoteException {
        ensurePrinterInitialized();
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.interswitch01);

        PrintLineStyle printLineStyle = new PrintLineStyle();
        mPrinter.setFooter(20);
        printLineStyle.setAlign(PrintLine.CENTER);
        mPrinter.setPrintStyle(printLineStyle);
        mPrinter.printBitmap(context, bitmap);
        return bitmap;
    }

    public Bitmap printBitmap(Bitmap bitmap) throws RemoteException {
        ensurePrinterInitialized();
        PrintLineStyle printLineStyle = new PrintLineStyle();
        mPrinter.setFooter(50);
        printLineStyle.setAlign(PrintLine.CENTER);
        mPrinter.setPrintStyle(printLineStyle);
        mPrinter.printBitmap(context, bitmap);
        return bitmap;
    }

    public void printReceipt(Receipt receipt) throws RemoteException {
        ensurePrinterInitialized();
        mPrinter.addPrintLintStyle(new PrintLineStyle(PrintStyle.FontStyle.BOLD, PrintLine.CENTER, 16));
        mPrinter.addText("-----");
        mPrinter.addText("Transaction Receipt");
        mPrinter.addText("TELLER COPY");
        mPrinter.addPrintLintStyle(new PrintLineStyle(PrintStyle.FontStyle.NORMAL, PrintLine.CENTER, 14));
        mPrinter.addText("- - - - - - - - - - - - - -");
        mPrinter.addPrintLintStyle(new PrintLineStyle(PrintStyle.FontStyle.NORMAL, PrintLine.LEFT, 14));
        mPrinter.addText("Bank:         " + receipt.getBank());
        mPrinter.addText("");
        mPrinter.addText("Merchant:     " + receipt.getMerchant());
        mPrinter.addText("Terminal ID:  " + receipt.getTerminalId());
        mPrinter.addText("");
        mPrinter.addText("Amount:       " + receipt.getAmount());
        mPrinter.addText("Currency      " + receipt.getCurrency());
        mPrinter.addText("Date:     " + receipt.getDateTime());
        mPrinter.addText("");
        mPrinter.addText("CARD number.");
        mPrinter.addPrintLintStyle(new PrintLineStyle(PrintStyle.FontStyle.NORMAL, PrintLine.LEFT, 14));
        mPrinter.addText(receipt.getCardNumber());
        mPrinter.addText("TYPE of transaction(TXN TYPE)");
        mPrinter.addText("" + receipt.getTransactionType());
        mPrinter.addText("");
        mPrinter.addText("AID:      " + receipt.getAid());
        mPrinter.addText("ATC:      " + receipt.getAtc());
        mPrinter.addText("TVR:      " + receipt.getTvr());
        mPrinter.addText("");
        mPrinter.addText("Response message");
        mPrinter.addText(receipt.getResponse());
        mPrinter.addText("");
        mPrinter.addText("");
        mPrinter.addPrintLintStyle(new PrintLineStyle(PrintStyle.FontStyle.NORMAL, PrintLine.CENTER, 14));
        mPrinter.addText("- - - - - - - - - - - - - -");
        mPrinter.addText("- - - - - - - - - - - - - -");
        mPrinter.setFooter(50);

        mPrinter.print(context);
    }

    public void getPrinterStatus() throws RemoteException {
        ensurePrinterInitialized();
        mPrinter.getPrinterStatus();
    }

    public void getPrinterDensity() throws RemoteException {
        ensurePrinterInitialized();
        mPrinter.getPrinterDensity();
    }

    public void getPrinterSpeed() throws RemoteException {
        ensurePrinterInitialized();
        mPrinter.getPrinterSpeed();
    }

    public void getPrinterTemperature() throws RemoteException {
        ensurePrinterInitialized();
        mPrinter.getPrinterTemperature();
    }

    public void getPrinterVoltage() throws RemoteException {
        ensurePrinterInitialized();
        mPrinter.getPrinterVoltage();
    }

    public void close() {
        if (mPrinter != null) {
            mPrinter.close();
            isInitialized = false;
        }
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public PrinterDevice getPrinter() {
        return mPrinter;
    }

    // Optional: Method to clear the instance (useful for testing)
    public static void clearInstance() {
        instance = null;
    }

    // Method to check if printer is available
    public static boolean isPrinterAvailable(Context context) {
        try {
            PrinterDevice printer = PrinterManager.getInstance().getPrinter();
            return printer != null;
        } catch (Exception e) {
            TRACE.e("Error checking printer availability: " + e.getMessage());
            return false;
        }
    }
}