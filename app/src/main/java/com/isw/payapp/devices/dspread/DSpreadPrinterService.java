package com.isw.payapp.devices.dspread;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.RemoteException;
import android.util.Log;

import com.action.printerservice.PrintStyle;
import com.action.printerservice.barcode.Barcode1D;
import com.action.printerservice.barcode.Barcode2D;
import com.dspread.print.device.PrinterDevice;
import com.dspread.print.device.PrinterInitListener;
import com.dspread.print.device.PrinterManager;
import com.dspread.print.device.bean.PrintLineStyle;
import com.dspread.print.util.TRACE;
import com.dspread.print.widget.PrintLine;
import com.isw.payapp.R;
import com.isw.payapp.devices.dspread.utils.DeviceUtils;
import com.isw.payapp.devices.dspread.utils.QRCodeUtil;
import com.isw.payapp.devices.interfaces.IPrinterProcessor;
import com.isw.payapp.model.Receipt;

public class DSpreadPrinterService implements IPrinterProcessor {

    private Context context;
    private PrinterDevice mPrinter;

    public static DSpreadPrinterService printerCommand;
    private boolean isInitialized = false;

    public DSpreadPrinterService(Context context) {
        this.context = context;
    }

    public static DSpreadPrinterService getInstance(){
        if(printerCommand == null){
            synchronized (DSpreadPrinterService.class) {
                if (printerCommand == null) {
                    printerCommand = new DSpreadPrinterService(getInstance().context);
                }
            }
        }
        return printerCommand;
    }

    public void setPrinter(PrinterDevice printer) {
        this.mPrinter = printer;
        this.isInitialized = (printer != null);
    }

    public PrinterDevice getmPrinter(){
        return this.mPrinter;
    }

    @Override
    public void initializePrinter() {
        try {
            mPrinter = PrinterManager.getInstance().getPrinter();
            if (mPrinter == null) {
                TRACE.e("Failed to get printer instance from PrinterManager");
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
       // mPrinter.close();
        if (mPrinter == null) {
            Log.i("PRINTER", "INIT====>");
            initializePrinter();
        }
        if (mPrinter == null || !isInitialized) {
            throw new RemoteException("Printer not initialized properly");
        }
    }

    @Override
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

    public Bitmap printQRcode(Context context, String align, String size, String content, String errorLevel) throws RemoteException {
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

    public Bitmap printBarCode(Context context, String align, String width, String height, String content, String speedLevel, String densityLevel, String symbology) throws RemoteException {
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

    public Bitmap printPicture(Context context) throws RemoteException {
        ensurePrinterInitialized();
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.interswitch01);

        PrintLineStyle printLineStyle = new PrintLineStyle();
        mPrinter.setFooter(20);
        printLineStyle.setAlign(PrintLine.CENTER);
        mPrinter.setPrintStyle(printLineStyle);
        mPrinter.printBitmap(context, bitmap);
        return bitmap;
    }

    public Bitmap printBitmap(Context context, Bitmap bitmap) throws RemoteException {
        ensurePrinterInitialized();
        PrintLineStyle printLineStyle = new PrintLineStyle();
        mPrinter.setFooter(50);
        printLineStyle.setAlign(PrintLine.CENTER);
        mPrinter.setPrintStyle(printLineStyle);
        mPrinter.printBitmap(context, bitmap);
        return bitmap;
    }

    @Override
    public void printReceipt(Receipt receipt) throws RemoteException {
        ensurePrinterInitialized();
        mPrinter.addPrintLintStyle(new PrintLineStyle(PrintStyle.FontStyle.BOLD, PrintLine.CENTER, 16));
        mPrinter.addText("-----");
        mPrinter.addText("POS Signing of purchase orders");
        mPrinter.addText("TELLER COPY");
        mPrinter.addPrintLintStyle(new PrintLineStyle(PrintStyle.FontStyle.NORMAL, PrintLine.CENTER, 14));
        mPrinter.addText("- - - - - - - - - - - - - -");
        mPrinter.addPrintLintStyle(new PrintLineStyle(PrintStyle.FontStyle.NORMAL, PrintLine.LEFT, 14));
        mPrinter.addText("ISSUER Agricultural Bank of China");
        mPrinter.addText("ACQ 48873110");
        mPrinter.addText("CARD number.");
        mPrinter.addPrintLintStyle(new PrintLineStyle(PrintStyle.FontStyle.NORMAL, PrintLine.LEFT, 14));
        mPrinter.addText("6228 48******8 116 S");
        mPrinter.addText("TYPE of transaction(TXN TYPE)");
        mPrinter.addText("SALE");
        mPrinter.addPrintLintStyle(new PrintLineStyle(PrintStyle.FontStyle.NORMAL, PrintLine.CENTER, 14));
        mPrinter.addText("- - - - - - - - - - - - - -");
        mPrinter.addTexts(new String[]{"BATCH NO", "000043"}, new int[]{5, 5}, new int[]{PrintStyle.Alignment.NORMAL, PrintStyle.Alignment.CENTER});
        mPrinter.addTexts(new String[]{"VOUCHER NO", "000509"}, new int[]{5, 5}, new int[]{PrintStyle.Alignment.NORMAL, PrintStyle.Alignment.CENTER});
        mPrinter.addTexts(new String[]{"AUTH NO", "000786"}, new int[]{5, 5}, new int[]{PrintStyle.Alignment.NORMAL, PrintStyle.Alignment.CENTER});
        mPrinter.addTexts(new String[]{"DATE/TIME", "2010/12/07 16:15:17"}, new int[]{5, 5}, new int[]{PrintStyle.Alignment.NORMAL, PrintStyle.Alignment.CENTER});
        mPrinter.addTexts(new String[]{"REF NO", "000001595276"}, new int[]{5, 5}, new int[]{PrintStyle.Alignment.NORMAL, PrintStyle.Alignment.CENTER});
        mPrinter.addTexts(new String[]{"2014/12/07 16:12:17", ""}, new int[]{5, 5}, new int[]{PrintStyle.Alignment.NORMAL, PrintStyle.Alignment.CENTER});
        mPrinter.addTexts(new String[]{"AMOUNT:", ""}, new int[]{5, 5}, new int[]{PrintStyle.Alignment.NORMAL, PrintStyle.Alignment.CENTER});
        mPrinter.addText("RMB:249.00");
        mPrinter.addPrintLintStyle(new PrintLineStyle(PrintStyle.FontStyle.NORMAL, PrintLine.CENTER, 12));
        mPrinter.addText("- - - - - - - - - - - - - -");
        mPrinter.addText("Please scan the QRCode for getting more information: ");
        mPrinter.addBarCode(context, Barcode1D.CODE_128.name(), 400, 100, "123456", PrintLine.CENTER);
        mPrinter.addText("Please scan the QRCode for getting more information:");
        mPrinter.addQRCode(300, Barcode2D.QR_CODE.name(), "123456", PrintLine.CENTER);
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

    public void close(){
        if (mPrinter != null) {
            mPrinter.close();
            isInitialized = false;
        }
    }

    public boolean isInitialized() {
        return isInitialized;
    }
}