package com.isw.payapp.lmpl;

import android.content.Context;
import android.util.Log;

import com.isw.payapp.payments.KimonoRequest;
import com.isw.payapp.tasks.EmvTLVExtractor;
import com.isw.payapp.tasks.PinPadTasks;
import com.isw.payapp.terminal.model.CardModel;
import com.isw.payapp.terminal.model.EmvModel;
import com.isw.payapp.terminal.model.PayData;
import com.telpo.emv.EmvAmountData;
import com.telpo.emv.EmvCandidateApp;
import com.telpo.emv.EmvOnlineData;
import com.telpo.emv.EmvPinData;
import com.telpo.emv.EmvService;
import com.telpo.emv.EmvServiceListener;
import com.telpo.pinpad.PinpadService;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class IccCardReadImpl extends EmvServiceListener {
    private final Context context;
    private final EmvService emvService;
    private PinPadTasks param;
    private final PayData payData;
    private final int event;
    private boolean bUIThreadisRunning;
    private int mResult, ret;
    public static int Mag = 0;
    public static int IC = 1;
    public static int Nfc = 2;
    private CardModel cardModel;
    private String kimonoData;
    private EmvTLVExtractor emvTLVExtractor;
    private KimonoRequest getPurchaseData;
    private String pan;

    public IccCardReadImpl(Context context, EmvService emvService, PayData payData, int event) {
        this.context = context;
        this.emvService = emvService;
        this.payData = payData;
        this.event = event;
    }


    @Override
    public int onInputAmount(EmvAmountData emvAmountData) {
        long amt = (long) (Double.parseDouble(payData.getAmount()) * 100);
        emvAmountData.Amount = amt;
        emvAmountData.TransCurrCode = (short) 404;//rupay834   //156 personal coins
        emvAmountData.ReferCurrCode = (short) 404;//rupay834    //156 personal coins
        emvAmountData.TransCurrExp = (byte) 2;
        emvAmountData.ReferCurrExp = (byte) 2;
        emvAmountData.ReferCurrCon = 0;
        emvAmountData.CashbackAmount = 0;
        return EmvService.EMV_TRUE;
    }

    @Override
    public int onInputPin(EmvPinData emvPinData) {
        Log.w("CARD_READER", "onInputPin: " + "callback [onInputPIN]:" + emvPinData.type);

        bUIThreadisRunning = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                PinpadService.Open(context);
                param = new PinPadTasks(context, emvPinData, payData.getAmount(),event);
                cardModel = param.extractCardData();
                pan = cardModel.getPan();
                if (cardModel != null) {
                    Log.i("CARD_READER", "CARD READER VALUES :   " + cardModel.getPinBlock() + "\n" + cardModel.getKsn());

                } else {
                    Log.i("CARD_READER", "NULL");
                    mResult = EmvService.EMV_FALSE;
                }

                if (ret == PinpadService.PIN_ERROR_CANCEL) {
                    mResult = EmvService.ERR_USERCANCEL;

                    Log.e("CARD_READER", "PIN_ERROR_CANCEL");
                } else if (ret == PinpadService.PIN_OK) {
                    mResult = EmvService.EMV_TRUE;
                } else {
                    mResult = EmvService.EMV_FALSE;
                }
                bUIThreadisRunning = false;
            }
        }).start();
        while (bUIThreadisRunning) {//Wait for user confirmation
            try {
                Thread.currentThread().sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        //No password entered or input canceled
        Log.w("listener", "onInputPIN callback result: " + mResult);
        if (mResult != EmvService.EMV_TRUE) {
            return mResult;
        }
        // return EmvService.EMV_TRUE;
        return mResult;
    }

    @Override
    public int onSelectApp(EmvCandidateApp[] emvCandidateApps) {
        return emvCandidateApps[0].index;
    }

    @Override
    public int onSelectAppFail(int i) {
        return EmvService.EMV_FALSE;
    }

    @Override
    public int onFinishReadAppData() {
        return EmvService.EMV_TRUE;
    }

    @Override
    public int onVerifyCert() {
        return EmvService.EMV_TRUE;
    }

    @Override
    public int onOnlineProcess(EmvOnlineData emvOnlineData) {
        if (event == IC) {
            Log.i("ONLINEPOC", "TEST");
            EmvModel emvModel;

            try {
                emvTLVExtractor = new EmvTLVExtractor(emvService);
                getPurchaseData = new KimonoRequest(emvTLVExtractor.extractEmvData(), payData, cardModel);
                setKimonoData(getPurchaseData.Payload());
                payData.setKimonoData(getPurchaseData.Payload());
                Log.i("TEST_CARD_IMPL", "REQ DATA:\n" + getKimonoData());
                // return EmvService.ONLINE_FAILED;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {

            //-------------------------------------------------------------------------------------------------------
        }
        return EmvService.EMV_TRUE;
    }

    @Override
    public int onRequireTagValue(int i, int i1, byte[] bytes) {
        return EmvService.EMV_TRUE;
    }

    @Override
    public int onRequireDatetime(byte[] datetime) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        Date curDate = new Date(System.currentTimeMillis());//Get current time
        String str = formatter.format(curDate);
        byte[] time = new byte[0];
        try {
            time = str.getBytes("ascii");
            System.arraycopy(time, 0, datetime, 0, datetime.length);
            return EmvService.EMV_TRUE;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Log.e("MyEmvService", "onRequireDatetime failed");
            return EmvService.EMV_FALSE;
        }
    }

    @Override
    public int onReferProc() {
        return EmvService.EMV_TRUE;
    }

    @Override
    public int OnCheckException(String s) {
        return EmvService.EMV_TRUE;
    }

    @Override
    public int OnCheckException_qvsdc(int i, String s) {
        return 0;
    }

    @Override
    public int onMir_FinishReadAppData() {
        return EmvService.EMV_TRUE;
    }

    @Override
    public int onMir_DataExchange() {
        return 0;
    }

    @Override
    public int onMir_Hint() {
        return 0;
    }

    public void setKimonoData(String kimonoData) {
        this.kimonoData = kimonoData;
    }

    public String getKimonoData() {
        return kimonoData;
    }

    public String getPan() {
        return pan;
    }
}
