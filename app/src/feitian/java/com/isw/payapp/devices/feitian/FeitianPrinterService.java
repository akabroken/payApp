package com.isw.payapp.devices.feitian;

import static com.ftpos.library.smartpos.errcode.ErrCode.ERR_SUCCESS;
import static com.ftpos.library.smartpos.printer.AlignStyle.PRINT_STYLE_CENTER;
import static com.ftpos.library.smartpos.printer.AlignStyle.PRINT_STYLE_LEFT;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.ftpos.library.smartpos.printer.OnPrinterCallback;
import com.ftpos.library.smartpos.printer.PrintStatus;
import com.ftpos.library.smartpos.printer.Printer;
import com.isw.payapp.BuildConfig;
import com.isw.payapp.devices.feitian.helpers.SvrHelper;
import com.isw.payapp.devices.interfaces.IEmvProcessor;
import com.isw.payapp.devices.interfaces.IPrinterProcessor;
import com.isw.payapp.helpers.ConfigManager;
import com.isw.payapp.model.Receipt;
import com.isw.payapp.model.TerminalConfigModel;
import com.jirui.logger.Logger;

public class FeitianPrinterService implements IPrinterProcessor {

    private final Context context;
    protected Printer printer;
    private static final String TAG = "FeitianPrinterService";

    public FeitianPrinterService(Context context){
        this.context = context;
    }


    @Override
    public void initializePrinter() {
        printer = SvrHelper.instance().getPrinter();
    }

    @Override
    public void printText(String text) throws Exception {

    }

    @Override
    public void printReceipt(Receipt tlvs) {
        try {
            int ret = printer.open();
            if (ret != ERR_SUCCESS) {
                Logger.i("Printer open failed: " + String.format(" errCode = 0x%x\n", ret));
                return;
            }

            ret = printer.startCaching();
            if (ret != ERR_SUCCESS) {
                Logger.i("Printer start caching failed: " + String.format(" errCode = 0x%x\n", ret));
                return;
            }

            ret = printer.setGray(3);
            if (ret != ERR_SUCCESS) {
                Logger.i("Printer set gray failed: " + String.format(" errCode = 0x%x\n", ret));
                return;
            }

            PrintStatus printStatus = new PrintStatus();
            ret = printer.getStatus(printStatus);
            if (ret != ERR_SUCCESS) {
                Logger.i("Printer get status failed: " + String.format(" errCode = 0x%x\n", ret));
                return;
            }

            Logger.i("Temperature = " + printStatus.getmTemperature() + ", Gray = " + printStatus.getmGray());
            if (!printStatus.getmIsHavePaper()) {
                Logger.i("Printer out of paper");
                return;
            }

            printReceiptContent(tlvs);

        } catch (Exception e) {
            Logger.i("Print failed: " + e.toString());
            Log.e(TAG, "printReceipt: ", e);
        }
    }

    private void printReceiptContent(Receipt tlvs) {

        ConfigManager.refreshConfig(context);
        TerminalConfigModel config = ConfigManager.getConfig(context);
        Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), BuildConfig.APP_LOGO);
        printer.printBmp(bmp);
        printer.setAlignStyle(PRINT_STYLE_CENTER);
        printer.printStr(tlvs.getTransactionType());
        printer.printStr(" Receipt\n");

        printer.setAlignStyle(PRINT_STYLE_LEFT);
        printer.printStr("Please retain this receipt.\n");
        printer.printStr("------------------------\n");
        printer.printStr("Bank: " + tlvs.getBank() + "\n");
        printer.printStr("Merchant: " + tlvs.getMerchant() + "\n");
        printer.printStr("Terminal ID: " + tlvs.getTerminalId() + "\n");
        printer.printStr("------------------------\n");
        printer.printStr("Card Name: " + tlvs.getCardHolderName() + "\n");
        printer.printStr("Card Number: " + tlvs.getCardNumber() + "\n");
        if(!tlvs.getAmount().isEmpty()){
            printer.printStr("Amount: " + tlvs.getAmount() + " " + tlvs.getCurrency() + "\n");
        }
        printer.printStr("------------------------\n");
        printer.printStr("Entry Mode: " + tlvs.getEntryMode() + "\n");
        printer.printStr("AID: " + tlvs.getAid() + "\n");
        printer.printStr("ATC: " + tlvs.getAtc() + "\n");
        printer.printStr("TVR: " + tlvs.getTvr() + "\n");
        printer.printStr("Stan: " + tlvs.getStan() + "\n");
        printer.printStr("AuthId: " + tlvs.getAuthId() + "\n");
        printer.printStr("RRN: " + tlvs.getReferenceNumber() + "\n");
        printer.printStr("------------------------\n");
        printer.printStr("Date/Time: " + tlvs.getDateTime() + "\n");
        printer.printStr("Transaction Type: " + tlvs.getTransactionType() + "\n");
        printer.printStr("------------------------\n");
        printer.printStr("Response: " + tlvs.getResponse() + "\n");
        printer.printStr("------------------------\n");
        printer.feed(1);
        printer.setAlignStyle(PRINT_STYLE_CENTER);
        printer.printStr("Thank you!\n");
        printer.setAlignStyle(PRINT_STYLE_LEFT);
        printer.printStr("Our contacts:"+config.getAddress1()+"\n");
        printer.printStr("Email:"+config.getAddress2() +"\n");
        printer.feed(1);


        int usedPaperLen = printer.getUsedPaperLenManage();
        if (usedPaperLen < 0) {
            Logger.i("Get used paper length failed");
        } else {
            Logger.i("UsedPaperLenManage = " + usedPaperLen + "mm");
        }

        printer.print(new OnPrinterCallback() {
            @Override
            public void onSuccess() {
                Logger.i("Print success");
                printer.feed(32);
            }

            @Override
            public void onError(int errorCode) {
                Logger.i("Print failed: " + String.format(" errCode = 0x%x\n", errorCode));
            }
        });
    }
}
