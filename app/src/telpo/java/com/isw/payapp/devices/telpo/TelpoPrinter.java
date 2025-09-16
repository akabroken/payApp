package com.isw.payapp.devices.telpo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.isw.payapp.R;
import com.isw.payapp.devices.interfaces.PosPrinter;
import com.isw.payapp.model.Receipt;
import com.isw.payapp.terminal.config.TerminalConfig;
import com.telpo.tps550.api.DeviceNotFoundException;
import com.telpo.tps550.api.DeviceNotOpenException;
import com.telpo.tps550.api.TelpoException;
import com.telpo.tps550.api.printer.UsbThermalPrinter;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TelpoPrinter implements PosPrinter {

    private  Context context;
    private UsbThermalPrinter usbThermalPrinter;
    public TelpoPrinter(Context context) {
        this.context = context;
    }

    @Override
    public void initializePrinter() {
        usbThermalPrinter = new UsbThermalPrinter(context);
    }

    @Override
    public void printText(String text) {
        initializePrinter();
        int st_check;
        try {
            st_check = usbThermalPrinter.checkStatus();
            //if(st_check !=thermalPrinter.STATUS_OK)
            //    thermalPrinter.stop();

            usbThermalPrinter.start(1);
            usbThermalPrinter.reset();
            usbThermalPrinter.setMonoSpace(true);
            usbThermalPrinter.setGray(7);
            usbThermalPrinter.setFontSize(2);
            usbThermalPrinter.setAlgin(usbThermalPrinter.ALGIN_LEFT);
            usbThermalPrinter.walkPaper(5);
            usbThermalPrinter.addString("--------------------------------");
            usbThermalPrinter.setAlgin(usbThermalPrinter.ALGIN_MIDDLE);
            usbThermalPrinter.addString(text);
            usbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            usbThermalPrinter.addString(" THANK YOU");
            usbThermalPrinter.printString();
            usbThermalPrinter.walkPaper(10);
            usbThermalPrinter.stop();
        } catch (DeviceNotFoundException dex) {
            Log.v("Error!!", dex.getStackTrace().toString());
        } catch (DeviceNotOpenException dex) {
            Log.v("Error!!", dex.getStackTrace().toString());
        } catch (TelpoException dex) {
            Log.v("Error!!", dex.getStackTrace().toString());
        } catch (Exception dex) {
            Log.v("Error!!", dex.getStackTrace().toString());
        }
    }

    @Override
    public void printReceipt(Receipt receipt) {
        try {
            usbThermalPrinter = new UsbThermalPrinter(context);
            TerminalConfig terminalConfig = new TerminalConfig();
            usbThermalPrinter.start(1);
            usbThermalPrinter.reset();
            usbThermalPrinter.setMonoSpace(true);
            usbThermalPrinter.setGray(7);
            usbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            Bitmap bitmap1 = BitmapFactory.decodeResource(context.getResources(), R.drawable.vcblogo);
//           Bitmap bitmap2 = ThumbnailUtils.extractThumbnail(bitmap1, 244, 116);
//            usbThermalPrinter.printLogo(bitmap2, true);
            // Calculate the scaled dimensions while maintaining the aspect ratio
            int targetWidth = 244; // Desired width for the printer
            int targetHeight = 116; // Desired height for the printer

// Calculate the aspect ratio of the original bitmap
            float originalWidth = bitmap1.getWidth();
            float originalHeight = bitmap1.getHeight();
            float aspectRatio = originalWidth / originalHeight;

// Adjust the dimensions to fit within the target size while preserving the aspect ratio
            int scaledWidth, scaledHeight;
            if (targetWidth / aspectRatio <= targetHeight) {
                // Scale based on width
                scaledWidth = targetWidth;
                scaledHeight = Math.round(targetWidth / aspectRatio);
            } else {
                // Scale based on height
                scaledHeight = targetHeight;
                scaledWidth = Math.round(targetHeight * aspectRatio);
            }

// Resize the bitmap using Bitmap.createScaledBitmap
            Bitmap bitmap2 = Bitmap.createScaledBitmap(bitmap1, scaledWidth, scaledHeight, true);
            usbThermalPrinter.printLogo(bitmap2, true);

            usbThermalPrinter.setTextSize(30);
            usbThermalPrinter.addString("PIN CHANGE\n");
            usbThermalPrinter.addString("CUSTOMER RECEIPT");
            usbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
            usbThermalPrinter.setTextSize(24);
            usbThermalPrinter.addString("MERCHANT NAME:  " + terminalConfig.loadTerminalDataFromJson(context, "__merchantloc"));
            usbThermalPrinter.addString("MERCHANT ID:    " + terminalConfig.loadTerminalDataFromJson(context, "__mid"));
            usbThermalPrinter.addString("TERMINAL ID:    " + terminalConfig.loadTerminalDataFromJson(context, "__tid"));
            int i = usbThermalPrinter.measureText("CARD NO :" + receipt.getTransactionData().getCardModel().getPan());
            int i1 = usbThermalPrinter.measureText(" ");
            int SpaceNumber = (384 - i) / i1;
            String spaceString = "";
            for (int j = 0; j < SpaceNumber; j++) {
                spaceString += " ";
            }
            usbThermalPrinter.addString("CARD NO:" + spaceString + receipt.getTransactionData().getCardModel().getPan());
            usbThermalPrinter.addString("TRANS TYPE:        "+receipt.getTransactionData().getPaymentApp());
            usbThermalPrinter.addString("RSP CODE:          "+ receipt.getTransactionData().getResponseCode());
            usbThermalPrinter.addString("   "+ receipt.getTransactionData().getResponseMsg());
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date curDate = new Date(System.currentTimeMillis());//Get current time
            String str = formatter.format(curDate);
            usbThermalPrinter.addString("DATE/TIME:   " + str);
            usbThermalPrinter.addString("STAN NO:     " + receipt.getTransactionData().getStan());
            usbThermalPrinter.addString("AUTH NO:     " + receipt.getTransactionData().getAuthCode());
            usbThermalPrinter.addString("REFER NO:    " + receipt.getTransactionData().getRefferanceNo());

            usbThermalPrinter.printString();
            usbThermalPrinter.walkPaper(20);

        } catch (TelpoException e) {
            e.printStackTrace();
        } finally {
            usbThermalPrinter.stop();
        }
    }
}
