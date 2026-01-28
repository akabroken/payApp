package com.isw.payapp.devices.newtelpo;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.common.sdk.emv.PinpadService;
import com.isw.payapp.devices.callbacks.EmvServiceCallback;
import com.isw.payapp.devices.interfaces.IEmvProcessor;
import com.isw.payapp.devices.newtelpo.EMV.EMVHandler;
import com.isw.payapp.devices.newtelpo.EMV.EMVUtilsConfigs;
import com.isw.payapp.model.TransactionData;
import com.isw.payapp.terminal.config.TerminalConfig;
import com.telpo.emv.EmvService;
import com.telpo.util.DefaultAPPCAPK;

import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NewTelpoEmvService implements IEmvProcessor {

    private static final String TAG = "NewTelpoEmvService";

    private final WeakReference<Activity> classActivityRef;
    private Context context;
    private TransactionData transData;
    private EmvServiceCallback classEmvCallBacks;
    private TerminalConfig terminalConfig;
    private CountDownLatch transactionLatch;
    private ExecutorService emvExecutor;

    private EmvService emvService;

    private EMVHandler emvHandler;
    private PinpadService pinpadService;
    public int _LastCode = 0;
    public boolean isDevInit = false;


    public NewTelpoEmvService(Activity classActivity, TransactionData transData, EmvServiceCallback classEmvCallBacks){
        this.classActivityRef = new WeakReference<>(classActivity);
        this.context = classActivity;
        this.transData = transData;
        this.classEmvCallBacks = classEmvCallBacks;
        this.terminalConfig = new TerminalConfig();
        this.transactionLatch = new CountDownLatch(1);
        this.emvExecutor = Executors.newSingleThreadExecutor();

    }
    @Override
    public void initializeDevice() throws Exception {

    }

    @Override
    public void initializeEmvService() throws Exception {

        //Init the EmvService
        emvService = EmvService.getInstance();
        Log.d(TAG,"Debug On:" + emvService.Emv_SetDebugOn(1));
        //     emvService.setListener(MyListener);

        if(EmvService.EMV_TRUE != (_LastCode = emvService.Open(context))) {
            Log.d(TAG,"EmvService.Open Fail:"+_LastCode);
            return;
        }
        Log.d(TAG,"EmvService.Open succ");

        if(EmvService.EMV_DEVICE_TRUE != (_LastCode = emvService.deviceOpen())) {
            Log.d(TAG,"EmvService.deviceOpen Fail:"+_LastCode);
            return;
        }
        Log.d(TAG,"EmvService.deviceOpen succ");

        Log.d(TAG,"EMV init ok!");
        emvService.Emv_RemoveAllApp();
        emvService.Emv_RemoveAllCapk();
        DefaultAPPCAPK.Add_All_APP(emvService);
        EMVUtilsConfigs.addAID(emvService,"A0000000043060");//tba
        EMVUtilsConfigs.addAID(emvService,"D86200010810B0");//tba
        EMVUtilsConfigs.addAID(emvService,"A0000000032010");//tba
        EMVUtilsConfigs.addAID(emvService,"A0000003710001");//tba
        EMVUtilsConfigs.addAmexAID(emvService);
        DefaultAPPCAPK.Add_All_CAPK(emvService);
        EMVUtilsConfigs.addAID(emvService,"A000000333010101");//tba
        Log.d(TAG,"Add Apps and Capks ok!");

        //Init the PinpadService
        _LastCode = InitPinPad();
        if(PinpadService.PIN_OK != _LastCode) {
            Log.d(TAG,"InitPinPad Fail:"+_LastCode);
            throw  new RuntimeException("InitPinPad Fail:"+_LastCode);
        }
        isDevInit = true;
        Log.d(TAG,"Dev init succ!!!");

    }

    @Override
    public void startEmvService() throws Exception {

        Log.i(TAG, "Teller names:"+ transData.getTellerdetail());
        emvHandler = new EMVHandler(context,transData, emvService, pinpadService, classEmvCallBacks);
        emvHandler.startCardDetection();

    }

    @Override
    public String getResponse() {
        return "";
    }

    @Override
    public void cancelTransaction() {
        emvHandler.stopCardDetection();
    }

    @Override
    public void setViews(EditText pinpadEditText, View scvText, View tvReceipt, View btnSendReceipt) {

    }

    public int InitPinPad()
    {
        pinpadService = new PinpadService(context);
        return pinpadService.Pinpad_Open(context);
    }
}
