package com.isw.payapp.commonActions;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.telpo.tps550.api.DeviceNotFoundException;
import com.telpo.tps550.api.DeviceNotOpenException;
import com.telpo.tps550.api.TelpoException;
import com.telpo.tps550.api.printer.UsbThermalPrinter;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PrinterAction {

    public void testPrinter(UsbThermalPrinter usbThermalPrinter) {
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
            usbThermalPrinter.addString("TEST PRINTER" );
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

    public void PrintIC(UsbThermalPrinter usbThermalPrinter,String cardNum, String Amount, String auth,String stan,String ref){
        try {
            usbThermalPrinter.start(1);
            usbThermalPrinter.reset();
            usbThermalPrinter.setMonoSpace(true);
            usbThermalPrinter.setGray(7);
            usbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
//                            Bitmap bitmap1= BitmapFactory.decodeResource(context.getResources(),R.mipmap.interswitch01);
//                            Bitmap bitmap2 = ThumbnailUtils.extractThumbnail(bitmap1, 244, 116);
//                            usbThermalPrinter.printLogo(bitmap2,true);

            usbThermalPrinter.setTextSize(30);
            usbThermalPrinter.addString("POS SALES SLIP\n");
            usbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
            usbThermalPrinter.setTextSize(24);
            usbThermalPrinter.addString("MERCHANT NAME:    VICTORIAL BANK ");
            usbThermalPrinter.addString("MERCHANT NO:      CBLKE0000000001");
            usbThermalPrinter.addString("TERMINAL NO:      CBLKE001");
            int i = usbThermalPrinter.measureText("CARD NO TEST:" + cardNum);
            int i1 = usbThermalPrinter.measureText(" ");
            int SpaceNumber = (384 - i) / i1;
            String spaceString = "";
            for (int j = 0; j < SpaceNumber; j++) {
                spaceString += " ";
            }
            usbThermalPrinter.addString("CARD NOUII:" + spaceString + cardNum);
            usbThermalPrinter.addString("TRANS TYPE:                GOODS");
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date curDate = new Date(System.currentTimeMillis());//Get current time
            String str = formatter.format(curDate);
            usbThermalPrinter.addString("DATE/TIME:   " + str);
            usbThermalPrinter.addString("STAN NO:             "+stan);
            usbThermalPrinter.addString("AUTH NO:             "+auth);
            usbThermalPrinter.addString("REFER NO:            "+ref);
//            i = usbThermalPrinter.measureText("AMOUNT:" + "KSH" + Amount);
//            i1 = usbThermalPrinter.measureText(" ");
//            SpaceNumber = (384 - i) / i1;
//            spaceString = "";
//            for (int j = 0; j < SpaceNumber; j++) {
//                spaceString += " ";
//            }
            usbThermalPrinter.addString("AMOUNT:      Ksh." + Amount);
            usbThermalPrinter.addString("CARD HOLDER SIGNATURE:");
           // usbThermalPrinter.printLogo(bitmap, true);
            usbThermalPrinter.printString();
            usbThermalPrinter.walkPaper(10);
        } catch (TelpoException e) {
            e.printStackTrace();
        } finally {
            usbThermalPrinter.stop();
        }
    }

    public void PrintMAG(UsbThermalPrinter usbThermalPrinter,String cardNum, String Amount){
        try {
            usbThermalPrinter.start(1);
            usbThermalPrinter.reset();
            usbThermalPrinter.setMonoSpace(true);
            usbThermalPrinter.setGray(7);
            usbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
//                            Bitmap bitmap1= BitmapFactory.decodeResource(context.getResources(),R.mipmap.interswitch01);
//                            Bitmap bitmap2 = ThumbnailUtils.extractThumbnail(bitmap1, 244, 116);
//                            usbThermalPrinter.printLogo(bitmap2,true);

            usbThermalPrinter.setTextSize(30);
            usbThermalPrinter.addString("POS SALES SLIP\n");
            usbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
            usbThermalPrinter.setTextSize(24);
            usbThermalPrinter.addString("MERCHANT NAME:             Telpo");
            usbThermalPrinter.addString("MERCHANT NO:                  01");
            usbThermalPrinter.addString("TERMINAL NO:                  02");
            int i = usbThermalPrinter.measureText("CARD NO MAG:" + cardNum);
            int i1 = usbThermalPrinter.measureText(" ");
            int SpaceNumber = (384 - i) / i1;
            String spaceString = "";
            for (int j = 0; j < SpaceNumber; j++) {
                spaceString += " ";
            }

            usbThermalPrinter.addString("CARD NO:" + spaceString + cardNum);
            usbThermalPrinter.addString("TRANS TYPE:                GOODS");
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date curDate = new Date(System.currentTimeMillis());//获取当前时间
            String str = formatter.format(curDate);
            usbThermalPrinter.addString("DATE/TIME:   " + str);
            usbThermalPrinter.addString("EXP DATE:             2019-12-30");
            usbThermalPrinter.addString("BATCH NO:             2019000168");
            usbThermalPrinter.addString("REFER NO:             2019001232");
            i = usbThermalPrinter.measureText("AMOUNT:" + "$" + Amount);
            i1 = usbThermalPrinter.measureText(" ");
            SpaceNumber = (384 - i) / i1;
            spaceString = "";
            for (int j = 0; j < SpaceNumber; j++) {
                spaceString += " ";
            }
            usbThermalPrinter.addString("AMOUNT:" + spaceString + "$" + Amount);
            usbThermalPrinter.addString("CARD HOLDER SIGNATURE:");
           // usbThermalPrinter.printLogo(bitmap, true);
            usbThermalPrinter.printString();
            usbThermalPrinter.walkPaper(10);
        } catch (TelpoException e) {
            e.printStackTrace();
        } finally {
            usbThermalPrinter.stop();
        }
    }

    public void PrindVISACLLS(UsbThermalPrinter usbThermalPrinter,String cardNum, String Amount){
        try {
            usbThermalPrinter.start(1);
            usbThermalPrinter.reset();
            usbThermalPrinter.setMonoSpace(true);
            usbThermalPrinter.setGray(7);
            usbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
//                                        Bitmap bitmap1= BitmapFactory.decodeResource(context.getResources(),R.mipmap.interswitch01);
//                                        Bitmap bitmap2 = ThumbnailUtils.extractThumbnail(bitmap1, 244, 116);
//                                        usbThermalPrinter.printLogo(bitmap2,true);

            usbThermalPrinter.setTextSize(30);
            usbThermalPrinter.addString("POS SALES SLIP\n");
            usbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
            usbThermalPrinter.setTextSize(24);
            usbThermalPrinter.addString("MERCHANT NAME:             Telpo");
            usbThermalPrinter.addString("MERCHANT NO:                  01");
            usbThermalPrinter.addString("TERMINAL NO:                  02");
            int i = usbThermalPrinter.measureText("CARD NO NFC1:" + cardNum);
            int i1 = usbThermalPrinter.measureText(" ");
            int SpaceNumber = (384 - i) / i1;
            String spaceString = "";
            for (int j = 0; j < SpaceNumber; j++) {
                spaceString += " ";
            }

            usbThermalPrinter.addString("CARD NO:" + spaceString + cardNum);
            usbThermalPrinter.addString("TRANS TYPE:                GOODS");
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date curDate = new Date(System.currentTimeMillis());//获取当前时间
            String str = formatter.format(curDate);
            usbThermalPrinter.addString("DATE/TIME:   " + str);
            usbThermalPrinter.addString("EXP DATE:             2019-12-30");
            usbThermalPrinter.addString("BATCH NO:             2019000168");
            usbThermalPrinter.addString("REFER NO:             2019001232");
            i = usbThermalPrinter.measureText("AMOUNT:" + "$" + Amount);
            i1 = usbThermalPrinter.measureText(" ");
            SpaceNumber = (384 - i) / i1;
            spaceString = "";
            for (int j = 0; j < SpaceNumber; j++) {
                spaceString += " ";
            }
            usbThermalPrinter.addString("AMOUNT:" + spaceString + "$" + Amount);
            usbThermalPrinter.addString("CARD HOLDER SIGNATURE:");
            //usbThermalPrinter.printLogo(bitmap, true);
            usbThermalPrinter.printString();
            usbThermalPrinter.walkPaper(10);
        } catch (TelpoException e) {
            e.printStackTrace();
        } finally {
            usbThermalPrinter.stop();
        }
    }

    public void PrintMCCLLS(UsbThermalPrinter usbThermalPrinter,String cardNum, String Amount){
        try {
            usbThermalPrinter.start(1);
            usbThermalPrinter.reset();
            usbThermalPrinter.setMonoSpace(true);
            usbThermalPrinter.setGray(7);
            usbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
//                                        Bitmap bitmap1= BitmapFactory.decodeResource(context.getResources(),R.mipmap.interswitch01);
//                                        Bitmap bitmap2 = ThumbnailUtils.extractThumbnail(bitmap1, 244, 116);
//                                        usbThermalPrinter.printLogo(bitmap2,true);

            usbThermalPrinter.setTextSize(30);
            usbThermalPrinter.addString("POS SALES SLIP\n");
            usbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
            usbThermalPrinter.setTextSize(24);
            usbThermalPrinter.addString("MERCHANT NAME:             Telpo");
            usbThermalPrinter.addString("MERCHANT NO:                  01");
            usbThermalPrinter.addString("TERMINAL NO:                  02");
            int i = usbThermalPrinter.measureText("CARD NO 555:" + cardNum);
            int i1 = usbThermalPrinter.measureText(" ");
            int SpaceNumber = (384 - i) / i1;
            String spaceString = "";
            for (int j = 0; j < SpaceNumber; j++) {
                spaceString += " ";
            }

            usbThermalPrinter.addString("CARD NO:" + spaceString + cardNum);
            usbThermalPrinter.addString("TRANS TYPE:                GOODS");
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date curDate = new Date(System.currentTimeMillis());//获取当前时间
            String str = formatter.format(curDate);
            usbThermalPrinter.addString("DATE/TIME:   " + str);
            usbThermalPrinter.addString("EXP DATE:             2019-12-30");
            usbThermalPrinter.addString("BATCH NO:             2019000168");
            usbThermalPrinter.addString("REFER NO:             2019001232");
            i = usbThermalPrinter.measureText("AMOUNT:" + "$" + Amount);
            i1 = usbThermalPrinter.measureText(" ");
            SpaceNumber = (384 - i) / i1;
            spaceString = "";
            for (int j = 0; j < SpaceNumber; j++) {
                spaceString += " ";
            }
            usbThermalPrinter.addString("AMOUNT:" + spaceString + "$" + Amount);
            usbThermalPrinter.addString("CARD HOLDER SIGNATURE:");
          //  usbThermalPrinter.printLogo(bitmap, true);
            usbThermalPrinter.printString();
            usbThermalPrinter.walkPaper(10);
        } catch (TelpoException e) {
            e.printStackTrace();
        } finally {
            usbThermalPrinter.stop();
        }
    }

    public void PrintUPI(UsbThermalPrinter usbThermalPrinter,String cardNum, String Amount){
        try {
            usbThermalPrinter.start(1);
            usbThermalPrinter.reset();
            usbThermalPrinter.setMonoSpace(true);
            usbThermalPrinter.setGray(7);
            usbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
//                                        Bitmap bitmap1= BitmapFactory.decodeResource(context.getResources(),R.mipmap.interswitch01);
//                                        Bitmap bitmap2 = ThumbnailUtils.extractThumbnail(bitmap1, 244, 116);
//                                        usbThermalPrinter.printLogo(bitmap2,true);

            usbThermalPrinter.setTextSize(30);
            usbThermalPrinter.addString("POS SALES SLIP\n");
            usbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
            usbThermalPrinter.setTextSize(24);
            usbThermalPrinter.addString("MERCHANT NAME:             Telpo");
            usbThermalPrinter.addString("MERCHANT NO:                  01");
            usbThermalPrinter.addString("TERMINAL NO:                  02");
            int i = usbThermalPrinter.measureText("CARD NO 666:" + cardNum);
            int i1 = usbThermalPrinter.measureText(" ");
            int SpaceNumber = (384 - i) / i1;
            String spaceString = "";
            for (int j = 0; j < SpaceNumber; j++) {
                spaceString += " ";
            }

            usbThermalPrinter.addString("CARD NO444:" + spaceString + cardNum);
            usbThermalPrinter.addString("TRANS TYPE:                GOODS");
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date curDate = new Date(System.currentTimeMillis());//获取当前时间
            String str = formatter.format(curDate);
            usbThermalPrinter.addString("DATE/TIME:   " + str);
            usbThermalPrinter.addString("EXP DATE:             2019-12-30");
            usbThermalPrinter.addString("BATCH NO:             2019000168");
            usbThermalPrinter.addString("REFER NO:             2019001232");
            i = usbThermalPrinter.measureText("AMOUNT:" + "$" + Amount);
            i1 = usbThermalPrinter.measureText(" ");
            SpaceNumber = (384 - i) / i1;
            spaceString = "";
            for (int j = 0; j < SpaceNumber; j++) {
                spaceString += " ";
            }
            usbThermalPrinter.addString("AMOUNT:" + spaceString + "$" + Amount);
            usbThermalPrinter.addString("CARD HOLDER SIGNATURE:");
           // usbThermalPrinter.printLogo(bitmap, true);
            usbThermalPrinter.printString();
            usbThermalPrinter.walkPaper(10);
        } catch (TelpoException e) {
            e.printStackTrace();
        } finally {
            usbThermalPrinter.stop();
        }
    }
}
