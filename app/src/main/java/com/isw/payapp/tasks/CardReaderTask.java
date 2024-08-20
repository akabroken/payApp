//package com.isw.payapp.tasks;
//
//import android.annotation.SuppressLint;
//import android.app.Activity;
//import android.app.AlertDialog;
//import android.app.KeyguardManager;
//import android.content.Context;
//import android.content.DialogInterface;
//import android.graphics.Bitmap;
//import android.media.ThumbnailUtils;
//import android.os.Handler;
//import android.os.Looper;
//import android.os.PowerManager;
//import android.util.Log;
//import android.widget.Toast;
//
//import com.isw.payapp.R;
//import com.isw.payapp.commonActions.PrinterAction;
//import com.isw.payapp.dialog.DialogListener;
//import com.isw.payapp.dialog.MyProgressDialog;
//import com.isw.payapp.dialog.WritePadDialog;
//import com.isw.payapp.interfaces.CardReaderListener;
//import com.isw.payapp.interfaces.DetectCardListener;
//import com.isw.payapp.payments.KimonoPurchase;
//import com.isw.payapp.terminal.config.DefaultAppCapk;
//import com.isw.payapp.terminal.model.CardModel;
//import com.isw.payapp.terminal.model.EmvModel;
//import com.isw.payapp.terminal.model.PayData;
//import com.isw.payapp.terminal.model.TerminalModel;
//import com.isw.payapp.terminal.processors.PurchaseProcessor;
//import com.telpo.emv.EmvAmountData;
//import com.telpo.emv.EmvCandidateApp;
//import com.telpo.emv.EmvOnlineData;
//import com.telpo.emv.EmvParam;
//import com.telpo.emv.EmvPinData;
//import com.telpo.emv.EmvService;
//import com.telpo.emv.EmvServiceListener;
//import com.telpo.pinpad.PinpadService;
//import com.telpo.tps550.api.printer.UsbThermalPrinter;
//
//import org.w3c.dom.Document;
//import org.w3c.dom.Element;
//import org.w3c.dom.NodeList;
//
//import java.io.ByteArrayInputStream;
//import java.io.UnsupportedEncodingException;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.concurrent.TimeUnit;
//
//import javax.xml.parsers.DocumentBuilder;
//import javax.xml.parsers.DocumentBuilderFactory;
//
//public class CardReaderTask /*implements DetectCardListener*/ {
//    //Context
//    private Context context;
//    private final Handler uiHandler;
//    private final MyProgressDialog progressDialog;
//    // Telpo
//    private CardReaderListener listener;
//    //
//    private EmvService emvService;
//    private UsbThermalPrinter usbThermalPrinter;
//
//    private String Amount;
//
//    //Models
//    private EmvModel emvModel;
//    private CardModel cardModel;
//    private TerminalModel terminalModel;
//    private PayData payData;
//    //INTEGER
//    int ret, event;
//
//    //STRING
//    //
//    private String out_pinblock;
//    private String out_ksn;
//    private String ksn_tag;
//    private String cardNum;
//    //
//    public static int Mag = 0;
//    public static int IC = 1;
//    public static int Nfc = 2;
//
//    boolean waitsign = true;
//    //
//
//    boolean userCancel = false;
//    boolean isSupportIC = true;
//    boolean isSupportMag = true;
//    boolean isSupportNfc = true;
//
//    long startMs;
//
//    int mResult;
//    boolean bUIThreadisRunning = true;
//
//    public CardReaderTask(Context context, Handler uiHandler, MyProgressDialog progressDialog, PayData payData) {
//        this.context = context;
//        this.payData = payData;
//        this.uiHandler = uiHandler;
//        this.progressDialog = progressDialog;
//
//        GetStarted();
//    }
//
//
//    private void GetStarted(){
//        initCardReader();
//        emvService = EmvService.getInstance();
//        emvService.setListener(emvListener);
//        uiHandler.post(()->{
//            MyProgressDialog pDialog = new MyProgressDialog(context);
//            //progressDialog = new MyProgressDialog(context);
//
//            pDialog.setTitle("Read card");
//            pDialog.setCancelable(true);
//            pDialog.setCanceledOnTouchOutside(false);
//            pDialog.setMessage("Please Insert card");
//            pDialog.show();
//           // listener.onProgressUpdate("Waiting for card insertion...");
//
//            try {
//                TimeUnit.SECONDS.sleep(3);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            pDialog.dismiss();
//
//        });
//    }
//    public void setListener(CardReaderListener listener) {
//        this.listener = listener;
//    }
//
//
//    public void wakeUpAndUnlock(Context context) {
//        KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
//        KeyguardManager.KeyguardLock kl = km.newKeyguardLock("unLock");
//        //Unlock
//        kl.disableKeyguard();
//        //Get the power manager object
//        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
//        //Get the PowerManager.WakeLock object. The following parameter | means passing in two values ​​at the same time. The last one is the Tag used in LogCat.
//        @SuppressLint("InvalidWakeLockTag") PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK, "bright");
//        //light up screen
//        wl.acquire();
//        //freed
//        wl.release();
//    }
//    private int initCardReader(){
//        int ret = EmvService.Open(context);
//        if (ret != EmvService.EMV_TRUE) {
//            Log.e("PAYMENTTASK", "EmvService.Open fail");
//            Toast.makeText(context, "EmvService.Open fail", Toast.LENGTH_SHORT).show();
//            return EmvService.EMV_FALSE;
//        }
//
//        ret = EmvService.deviceOpen();
//        if (ret != 0) {
//            Log.e("PAYMENTTASK", "EmvService.Open fail");
//            Toast.makeText(context, "EmvService.Open fail", Toast.LENGTH_SHORT).show();
//            return EmvService.EMV_FALSE;
//        }
//
//        ret = PinpadService.Open((context));//Returns 0 on success and otherwise on failure
//
//
//        if (ret == PinpadService.PIN_ERROR_NEED_TO_FOMRAT) {
//            PinpadService.TP_PinpadFormat(context);
//            ret = PinpadService.Open((context));//Returns 0 on success and otherwise on failure
//        }
//        Log.d("PAYMENTTASK", "PinpadService deviceOpen open:" + ret);
//        if (ret != 0) {
//            Toast.makeText(context, "PinpadService open fail", Toast.LENGTH_SHORT).show();
//            return EmvService.EMV_FALSE;
//        }
//        EmvService.Emv_SetDebugOn(1);//Turn on debugging information
//
//        EmvService.Emv_RemoveAllApp();
//        DefaultAppCapk.Add_All_APP();
//
//        EmvService.Emv_RemoveAllCapk();
//        DefaultAppCapk.Add_All_CAPK();
//        return  EmvService.EMV_TRUE;
//    }
//
//
//    public void waitForCardInsertion() throws InterruptedException {
//
//        uiHandler.post(()->{
//            MyProgressDialog            progressDialog = new MyProgressDialog(context);
//
//            progressDialog.setTitle("Read card");
//            progressDialog.setCancelable(true);
//            progressDialog.setCanceledOnTouchOutside(false);
//            progressDialog.setMessage("Please Insert card");
//            progressDialog.show();
//            listener.onProgressUpdate("Waiting for card insertion...");
////            initCardReader();
////            emvService = EmvService.getInstance();
////            emvService.setListener(emvListener);
//            try {
//                TimeUnit.SECONDS.sleep(2);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            progressDialog.dismiss();
//
//        });
//    }
//
//
//    public void cardDitected() throws InterruptedException {
//        listener.onCardDataRead("Card detected...");
//        TimeUnit.SECONDS.sleep(2);
//        openDevice();
//        try {
//            // publishProgress("detect card ..");
//            event = detectCard();
//            //publishProgress("open device..");
//
//            Log.e("CARDREADER", "event  " + event);
//
//            if (event == IC) {
//
//                ret = EmvService.IccCard_Poweron();
//                Log.w("CARDREADER", "IccCard_Poweron: " + ret);
//                ret = emvService.Emv_TransInit();
//                Log.w("CARDREADER", "Emv_TransInit: " + ret);
//
//                {
//                    EmvParam mEMVParam;
//                    mEMVParam = new EmvParam();
//                    mEMVParam.MerchName = "TEST ANDROID ISW".getBytes();
//                    //   mEMVParam.MerchCateCode=
//                    mEMVParam.MerchId = "CBLKE0000000001".getBytes();
//                    mEMVParam.TermId = "CBLKE001".getBytes();
//                    mEMVParam.TerminalType = 0x22;
//                    mEMVParam.Capability = new byte[]{(byte) 0xE0, (byte) 0xF9, (byte) 0xC8};
//                    mEMVParam.ExCapability = new byte[]{(byte) 0xE0, 0x00, (byte) 0xF0, (byte) 0xA0, 0x01};
//                    mEMVParam.CountryCode = new byte[]{(byte) 0x04, (byte) 0x04};
//                    mEMVParam.TransType = 0x00; //0x31
//                    emvService.Emv_SetParam(mEMVParam);
//                }
//                startMs = System.currentTimeMillis();
//                Log.e("CARDREADER", "emvlog_start");
//
//                emvService.Emv_SetOfflinePinCBenable(1);  //offline card will call back oninputpin
//
//                ret = emvService.Emv_StartApp(0);
//                Log.e("CARDREADER", "Emv_StartApp: " + ret);
//
//                if (ret == EmvService.EMV_TRUE) {
//
//                    //handler.sendMessage(handler.obtainMessage(4));
//
//                } else {
//
//                    //handler.sendMessage(handler.obtainMessage(7));
//                }
//
//
//            } else {
//                //      progressDialog.dismiss();
//                //   stopPlayer.start();
//                Log.w("CARDREADER", "NFC: " + "detect error:" + ret);
//                //       handler.sendMessage(handler.obtainMessage(4));
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    // Simulate reading card data
//    public void readCardData() throws InterruptedException {
//
//        TimeUnit.SECONDS.sleep(2);
//        String cardData = "EMV Data from card";
//        listener.onCardDataRead(cardData);
//    }
//
//
//    public void postRequestToServer(String cardData) throws InterruptedException {
//
//
//        TimeUnit.SECONDS.sleep(2);
//        String response = "Response from server";
//        listener.onRequestComplete(response);
//
//    }
//
//
//    public void onError(String errorMessage) {
//
//    }
//
//
//    final EmvServiceListener emvListener = new EmvServiceListener() {
//        //  progressDialog = new TelpoProgressDialog(context, 80000, dialogTimeOutListener);
//        @Override
//        public int onInputAmount(EmvAmountData emvAmountData) {
//            long amt = (long) (Double.parseDouble(payData.getAmount()) * 100);
//            emvAmountData.Amount = amt;
//            emvAmountData.TransCurrCode = (short) 404;//rupay834   //156 personal coins
//            emvAmountData.ReferCurrCode = (short) 404;//rupay834    //156 personal coins
//            emvAmountData.TransCurrExp = (byte) 2;
//            emvAmountData.ReferCurrExp = (byte) 2;
//            emvAmountData.ReferCurrCon = 0;
//            emvAmountData.CashbackAmount = 0;
//            return EmvService.EMV_TRUE;
//        }
//
//        @Override
//        public int onInputPin(final EmvPinData emvPinData) {
//            Log.w("CARD_READER", "onInputPin: " + "callback [onInputPIN]:" + emvPinData.type);
//
//            bUIThreadisRunning = true;
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    PinpadService.Open(context);
//                    PinPadTasks param = new PinPadTasks(context, emvPinData, payData.getAmount());
//                    CardModel cardModel = param.extractCardData();
//                    if (cardModel != null) {
//                        Log.i("CARD_READER", "CARD READER VALUES :   " + cardModel.getPinBlock() + "\n" + cardModel.getKsn());
//                        out_pinblock = cardModel.getPinBlock();
//                        out_ksn = cardModel.getKsn().substring(4);
//                        ksn_tag = cardModel.getKsn().substring(4, 10);
//                    } else {
//                        Log.i("CARD_READER", "NULL");
//                        mResult = EmvService.EMV_FALSE;
//                    }
//
//                    if (ret == PinpadService.PIN_ERROR_CANCEL) {
//                        mResult = EmvService.ERR_USERCANCEL;
//
//                        Log.e("CARD_READER", "PIN_ERROR_CANCEL");
//                    } else if (ret == PinpadService.PIN_OK) {
//                        mResult = EmvService.EMV_TRUE;
//                    } else {
//                        mResult = EmvService.EMV_FALSE;
//                    }
//                    bUIThreadisRunning = false;
//                }
//            }).start();
//            while (bUIThreadisRunning) {//Wait for user confirmation
//                try {
//                    Thread.currentThread().sleep(500);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            //No password entered or input canceled
//            Log.w("listener", "onInputPIN callback result: " + mResult);
//            if (mResult != EmvService.EMV_TRUE) {
//                return mResult;
//            }
//            // return EmvService.EMV_TRUE;
//            return mResult;
//        }
//
//        @Override
//        public int onSelectApp(EmvCandidateApp[] emvCandidateApps) {
//            return emvCandidateApps[0].index;
//            // return 0;
//        }
//
//        @Override
//        public int onSelectAppFail(int i) {
//            return EmvService.EMV_TRUE;
//        }
//
//        @Override
//        public int onFinishReadAppData() {
//            return EmvService.EMV_TRUE;
//        }
//
//        @Override
//        public int onVerifyCert() {
//            return EmvService.EMV_TRUE;
//        }
//
//        @Override
//        public int onOnlineProcess(EmvOnlineData emvOnlineData) {
//            if (event == IC) {
//                Log.i("ONLINEPOC", "TEST");
//                EmvModel emvModel;
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                uiHandler.post(()->{
//                    uiHandler.sendMessage(uiHandler.obtainMessage(1));
//                });
//
//                try {
//
//                    //Extracting EMV Data
//                    StringBuffer p;
//                    // emvService.
//                    emvModel = new EmvModel();
//
//                    EmvTLVExtractor emvTLVExtractor = new EmvTLVExtractor(emvService);
//
//                    PrinterAction printerAction = new PrinterAction();
//                    CardModel cardModel = new CardModel();
//                    cardModel.setKsn(out_ksn);
//                    cardModel.setKsnd("605");
//                    cardModel.setPinBlock(out_pinblock);
//                    cardModel.setPinType("Dukpt");
//                    cardModel.setKSNTag(ksn_tag);
//                    // handler.sendMessage(handler.obtainMessage(5));
//                    KimonoPurchase getPurchaseData = new KimonoPurchase(emvTLVExtractor.extractEmvData(), payData.getAmount(), cardModel);
//                    String payload = getPurchaseData.Payload();
//                    PurchaseProcessor purchaseProcessor = new PurchaseProcessor();
//                    String out = purchaseProcessor.process(payload);
//                    //   handler.sendMessage(handler.obtainMessage(5));
//                    try {
//                        Thread.sleep(6000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    //        progressDialog.dismiss();
//
//                    Log.i("Kennedy", "RESPONSE: " + out);
//
//                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//                    DocumentBuilder builder = factory.newDocumentBuilder();
//                    Document doc = builder.parse(new ByteArrayInputStream(out.getBytes()));
//
//                    Element root = doc.getDocumentElement();
//                    NodeList nodeList = root.getChildNodes();
//
//                    String message = doc.getElementsByTagName("description").item(0).getTextContent();
//                    String resp = doc.getElementsByTagName("field39").item(0).getTextContent();
//
//                    Log.i("RESPONSETESTfield39", resp + "--->" + message);
//                    if (resp.equals("00")) {
//                        emvOnlineData.ResponeCode = "00".getBytes("ascii");
//
//                        //        handler.sendMessage(handler.obtainMessage(2));//
//                        while (waitsign) {
//                            Thread.sleep(500);
//                        }
//                        String auth = doc.getElementsByTagName("authId").item(0).getTextContent();
//                        String stan = doc.getElementsByTagName("stan").item(0).getTextContent();
//                        cardNum = emvModel.getPan().substring(0, 6) + "***" + emvModel.getPan().substring(12);
//                        String ref = doc.getElementsByTagName("referenceNumber").item(0).getTextContent();
//                        printerAction.PrintIC(usbThermalPrinter, cardNum, Amount, auth, stan, ref);
//
//                        return EmvService.ONLINE_APPROVE;
//                    } else {
//                        // Use runOnUiThread to execute UI operations on the main thread
//                        ((Activity) context).runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                new AlertDialog.Builder(context)
//                                        .setMessage("\n\t\t\t\t\tTransaction Declined\n\nReason: " + message)
//                                        .setPositiveButton("OK", null)
//                                        .show();
//                            }
//                        });
//
//
//                        try {
//                            // Add a delay if necessary before setting the response code
//                            Thread.sleep(500);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//
//                        emvOnlineData.ResponeCode = resp.getBytes("ascii");
//                        return EmvService.ONLINE_FAILED;
//                    }
//                    // return EmvService.ONLINE_FAILED;
//                } catch (UnsupportedEncodingException e) {
//                    e.printStackTrace();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            } else {
//
//                //-------------------------------------------------------------------------------------------------------
//            }
//
//
//            return EmvService.EMV_TRUE;
//        }
//
//        @Override
//        public int onRequireTagValue(int i, int i1, byte[] bytes) {
//            return EmvService.EMV_TRUE;
//        }
//
//        @Override
//        public int onRequireDatetime(byte[] datetime) {
//            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
//            Date curDate = new Date(System.currentTimeMillis());//Get current time
//            String str = formatter.format(curDate);
//            byte[] time = new byte[0];
//            try {
//                time = str.getBytes("ascii");
//                System.arraycopy(time, 0, datetime, 0, datetime.length);
//                return EmvService.EMV_TRUE;
//            } catch (UnsupportedEncodingException e) {
//                e.printStackTrace();
//                Log.e("MyEmvService", "onRequireDatetime failed");
//                return EmvService.EMV_FALSE;
//            }
//        }
//
//        @Override
//        public int onReferProc() {
//            return EmvService.EMV_TRUE;
//        }
//
//        @Override
//        public int OnCheckException(String Pan) {
//            return EmvService.EMV_FALSE;
//        }
//
//        @Override
//        public int OnCheckException_qvsdc(int i, String Pan) {
//            return EmvService.EMV_TRUE;
//        }
//
//        @Override
//        public int onMir_FinishReadAppData() {
//            return 0;
//        }
//
//        @Override
//        public int onMir_DataExchange() {
//            return 0;
//        }
//
//        @Override
//        public int onMir_Hint() {
//            return 0;
//        }
//    };
//
//
//   // @Override
//    public void openDevice() {
//        int ret;
//        if (isSupportMag) {
//            event = EmvService.MagStripeOpenReader();
//        }
//
//        if (isSupportIC) {
//            event = EmvService.IccOpenReader();
//        }
//
//        if (isSupportNfc) {
//            event = EmvService.NfcOpenReader(1000);
//        }
//    }
//
//   // @Override
//    public void deviceClose() {
//        int ret;
//        if (isSupportMag) {
//            ret = EmvService.MagStripeCloseReader();
//        }
//
//        if (isSupportIC) {
//            if (event == IC) {
//                ret = EmvService.IccCard_Poweroff();
//            }
//            ret = EmvService.IccCloseReader();
//        }
//
//        if (isSupportNfc) {
//            ret = EmvService.NfcCloseReader();
//        }
//    }
//
//
//   // @Override
//    public int detectCard() {
//        int ret;
//        userCancel = false;
//
//        while (true) {
//            if (userCancel) {
//                Log.e("CARDREADER", "userCancel");
//                return IC;
//            }
//
//            if (isSupportMag) {
//                ret = EmvService.MagStripeCheckCard(1000);
//                DefaultAppCapk.Log("MagStripeCheckCard:" + ret);
//                if (ret == 0) {
//                    Log.e("CARDREADER", "Mag");
//                    return Mag;
//                }
//            }
//
//            if (isSupportIC) {
//                ret = EmvService.IccCheckCard(300);
//                DefaultAppCapk.Log("IccCheckCard:" + ret);
//                if (ret == 0) {
//                    Log.e("CARDREADER", "IC");
//                    return IC;
//                }
//            }
//
//            if (isSupportNfc) {
//                ret = EmvService.NfcCheckCard(1000);
//                DefaultAppCapk.Log("NfcCheckCard:" + ret);
//                if (ret == 0) {
//                    Log.e("CARDREADER", "Nfc");
//                    return Nfc;
//                }
//            }
//        }
//    }
//
//    //@Override
//    public int detectNFC() {
//        userCancel = false;
//        long j = System.currentTimeMillis();
//        int ret = -1;
//        while (System.currentTimeMillis() - j < 20 * 1000) {
//
//            if (userCancel == true) {
//                return -4;
//            }
//
//            ret = EmvService.NfcCheckCard(100);
//            if (ret == 0) {
//                return 0;
//            }
//            j++;
//        }
//        return ret;
//    }
//
//    //@Override
//    public int detectCardKernel() {
//        int ret = -1;
//        ret = emvService.NFC_CheckKernelID();
//        return ret;
//    }
//
//  //  @Override
//    public byte[] getBooleanArray(byte b) {
//        byte[] array = new byte[8];
//        for (int i = 7; i >= 0; i--) {
//            array[i] = (byte) (b & 1);
//            b = (byte) (b >> 1);
//        }
//        return array;
//    }
//
//}
