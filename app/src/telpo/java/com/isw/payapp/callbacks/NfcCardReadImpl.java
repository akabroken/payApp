package com.isw.payapp.callbacks;

import android.content.Context;
import android.util.Log;

import com.isw.payapp.paymentsRequests.KxmlRequest;
import com.isw.payapp.tasks.EmvTLVExtractor;
import com.isw.payapp.model.CardModel;
import com.isw.payapp.model.TransactionData;
import com.isw.payapp.utils.StringUtils;
import com.telpo.emv.EmvAmountData;
import com.telpo.emv.EmvCandidateApp;
import com.telpo.emv.EmvOnlineData;
import com.telpo.emv.EmvParam;
import com.telpo.emv.EmvPinData;
import com.telpo.emv.EmvService;
import com.telpo.emv.EmvServiceListener;
import com.telpo.emv.EmvTLV;
import com.telpo.emv.QvsdcParam;
import com.telpo.pinpad.PinParam;
import com.telpo.pinpad.PinpadService;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NfcCardReadImpl {
    private Context context;
    private int iRet;
    private final EmvService emvService;
    private final TransactionData payData;
    private EmvTLVExtractor emvTLVExtractor;
    private KxmlRequest kimonoRequest;
    private CardModel cardModel;

    public NfcCardReadImpl(Context context, int iRet, EmvService emvService, TransactionData payData) {
        this.context = context;
        this.iRet = iRet;
        this.emvService = emvService;
        this.payData = payData;
        cardModel = new CardModel();
    }

    int mResult;
    boolean bUIThreadisRunning = true;

    EmvServiceListener listener = new EmvServiceListener() {

        @Override
        public int onInputAmount(EmvAmountData AmountData) {
            double amt = Double.parseDouble(payData.getAmount());
            AmountData.Amount = (long) amt * 100;
            AmountData.TransCurrCode = (short) 404;//rupay834   //156人名币
            AmountData.ReferCurrCode = (short) 404;//rupay834    //156人名币
            AmountData.TransCurrExp = (byte) 2;
            AmountData.ReferCurrExp = (byte) 2;
            AmountData.ReferCurrCon = 0;
            AmountData.CashbackAmount = 0;
            return EmvService.EMV_TRUE;
        }

        @Override
        public int onInputPin(final EmvPinData PinData) {
            Log.w("input pin", "onInputPin: " + "callback [onInputPIN]:" + PinData.type);

            bUIThreadisRunning = true;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    int ret;
                    PinParam param = new PinParam(context);

                    EmvTLV pan = new EmvTLV(0x5A);
                    ret = emvService.Emv_GetTLV(pan);
                    Log.e("yw_getpantlv", "result" + ret);

                    if (ret == EmvService.EMV_TRUE) {
                        StringBuffer p = new StringBuffer(StringUtils.bytesToHexString(pan.Value));
                        if (p.charAt(p.toString().length() - 1) == 'F') {
                            p.deleteCharAt(p.toString().length() - 1);
                        }
                        param.CardNo = p.toString();

                        Log.w("listener", "CardNo: " + param.CardNo);
                    }

                    param.KeyIndex = 1;
                    param.WaitSec = 100;
                    param.MaxPinLen = 6;
                    param.MinPinLen = 4;
                    param.IsShowCardNo = 1;
                    param.Amount = payData.getAmount();
                    PinpadService.Open(context);

                    if (PinData.type == 0) {
                        ret = PinpadService.TP_PinpadGetPin(param);

                    } else {
                        ret = PinpadService.TP_PinpadGetPlainPin(param, 0, 0, 0);  //明文pinpad

                        if (ret == PinpadService.PIN_OK) {
                            PinData.Pin = param.Pin_Block;
                        }
                    }

                    //
                    //  pin-block
                    // Log.e("yw", "TP_PinpadGetPin: " + ret + "\nPinblock: " + StringUtil.bytesToHexString(param.Pin_Block));
                    if (ret == PinpadService.PIN_ERROR_CANCEL) {
                        mResult = EmvService.ERR_USERCANCEL;

                        Log.e("yw", "PIN_ERROR_CANCEL");
                    } else if (ret == PinpadService.PIN_OK) {
                        mResult = EmvService.EMV_TRUE;
                    } else {
                        mResult = EmvService.EMV_FALSE;
                    }

                    bUIThreadisRunning = false;
                }
            }).start();

            while (bUIThreadisRunning) {//等待用户确认
                try {
                    Thread.currentThread().sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }


            Log.w("listener", "onInputPIN callback result: " + mResult);
            if (mResult != EmvService.EMV_TRUE) {
                return mResult;
            }
            return EmvService.EMV_TRUE;
        }

        @Override
        public int onSelectApp(EmvCandidateApp[] appList) {//多个AID   在这里处理选择

            return appList[0].index;
        }

        @Override
        public int onSelectAppFail(int ErrCode) {  //AID
            return EmvService.EMV_TRUE;
        }

        @Override
        public int onFinishReadAppData() {
            //paywave 1 paypass2

            return EmvService.EMV_TRUE;
        }

        @Override
        public int onVerifyCert() {
            return EmvService.EMV_TRUE;
        }


        @Override // 7671 --- 1234
        public int onOnlineProcess(EmvOnlineData OnlineData) {

            // EMV process
            payData.setCardType("Nfc");
            int i = emvService.qVsdc_IsNeedPin();
            Log.e("yw_paywave", "needpin  " + i);
            //paywave 2
            //-------------------------------------------------------------------------------------------------------
            final PinParam param = new PinParam(context);
            final int ret;
            EmvTLV pan = new EmvTLV(0x5A);
            ret = emvService.Emv_GetTLV(pan);
            if (ret == EmvService.EMV_TRUE) {
                StringBuffer p = new StringBuffer(StringUtils.bytesToHexString(pan.Value));
                if (p.charAt(p.toString().length() - 1) == 'F') {
                    p.deleteCharAt(p.toString().length() - 1);
                }
                param.CardNo = p.toString();

                Log.w("listener", "CardNo: " + param.CardNo);
            } else {
                pan = new EmvTLV(0x57);
                if (emvService.Emv_GetTLV(pan) == EmvService.EMV_TRUE) {
                    String panstr = StringUtils.bytesToHexString(pan.Value);
                    Log.w("pan", "panstr: " + panstr);
                    int index = panstr.indexOf("D");
                    Log.w("pan", "index: " + index);
                    param.CardNo = panstr.substring(0, index);

                }
            }

            bUIThreadisRunning = true;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    int ret;
                    param.PinBlockFormat = 0;
                    param.KeyIndex = 0;
                    param.WaitSec = 100;
                    param.MaxPinLen = 6;
                    param.MinPinLen = 4;
                    param.IsShowCardNo = 1;
                    param.Amount = payData.getAmount();
                    PinpadService.Open(context);

                    //  ret = PinpadService.TP_PinpadGetPin(param);
                    ret = PinpadService.TP_PinpadDukptSessionStart(0);
                    if (0 != ret) {
                        //  mResult = EmvService.EMV_FALSE;
                    }
                    ret = PinpadService.TP_PinpadDukptGetPin(param);
                    if (ret != 0) {
                        Log.e("PINPADERR", "PIN INPUT ERROR : " + ret);
                        // mResult = EmvService.ERR_NOPIN;
                        cardModel = null;
                    }
                    //  pin-block
                    Log.e("yw", "TP_PinpadGetPinnn: " + ret + "\nPinblock: " + StringUtils.bytesToHexString(param.Pin_Block));
                    cardModel.setPinBlock(StringUtils.bytesToHexString(param.Pin_Block));
                    cardModel.setKsn(StringUtils.bytesToHexString(param.Curr_KSN).substring(4));
                    cardModel.setKSNTag(StringUtils.bytesToHexString(param.Curr_KSN).substring(4, 10));
                    cardModel.setKsnd("605");
                    cardModel.setPinType("Dukpt");
                    PinpadService.TP_PinpadDukptSessionEnd();
                    if (ret == PinpadService.PIN_ERROR_CANCEL) {
                        mResult = EmvService.ERR_USERCANCEL;
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

            Log.w("listener", "onInputPIN callback result: " + mResult);
            if (mResult != EmvService.EMV_TRUE) {
                return mResult;
            } else {

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


                try {
                    OnlineData.ResponeCode = "00".getBytes("ascii");
                    return EmvService.ONLINE_APPROVE;//success

                    //    return EmvService.ONLINE_FAILED;  //fail
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            //-------------------------------------------------------------------------------------------------------
            return EmvService.EMV_TRUE;


        }

        @Override
        public int onRequireTagValue(int tag, int len, byte[] value) {

            //paypass1——————————-----------------------------------


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
        public int OnCheckException(String PAN) {
            return EmvService.EMV_FALSE;
        }

        @Override
        public int OnCheckException_qvsdc(int index, String PAN) {
            return EmvService.EMV_TRUE;
        }

        @Override
        public int onMir_FinishReadAppData() {
            return 0;
        }

        @Override
        public int onMir_DataExchange() {
            return 0;
        }

        @Override
        public int onMir_Hint() {
            return 0;
        }
    };


    private EmvParam initEmvParam() {
        EmvParam mEMVParam;
        mEMVParam = new EmvParam();
        mEMVParam.MerchName = "TEST ANDROID ISW".getBytes();
        mEMVParam.MerchId = "CBLKE0000000001".getBytes();
        mEMVParam.TermId = "CBLKE001".getBytes();
        mEMVParam.TerminalType = 0x22;
        mEMVParam.Capability = new byte[]{(byte) 0xE0, (byte) 0xF9, (byte) 0xC8};
        mEMVParam.ExCapability = new byte[]{(byte) 0xE0, 0x00, (byte) 0xF0, (byte) 0xA0, 0x01};
        mEMVParam.CountryCode = new byte[]{(byte) 0x04, (byte) 0x04};
        mEMVParam.TransType = 0x00; //0x31
        return mEMVParam;
        // Log.w("readcard", "qVsdc_Preprocess: " + ret);
    }

    private QvsdcParam vParam(long val) {
        QvsdcParam qvsdcParam = new QvsdcParam();
        qvsdcParam.AMOUNT_Amount = val;//Long.parseLong(payData.getAmount());
        qvsdcParam.AMOUNT_CashbackAmount = 0;
        qvsdcParam.AMOUNT_CurrCode = 404;
        qvsdcParam.AMOUNT_CurrExp = 2;
        qvsdcParam.SUPPORT_MSD = 0;
        qvsdcParam.SUPPORT_EMV = 0;
        qvsdcParam.SUPPORT_Signature = 1;
        qvsdcParam.SUPPORT_OnlinePIN = 1;
        qvsdcParam.SUPPORT_CashControl = 0;
        qvsdcParam.SUPPORT_CashbackControl = 0;
        qvsdcParam.SUPPORT_ZeroAmtCheck = 1;
        qvsdcParam.SUPPORT_ZeroCheckType = 0;
        return qvsdcParam;
    }

    public void readNfcCard() throws InterruptedException {
        emvService.setListener(listener);
        payData.setPosEntryMode("071");
        switch (iRet) {
            case EmvService.NFC_KERNEL_DEFAUT_CARD_VISA:
                double amt = Double.parseDouble(payData.getAmount());
                iRet = emvService.qVsdc_TransInit(vParam((long) amt*100));

                Log.w("readcard", "qVsdc_TransInit: " + iRet);
                //initEmvParam();
                emvService.Emv_SetParam(initEmvParam());
                // Preprocessing
                iRet = emvService.qVsdc_Preprocess();
                Log.i("qVsdc_Preprocess", "RESPONSE =" + iRet);
                // Start process
                iRet = emvService.qVsdc_StartApp();
                Log.w("qVsdc_StartApp", "RESPONSE = " + iRet);
                iRet = emvService.qVsdc_IsNeedPin();
                Log.i("qVsdc_IsNeedPin", "RESPONSE = " + iRet);
                Thread.sleep(1000);
                // onGetPin(getPan());


                break;
            case EmvService.NFC_KERNEL_DEFAUT_CARD_MASTER:
                break;
            case EmvService.NFC_KERNEL_DEFAUT_CARD_UNIONPAY:
                break;
            case EmvService.NFC_KERNEL_DEFAUT_CARD_UNKNOWN:
                break;
            default:
                break;
        }
    }

    private String getPan() {
        int ret;
        try {
            EmvTLV pan = new EmvTLV(0x5A);
            ret = emvService.Emv_GetTLV(pan);
            if (ret == EmvService.EMV_TRUE) {
                StringBuffer p = new StringBuffer(StringUtils.bytesToHexString(pan.Value));
                if (p.charAt(p.toString().length() - 1) == 'F') {
                    p.deleteCharAt(p.toString().length() - 1);
                }
                return p.toString();

            } else {
                pan = new EmvTLV(0x57);
                if (emvService.Emv_GetTLV(pan) == EmvService.EMV_TRUE) {
                    String panstr = StringUtils.bytesToHexString(pan.Value);
                    Log.w("pan", "panstr: " + panstr);
                    int index = panstr.indexOf("D");
                    Log.w("pan", "index: " + index);
                    return panstr.substring(0, index);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void onGetPin(String pan) {
        PinParam param = new PinParam(context);
        try {
            param.CardNo = pan;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    int ret;
                    param.KeyIndex = 0;
                    param.WaitSec = 100;
                    param.MaxPinLen = 4;
                    param.MinPinLen = 4;
                    // param.CardNo = "5223402300485719";
                    param.IsShowCardNo = 1;
                    param.Amount = payData.getAmount();
                    PinpadService.Open(context);

                    ret = PinpadService.TP_PinpadGetPin(param);
                    //  pin-block
                    Log.e("yw", "TP_PinpadGetPin: " + ret + "\nPinblock: " + StringUtils.bytesToHexString(param.Pin_Block));
                    if (ret == PinpadService.PIN_ERROR_CANCEL) {
                        cardModel.setPinBlock("0000000000000000");
                        cardModel.setKsn("00000000000000000");
                        cardModel.setKSNTag("000000");
                    } else if (ret == PinpadService.PIN_OK) {
                        cardModel.setPinBlock(StringUtils.bytesToHexString(param.Pin_Block));
                        cardModel.setKsn(StringUtils.bytesToHexString(param.Curr_KSN).substring(4));
                        cardModel.setKSNTag(StringUtils.bytesToHexString(param.Curr_KSN).substring(4, 10));

                    } else {
                        cardModel.setPinBlock("0000000000000000");
                        cardModel.setKsn("00000000000000000");
                        cardModel.setKSNTag("000000");
                    }
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public String procPayment() {
        try {
            emvTLVExtractor = new EmvTLVExtractor(emvService,payData);
            kimonoRequest = new KxmlRequest(emvTLVExtractor.extractEmvData(), payData, cardModel);
            return kimonoRequest.Payload();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
