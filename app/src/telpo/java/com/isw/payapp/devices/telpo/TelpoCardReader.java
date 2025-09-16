package com.isw.payapp.devices.telpo;

import android.content.Context;
import android.util.Log;

import com.isw.payapp.R;
import com.isw.payapp.devices.interfaces.PosEmv;
import com.isw.payapp.devices.telpo.configurations.DefaultAppCapk;
import com.isw.payapp.callbacks.IccCardReaderCallBack;
import com.isw.payapp.helpers.XmlHelper;
import com.isw.payapp.model.TransactionData;
import com.isw.payapp.terminal.config.TerminalConfig;
import com.telpo.emv.EmvParam;
import com.telpo.emv.EmvService;
import com.telpo.pinpad.PinpadService;

public class TelpoCardReader implements PosEmv {

    private Context context;
    private EmvService emvService;

    private  TransactionData payData;
    private IccCardReaderCallBack cardReaderCallBack;

    private TerminalConfig terminalConfig;


    private int currentEvent;
    private boolean userCancel = false;
    private boolean isSupportIC = true;
    private boolean isSupportMag = true;
    private boolean isSupportNfc = true;

    private static final int MAG_STRIPE = 0;
    private static final int ICC = 1;
    private static final int NFC = 2;

    private int ret;

    private String transactionData;


    public TelpoCardReader(Context context, TransactionData payData){
         this.context = context;
         this.payData = payData;
         this.terminalConfig = new TerminalConfig();
     }

    @Override
    public void initializeEmvService() throws Exception {

    }

    @Override
    public void startEmvService() throws Exception{
        emvService = EmvService.getInstance();
        ret = emvService.Open(context);
        if (ret != EmvService.EMV_TRUE) {
            throw new Exception("EMV service initialization failed");
        }

        ret = emvService.deviceOpen();
        if (ret != 0) {
            throw new Exception("EMV device initialization failed");
        }

        ret = PinpadService.Open(context);

        if (ret == PinpadService.PIN_ERROR_NEED_TO_FOMRAT) {
            PinpadService.TP_PinpadFormat(context);
            PinpadService.Open((context));
        }
        if (ret != 0) {
            throw new Exception("PIN pad initialization failed");
        }
        emvService.Emv_SetDebugOn(1);
        emvService.Emv_RemoveAllApp();
        DefaultAppCapk.Add_All_APP();
        emvService.Emv_RemoveAllCapk();
        DefaultAppCapk.Add_All_CAPK();
        openDevice();

        currentEvent = detectCard();
        Log.i("Test", "eee->" + currentEvent);

        if (currentEvent == ICC) {

            cardReaderCallBack = new IccCardReaderCallBack(context, emvService, payData, ICC);
            emvService.setListener(cardReaderCallBack);

            ret = emvService.IccCard_Poweron();

            ret = emvService.Emv_TransInit();

            payData.setCardType("IC");

            ret = configureEmvParameters();

            if (ret != 0) {
                throw new Exception("EMV configuration and initialization failed");
            }

            ret = emvService.Emv_StartApp(EmvService.EMV_FALSE);

//            if (ret != 0) {
//                throw new Exception("EMV Application Start failed %%-"+ret);
//            }

            transactionData = cardReaderCallBack.getKimonoData();
            System.out.println(transactionData+"----MMMM");
//            processIccCard();

        } else if (currentEvent == NFC) {
           // return processNfcCard();
        } else {

            // "card_type_not_supported";
        }
    }

    @Override
    public String getRespose() {
        return transactionData;
    }

    @Override
    public void cancelTransaction() {
//        PinpadService.Close();
//        EmvService.deviceClose();
        try {
            if (currentEvent == ICC) {
                EmvService.IccCard_Poweroff();
            }

            emvService.IccCloseReader();
            emvService.MagStripeCloseReader();
            emvService.NfcCloseReader();
            PinpadService.Close();
            emvService.deviceClose();
        } catch (Exception e) {
            Log.e("CardReaderJob", "Error closing resources", e);
        }
    }

    public void openDevice() {
        int ret;
        if (isSupportMag) {
            currentEvent = emvService.MagStripeOpenReader();
        }

        if (isSupportIC) {

            currentEvent = emvService.IccOpenReader();
        }

        if (isSupportNfc) {
            currentEvent = emvService.NfcOpenReader(1000);
        }
    }

    public void deviceClose() {
        int ret;
        if (isSupportMag) {
            ret = EmvService.MagStripeCloseReader();
        }

        if (isSupportIC) {
            if (currentEvent == ICC) {
                ret = EmvService.IccCard_Poweroff();
            }
            ret = EmvService.IccCloseReader();
        }

        if (isSupportNfc) {
            ret = EmvService.NfcCloseReader();
        }
    }

    private int detectCard() {
        while (!userCancel) {
            if (isSupportMag && EmvService.MagStripeCheckCard(1000) == 0) {
                return MAG_STRIPE;
            }

            if (isSupportIC && EmvService.IccCheckCard(300) == 0) {
                return ICC;
            }

            if (isSupportNfc && EmvService.NfcCheckCard(1000) == 0) {
                return NFC;
            }
        }
        return -1;
    }

    private int configureEmvParameters() {
        EmvParam emvParam = new EmvParam();
        emvParam.MerchName = terminalConfig.loadTerminalDataFromJson(context, "__merchantloc").getBytes();
        emvParam.MerchId = terminalConfig.loadTerminalDataFromJson(context, "__mid").getBytes();
        emvParam.TermId = terminalConfig.loadTerminalDataFromJson(context, "__tid").getBytes();
        emvParam.TerminalType = 0x22;
        emvParam.Capability = new byte[]{(byte) 0xE0, (byte) 0x40, (byte) 0xC8};
        emvParam.ExCapability = new byte[]{(byte) 0xE0, 0x00, (byte) 0xF0, (byte) 0xA0, 0x01};
        emvParam.CountryCode = new byte[]{(byte) 0x04, (byte) 0x04};
        emvParam.TransType = 0x00;
        emvService.Emv_SetParam(emvParam);
        emvService.Emv_SetOfflinePinCBenable(1);
        System.currentTimeMillis();
        return 0;
    }


}
