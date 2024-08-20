package com.isw.payapp.tasks;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.isw.payapp.commonActions.PrinterAction;
import com.isw.payapp.database.Kimono;
import com.isw.payapp.database.dao.TerminalDao;
import com.isw.payapp.database.dao.TransactionDao;
import com.isw.payapp.database.entities.Terminal;
import com.isw.payapp.database.entities.Transactions;
import com.isw.payapp.interfaces.CardReaderListener;
import com.isw.payapp.lmpl.IccCardReadImpl;
import com.isw.payapp.lmpl.NfcCardReadImpl;
import com.isw.payapp.lmpl.PinChangeImpl;
import com.isw.payapp.processors.KimonoApp;
import com.isw.payapp.processors.PinChangeProcessor;
import com.isw.payapp.terminal.config.DefaultAppCapk;
import com.isw.payapp.terminal.model.PayData;
import com.isw.payapp.terminal.processors.GetwayProcessor;
import com.telpo.emv.EmvParam;
import com.telpo.emv.EmvService;
import com.telpo.pinpad.PinpadService;
import com.telpo.tps550.api.printer.UsbThermalPrinter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.Callable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class PinChangeJobs implements Callable<String> {

    private static String WAIT_CARD = "Please Insert Card..";
    private static String READ_CARD = "Reading Card Data...";
    private static String POST_DATA = "Posting Data to Host";
    private static String RESPONSE = "Wait, Processing Data";
    private static String ERROR = "Error Encountered";
    private static int TRUE = 0;
    private static int FALSE = 1;
    private Context context;
    private NfcCardReadImpl nfcCardRead;
    //Telpo
    private EmvService emvService;
    private UsbThermalPrinter usbThermalPrinter;
    // private EmvServiceListener emvListener;
    private CardReaderListener listener;
    private PinChangeImpl cardReadListener;
    //
    private Handler uiHandler;

    //
    public static int Mag = 0;
    public static int IC = 1;
    public static int Nfc = 2;
    private PrinterAction printerAction;

    boolean waitsign = true;
    //
    private int event, ret;
    private long startMs;

    boolean userCancel = false;
    boolean isSupportIC = true;
    boolean isSupportMag = true;
    boolean isSupportNfc = true;

    //Model
    private PayData payData;

    private GetwayProcessor pinChangeProcessor;

    public PinChangeJobs(Context context, PayData payData) {
        // this.listener = listener;
        this.context = context;
        this.payData = payData;
    }

    public void setListner(CardReaderListener listener) {
        this.listener = listener;
    }

    @Override
    public String call() throws Exception {
        int ret;
        onWaitCardRead();
        Thread.sleep(1000);
        ret = onReadCardData();
        if (ret == TRUE) {
            Thread.sleep(1000);
            onPostingPayment();
            Thread.sleep(1000);
            onCloseProcess();
        }
        listener.onProgressEnd();

        return "CardReaderJob Task Completed";
    }

    //Wait for card read
    public void onWaitCardRead() {
        int ret = emvService.Open(context);
        if (ret != EmvService.EMV_TRUE) {
            Log.e("PAYMENTTASK", "EmvService.Open fail");
            listener.onProgressStart("EmvService.Open fail");
            return;
        }
        listener.onProgressStart("EmvService.Open Success");

        ret = emvService.deviceOpen();
        if (ret != 0) {
            Log.e("PAYMENTTASK", "EmvService.Open fail");
            Toast.makeText(context, "EmvService.Open fail", Toast.LENGTH_SHORT).show();
            listener.onProgressStart("EmvService.Open Device failed!!");
            return;
        }
        listener.onProgressStart("EmvService.Open Device success");

        ret = PinpadService.Open((context));//Returns 0 on success and otherwise on failure

        listener.onProgressStart("EmvService.Open PIN Pad");
        if (ret == PinpadService.PIN_ERROR_NEED_TO_FOMRAT) {
            PinpadService.TP_PinpadFormat(context);
            ret = PinpadService.Open((context));//Returns 0 on success and otherwise on failure
        }
        Log.d("PAYMENTTASK", "PinpadService deviceOpen open:" + ret);
        if (ret != 0) {
            listener.onProgressStart("EmvService.Open PIN Pad failed!!!");
            return;
        }
        listener.onProgressStart("EmvService.Open PIN Pad success");
        emvService.Emv_SetDebugOn(1);//Turn on debugging information

        emvService.Emv_RemoveAllApp();
        DefaultAppCapk.Add_All_APP();

        emvService.Emv_RemoveAllCapk();
        DefaultAppCapk.Add_All_CAPK();

        emvService = EmvService.getInstance();

        listener.onProgressStart("Init success");

        openDevice();
        //return  EmvService.EMV_TRUE;
    }

    //Read card Event
    public int onReadCardData() {
        listener.onCardDataRead("Please Insert Card");
        try {
            event = detectCard();

            if (event >= 0)
                listener.onCardDataRead("CardDetect");
            Log.e("CARDREADER", "event  " + event);

            if (event == IC) {
                listener.onCardDataRead("ICC Card Detected");
                cardReadListener = new PinChangeImpl(context, emvService, payData, event);
                emvService.setListener(cardReadListener);
                ret = EmvService.IccCard_Poweron();
                Log.w("CARDREADER", "IccCard_Poweron: " + ret);
                payData.setCardType("IC");
                ret = emvService.Emv_TransInit();
                Log.w("CARDREADER", "Emv_TransInit: " + ret);

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
                emvService.Emv_SetParam(mEMVParam);
                startMs = System.currentTimeMillis();

                Log.e("CARDREADER", "emvlog_start");
                emvService.Emv_SetOfflinePinCBenable(1);  //offline card will call back oninputpin
                Log.e("CARDREADER", "Emv_StartApp: " + ret);
                return TRUE;

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return FALSE;
    }

    public void onPostingPayment() throws Exception {
        listener.onRequestSent("Processing....");
        String prePayload = null;
        String out = null;
        pinChangeProcessor = new GetwayProcessor();

        Transactions transactions = new Transactions();
        transactions.setAmt(100);
        transactions.setAuthId("ABCDEF");
        transactions.setCardnum("1234567891234567");
        transactions.setTranDate("2024-03-25T12:32.34");
        transactions.setStan(1);


        Terminal terminal = new Terminal();
        terminal.setTerminalId("CBL00001");
        terminal.setAddress1("ISW");
        terminal.setLocation("NBO");
        terminal.setMerchantId("000000000000001");
        terminal.setAddress2("NBO WESTLANDS");
        terminal.setMerchType("7011");

        KimonoApp app = new KimonoApp(context);
        app.onCreate();
        Kimono kim = app.getKimono();
        TransactionDao transactionDao = kim.transactionDao();
        transactionDao.createTrans(transactions);
        List<Transactions> getTranData = transactionDao.getAll();
        for (int i = 0;i<getTranData.size(); i++){
            Log.i("DATABASETEST", "VALUES : "+ getTranData.get(i).tranDate );
        }
        TerminalDao terminalDao = kim.terminalDao();
        terminalDao.createTerminal(terminal);
        List<Terminal> getTerminal = terminalDao.getAll();
        for(int i = 0;i<getTerminal.size();i++){
            Log.i("TERMINADATA", "VALUES : "+getTerminal.get(i).terminalId+"|Merchant : "+ getTerminal.get(i).merchantId);
        }


        if (IC == event) {
            ret = emvService.Emv_StartApp(0);
            prePayload = cardReadListener.getKimonoData();
            out = pinChangeProcessor.process(prePayload);

            if (ret == EmvService.EMV_TRUE) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(new ByteArrayInputStream(out.getBytes()));

                Element root = doc.getDocumentElement();
                NodeList nodeList = root.getChildNodes();

                String message = doc.getElementsByTagName("description").item(0).getTextContent();
                String resp = doc.getElementsByTagName("field39").item(0).getTextContent();

                Log.i("RESPONSETESTfield39", resp + "--->" + message);
                if (resp.equals("00")) {

                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new AlertDialog.Builder(context)
                                    .setMessage("\n\t\t\t\t\tTransaction Approved")
                                    .setPositiveButton("OK", null)
                                    //.setCancelable()
                                    .show();
                        }
                    });
                    String auth = doc.getElementsByTagName("authId").item(0).getTextContent();
                    String stan = doc.getElementsByTagName("stan").item(0).getTextContent();
                    String cardNum = cardReadListener.getPan();
                    cardNum = cardNum.substring(0, 6) + "***" + cardNum.substring(12);
                    String ref = doc.getElementsByTagName("referenceNumber").item(0).getTextContent();
                    printerAction = new PrinterAction();
                    printerAction.PrintIC(usbThermalPrinter, cardNum, payData.getAmount(), auth, stan, ref);

                } else {
                    // Use runOnUiThread to execute UI operations on the main thread
                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new AlertDialog.Builder(context)
                                    .setMessage("\n\t\t\t\t\tTransaction Declined\n\nReason: " + message)
                                    .setPositiveButton("OK", null)
                                    //.setCancelable()
                                    .show();
                        }
                    });
                    listener.onRequestSent("Complete");
                }

            } else {

            }
        }
    }

    public void onCloseProcess() {
        listener.onProgressEnd();
    }


    public void openDevice() {
        int ret;
        if (isSupportMag) {
            event = emvService.MagStripeOpenReader();
        }

        if (isSupportIC) {
            event = emvService.IccOpenReader();
        }

        if (isSupportNfc) {
            event = emvService.NfcOpenReader(1000);
        }
    }

    // @Override
    public void deviceClose() {
        int ret;
        if (isSupportMag) {
            ret = EmvService.MagStripeCloseReader();
        }

        if (isSupportIC) {
            if (event == IC) {
                ret = EmvService.IccCard_Poweroff();
            }
            ret = EmvService.IccCloseReader();
        }

        if (isSupportNfc) {
            ret = EmvService.NfcCloseReader();
        }
    }


    // @Override
    public int detectCard() {
        int ret;
        userCancel = false;

        while (true) {
            if (userCancel) {
                Log.e("CARDREADER", "userCancel");
                return IC;
            }

            if (isSupportMag) {
                ret = EmvService.MagStripeCheckCard(1000);
                DefaultAppCapk.Log("MagStripeCheckCard:" + ret);
                if (ret == 0) {
                    Log.e("CARDREADER", "Mag");
                    return Mag;
                }
            }

            if (isSupportIC) {
                ret = EmvService.IccCheckCard(300);
                DefaultAppCapk.Log("IccCheckCard:" + ret);
                if (ret == 0) {
                    Log.e("CARDREADER", "IC");
                    return IC;
                }
            }

            if (isSupportNfc) {
                ret = EmvService.NfcCheckCard(1000);
                DefaultAppCapk.Log("NfcCheckCard:" + ret);
                if (ret == 0) {
                    Log.e("CARDREADER", "Nfc");
                    return Nfc;
                }
            }
        }
    }

    //@Override
    public int detectNFC() {
        userCancel = false;
        long j = System.currentTimeMillis();
        int ret = -1;
        while (System.currentTimeMillis() - j < 20 * 1000) {

            if (userCancel == true) {
                return -4;
            }

            ret = EmvService.NfcCheckCard(100);
            if (ret == 0) {
                return 0;
            }
            j++;
        }
        return ret;
    }

    //@Override
    public int detectCardKernel() {
        int ret = -1;
        ret = emvService.NFC_CheckKernelID();
        return ret;
    }

    //  @Override
    public byte[] getBooleanArray(byte b) {
        byte[] array = new byte[8];
        for (int i = 7; i >= 0; i--) {
            array[i] = (byte) (b & 1);
            b = (byte) (b >> 1);
        }
        return array;
    }
}
