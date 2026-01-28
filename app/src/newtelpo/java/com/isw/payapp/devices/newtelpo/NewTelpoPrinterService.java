package com.isw.payapp.devices.newtelpo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.common.apiutil.printer.UsbThermalPrinter;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.isw.payapp.BuildConfig;
import com.isw.payapp.R;
import com.isw.payapp.helpers.ConfigManager;
import com.isw.payapp.model.Receipt;
import com.isw.payapp.model.TerminalConfigModel;
import com.isw.payapp.terminal.config.TerminalConfig;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;

public class NewTelpoPrinterService {

    private final Context mContext;

    UsbThermalPrinter mUsbThermalPrinter;
    private OnTPRINTERSuccessListener monTPRINTERSuccessListener;

    public interface OnTPRINTERSuccessListener {
        void onTPRINTERSuccess(int data);
    }

    public NewTelpoPrinterService(Context mContext) {
        this.mContext = mContext;
    }



    public NewTelpoPrinterService(Context context,OnTPRINTERSuccessListener onTPRINTERSuccessListener) {
        this.mContext = context;
        this.monTPRINTERSuccessListener=onTPRINTERSuccessListener;
        mUsbThermalPrinter = new UsbThermalPrinter(mContext);
        new Thread(() -> {
            try {
                mUsbThermalPrinter.start(0);
                String version = mUsbThermalPrinter.getVersion();
                int status = mUsbThermalPrinter.checkStatus();

                if (monTPRINTERSuccessListener != null)
                    monTPRINTERSuccessListener.onTPRINTERSuccess(status);

                Log.d("printer version---",version);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void closePrinter(){
        try {
            if(mUsbThermalPrinter != null) mUsbThermalPrinter.stop();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void Print(Receipt receipt){
        new Thread(() -> {
            try{
                mUsbThermalPrinter.reset();
                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
                mUsbThermalPrinter.setTextSize(20);
                mUsbThermalPrinter.setGray(4);
                Bitmap barcode = CreateCode("12345678", BarcodeFormat.CODE_128, 320, 176);
                if (barcode != null) mUsbThermalPrinter.printLogo(barcode, true);
                Bitmap qrcode = CreateCode("12345678", BarcodeFormat.QR_CODE, 248, 248);
                if (qrcode != null) mUsbThermalPrinter.printLogo(qrcode, true);
                Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.interswitch);
                if(bitmap != null) mUsbThermalPrinter.printLogo(bitmap,true);
                mUsbThermalPrinter.addString("mContext.getString(R.string.printContent)");
                mUsbThermalPrinter.setTextSize(26);
                mUsbThermalPrinter.addString("mContext.getString(R.string.printContent1)");
                mUsbThermalPrinter.setTextSize(24);
                mUsbThermalPrinter.addString("mContext.getString(R.string.printContent1)");
                mUsbThermalPrinter.setTextSize(22);
                mUsbThermalPrinter.addString("mContext.getString(R.string.printContent1)");
                mUsbThermalPrinter.setTextSize(20);
                mUsbThermalPrinter.addString("mContext.getString(R.string.printContent1)");
                mUsbThermalPrinter.setTextSize(20);
                mUsbThermalPrinter.addString("mContext.getString(R.string.printContent2)");
                mUsbThermalPrinter.enlargeFontSize(1,2);
                mUsbThermalPrinter.addString("mContext.getString(R.string.printContent2)");
                mUsbThermalPrinter.enlargeFontSize(2,1);
                mUsbThermalPrinter.addString("mContext.getString(R.string.printContent2)");
                mUsbThermalPrinter.enlargeFontSize(2,2);
                mUsbThermalPrinter.addString("mContext.getString(R.string.printContent2)");
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.walkPaper(20);
            }catch (Exception e){
                e.printStackTrace();
            }
        }).start();
    }

    public void printReceipt(Receipt receipt) {
        new Thread(() -> {
        try {
          //  mUsbThermalPrinter = new UsbThermalPrinter(context);
            TerminalConfig terminalConfig = new TerminalConfig();
            ConfigManager.refreshConfig(mContext);
            TerminalConfigModel config = ConfigManager.getConfig(mContext);
            mUsbThermalPrinter.start(1);
            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setMonoSpace(true);
            mUsbThermalPrinter.setGray(7);
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            Bitmap bitmap1 = BitmapFactory.decodeResource(mContext.getResources(), BuildConfig.APP_LOGO);
//           Bitmap bitmap2 = ThumbnailUtils.extractThumbnail(bitmap1, 244, 116);
//            mUsbThermalPrinter.printLogo(bitmap2, true);
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
            mUsbThermalPrinter.printLogo(bitmap2, true);

            mUsbThermalPrinter.setTextSize(30);
            mUsbThermalPrinter.addString("PIN CHANGE\n");
            mUsbThermalPrinter.addString("CUSTOMER RECEIPT");
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
            mUsbThermalPrinter.setTextSize(24);
            mUsbThermalPrinter.addString("MERCHANT NAME:  " + config.getMerchantloc());
            mUsbThermalPrinter.addString("MERCHANT ID:    " + config.getMid());
            mUsbThermalPrinter.addString("TERMINAL ID:    " + config.getTid());
            int i = mUsbThermalPrinter.measureText("CARD NO :" + receipt.getCardNumber());
            int i1 = mUsbThermalPrinter.measureText(" ");
            int SpaceNumber = (384 - i) / i1;
            String spaceString = "";
            for (int j = 0; j < SpaceNumber; j++) {
                spaceString += " ";
            }
            mUsbThermalPrinter.addString("CARD NO:" + spaceString + receipt.getCardNumber());
            mUsbThermalPrinter.addString("TRANS TYPE:        "+receipt.getTransactionType());
            // mUsbThermalPrinter.addString("RSP CODE:          "+ receipt.getResponse());
            mUsbThermalPrinter.addString("   "+ receipt.getResponse());
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date curDate = new Date(System.currentTimeMillis());//Get current time
            String str = formatter.format(curDate);
            mUsbThermalPrinter.addString("DATE/TIME:   " + str);
            mUsbThermalPrinter.addString("Served by:" + receipt.getTeller());

//            mUsbThermalPrinter.addString("STAN NO:     " + receipt.getTransactionData().getStan());
//            mUsbThermalPrinter.addString("AUTH NO:     " + receipt.getTransactionData().getAuthCode());
//            mUsbThermalPrinter.addString("REFER NO:    " + receipt.getTransactionData().getRefferanceNo());

            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(20);


        }  catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        } finally {
            mUsbThermalPrinter.stop();
        }
        }).start();
    }

    public void printReceiptMerchant(Receipt receipt) {
        new Thread(() -> {
            try {
                //  mUsbThermalPrinter = new UsbThermalPrinter(context);
                TerminalConfig terminalConfig = new TerminalConfig();
                ConfigManager.refreshConfig(mContext);
                TerminalConfigModel config = ConfigManager.getConfig(mContext);
                mUsbThermalPrinter.start(1);
                mUsbThermalPrinter.reset();
                mUsbThermalPrinter.setMonoSpace(true);
                mUsbThermalPrinter.setGray(7);
                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
                Bitmap bitmap1 = BitmapFactory.decodeResource(mContext.getResources(), BuildConfig.APP_LOGO);
//           Bitmap bitmap2 = ThumbnailUtils.extractThumbnail(bitmap1, 244, 116);
//            mUsbThermalPrinter.printLogo(bitmap2, true);
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
                mUsbThermalPrinter.printLogo(bitmap2, true);

                mUsbThermalPrinter.setTextSize(30);
                mUsbThermalPrinter.addString("PIN CHANGE\n");
                mUsbThermalPrinter.addString("TELLER RECEIPT");
                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
                mUsbThermalPrinter.setTextSize(24);
                mUsbThermalPrinter.addString("MERCHANT NAME:  " + config.getMerchantloc());
                mUsbThermalPrinter.addString("MERCHANT ID:    " + config.getMid());
                mUsbThermalPrinter.addString("TERMINAL ID:    " + config.getTid());
                int i = mUsbThermalPrinter.measureText("CARD NO :" + receipt.getCardNumber());
                int i1 = mUsbThermalPrinter.measureText(" ");
                int SpaceNumber = (384 - i) / i1;
                String spaceString = "";
                for (int j = 0; j < SpaceNumber; j++) {
                    spaceString += " ";
                }
                mUsbThermalPrinter.addString("CARD NO:" + spaceString + receipt.getCardNumber());
                mUsbThermalPrinter.addString("TRANS TYPE:        "+receipt.getTransactionType());
                // mUsbThermalPrinter.addString("RSP CODE:          "+ receipt.getResponse());
                mUsbThermalPrinter.addString("   "+ receipt.getResponse());
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date curDate = new Date(System.currentTimeMillis());//Get current time
                String str = formatter.format(curDate);
                mUsbThermalPrinter.addString("DATE/TIME:   " + str);
                mUsbThermalPrinter.addString("Served by:      " + receipt.getTeller());

//            mUsbThermalPrinter.addString("STAN NO:     " + receipt.getTransactionData().getStan());
//            mUsbThermalPrinter.addString("AUTH NO:     " + receipt.getTransactionData().getAuthCode());
//            mUsbThermalPrinter.addString("REFER NO:    " + receipt.getTransactionData().getRefferanceNo());

                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.walkPaper(20);


            }  catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e.getMessage());
            } finally {
                mUsbThermalPrinter.stop();
            }
        }).start();
    }



    public Bitmap CreateCode(String str, BarcodeFormat type, int bmpWidth, int bmpHeight)
            throws WriterException {
        Hashtable<EncodeHintType, String> mHashtable = new Hashtable<EncodeHintType, String>();
        mHashtable.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        BitMatrix matrix = new MultiFormatWriter().encode(str, type, bmpWidth, bmpHeight, mHashtable);
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (matrix.get(x, y)) {
                    pixels[y * width + x] = 0xff000000;
                } else {
                    pixels[y * width + x] = 0xffffffff;
                }
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }
}
