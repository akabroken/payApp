package com.isw.payapp.devices.telpo;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.isw.payapp.devices.callbacks.EmvServiceCallback;
import com.isw.payapp.devices.interfaces.IEmvProcessor;
import com.isw.payapp.devices.telpo.configurations.DefaultAppCapk;
import com.isw.payapp.callbacks.IccCardReaderCallBack;
import com.isw.payapp.model.TransactionData;
import com.isw.payapp.terminal.config.TerminalConfig;
import com.telpo.emv.EmvParam;
import com.telpo.emv.EmvService;
import com.telpo.pinpad.PinpadService;

import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TelpoEmvService implements IEmvProcessor {

    private static final String TAG = "TelpoCardReader";
    private Context context;
    private EmvService emvService;
    private TransactionData payData;
    private IccCardReaderCallBack cardReaderCallBack;
    private final EmvServiceCallback classEmvCallBacks;
    private TerminalConfig terminalConfig;
    private final WeakReference<Activity> classActivityRef;

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
    private CountDownLatch transactionLatch;
    private volatile boolean transactionCompleted = false;
    private ExecutorService emvExecutor;

    public TelpoEmvService(Activity classActivity, TransactionData payData, EmvServiceCallback classEmvCallBacks) {
        this.classActivityRef = new WeakReference<>(classActivity);
        this.context = classActivity;
        this.payData = payData;
        this.classEmvCallBacks = classEmvCallBacks;
        this.terminalConfig = new TerminalConfig();
        this.transactionLatch = new CountDownLatch(1);
        this.emvExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void initializeDevice() throws Exception {
        classEmvCallBacks.onDeviceConnected("Please wait...");
        classEmvCallBacks.onLoading("Initializing device...");
    }

    @Override
    public void initializeEmvService() throws Exception {
        classEmvCallBacks.onLoading("Initializing EMV service...");
    }

    @Override
    public void startEmvService() throws Exception {
        Log.d(TAG, "Starting EMV service");
        classEmvCallBacks.onLoading("Starting payment process...");

        // Run EMV process in background thread to avoid blocking UI
        emvExecutor.execute(() -> {
            try {
                runEmvProcess();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "EMV process failed:", e);
                if (classEmvCallBacks != null) {
                    classEmvCallBacks.onError("EMV process failed: " + e.getMessage());
                }
                cleanupResources();
            }
        });
    }

    private void runEmvProcess() throws Exception {
        // Initialize EMV Service
        emvService = EmvService.getInstance();
        ret = emvService.Open(context);
        Log.i(TAG,"RET VALUE :"+ret);
        if (ret != EmvService.EMV_TRUE) {//
            throw new Exception("EMV service initialization failed with code: " + ret);
        }

        // Initialize EMV Device
        ret = emvService.deviceOpen();
        if (ret != 0) {
            throw new Exception("EMV device initialization failed with code: " + ret);
        }

        // Initialize PIN Pad
        ret = PinpadService.Open(context);
        if (ret == PinpadService.PIN_ERROR_NEED_TO_FOMRAT) {
            PinpadService.TP_PinpadFormat(context);
            ret = PinpadService.Open(context);
        }
        if (ret != 0) {
            throw new Exception("PIN pad initialization failed with code: " + ret);
        }

        // Configure EMV
        classEmvCallBacks.onLoading("Configuring payment terminal...");
        emvService.Emv_SetDebugOn(1);
        emvService.Emv_RemoveAllApp();
        DefaultAppCapk.Add_All_APP();
        emvService.Emv_RemoveAllCapk();
        DefaultAppCapk.Add_All_CAPK();

        openDevice();

        // Start card detection
        classEmvCallBacks.onLoading("Waiting for card...");
        classEmvCallBacks.onWaitingStatusChanged(true);

        // Detect card type
        currentEvent = detectCard();
        Log.i(TAG, "Detected card event: " + currentEvent);

        if (currentEvent == ICC) {
            processICCard();
        } else if (currentEvent == NFC) {
            processNFCCard();
        } else if (currentEvent == MAG_STRIPE) {
            processMagneticStripe();
        } else {
            throw new Exception("No card detected or card type not supported");
        }
    }

    private void processICCard() throws Exception {
        Log.d(TAG, "Processing IC card transaction");
        classEmvCallBacks.onLoading("Processing IC card...");

//        cardReaderCallBack = new IccCardReaderCallBack(context, emvService, payData, ICC);
        cardReaderCallBack = new IccCardReaderCallBack(context, emvService, payData, ICC, classEmvCallBacks);
        emvService.setListener(cardReaderCallBack);

        // Power on ICC card
        classEmvCallBacks.onLoading("Powering on card...");
        ret = emvService.IccCard_Poweron();
        if (ret != 0) {
            throw new Exception("ICC card power on failed with code: " + ret);
        }

        // Initialize transaction
        classEmvCallBacks.onLoading("Initializing transaction...");
        ret = emvService.Emv_TransInit();

        payData.setCardType("IC");

        // Configure EMV parameters
        classEmvCallBacks.onLoading("Configuring transaction...");
        ret = configureEmvParameters();
        if (ret != 0) {
            throw new Exception("EMV configuration failed with code: " + ret);
        }

        // Start application
        classEmvCallBacks.onLoading("Starting application...");
        ret = emvService.Emv_StartApp(EmvService.EMV_FALSE);

        if (ret != EmvService.EMV_TRUE) {
            throw new Exception("EMV application start failed with code: " + ret);
        }

        classEmvCallBacks.onLoading("Processing transaction...");

        // Wait for transaction completion with timeout
        boolean completed = transactionLatch.await(120, TimeUnit.SECONDS);
        if (!completed) {
            throw new Exception("Transaction timeout - no response within 120 seconds");
        }


        if (transactionData.equals("00")||transactionData.equals("01")) {
            transactionCompleted = true;
            transactionData = cardReaderCallBack.getKimonoData();
            Log.d(TAG, "Transaction completed successfully");
            classEmvCallBacks.onStopLoading();

        } else {
            throw new Exception("Transaction failed or was cancelled");
        }
    }

    private void processNFCCard() throws Exception {
        classEmvCallBacks.onError("NFC card processing not yet implemented");
        throw new Exception("NFC card processing not yet implemented");
    }

    private void processMagneticStripe() throws Exception {
        classEmvCallBacks.onError("Magnetic stripe processing not yet implemented");
        throw new Exception("Magnetic stripe processing not yet implemented");
    }

    @Override
    public String getResponse() {
        return transactionData;
    }

    @Override
    public void cancelTransaction() {
        Log.d(TAG, "Cancelling transaction");
        userCancel = true;
        transactionCompleted = false;
        classEmvCallBacks.onStopLoading();

        if (transactionLatch != null && transactionLatch.getCount() > 0) {
            transactionLatch.countDown();
        }

        if (emvExecutor != null && !emvExecutor.isShutdown()) {
            emvExecutor.shutdownNow();
        }

        try {
            if (currentEvent == ICC) {
                emvService.IccCard_Poweroff();
            }

            if (emvService != null) {
                emvService.IccCloseReader();
                emvService.MagStripeCloseReader();
                emvService.NfcCloseReader();
                emvService.deviceClose();
            }

            PinpadService.Close();

        } catch (Exception e) {
            Log.e(TAG, "Error during transaction cancellation", e);
        }

        if (classEmvCallBacks != null) {
            classEmvCallBacks.onTransactionCancelled();
        }
    }

    @Override
    public void setViews(EditText pinpadEditText, View scvText, View tvReceipt, View btnSendReceipt) {
        // UI view setup if needed
    }

    public void openDevice() {
        Log.d(TAG, "Opening card readers");
        if (isSupportMag) {
            ret = emvService.MagStripeOpenReader();
            Log.d(TAG, "Magnetic stripe reader opened: " + (ret == 0 ? "Success" : "Failed"));
        }

        if (isSupportIC) {
            ret = emvService.IccOpenReader();
            Log.d(TAG, "ICC reader opened: " + (ret == 0 ? "Success" : "Failed"));
        }

        if (isSupportNfc) {
            ret = emvService.NfcOpenReader(1000);
            Log.d(TAG, "NFC reader opened: " + (ret == 0 ? "Success" : "Failed"));
        }
    }

    private int detectCard() {
        Log.d(TAG, "Starting card detection");
        int detectionTimeout = 60000; // 60 seconds timeout
        long startTime = System.currentTimeMillis();
        long lastProgressUpdate = 0;

        while (!userCancel && (System.currentTimeMillis() - startTime) < detectionTimeout) {
            // Update progress every 2 seconds
            if (System.currentTimeMillis() - lastProgressUpdate > 2000) {
                classEmvCallBacks.onLoading("Please insert or tap card...");
                lastProgressUpdate = System.currentTimeMillis();
            }

            if (isSupportMag && emvService.MagStripeCheckCard(1000) == 0) {
                Log.d(TAG, "Magnetic stripe card detected");
                classEmvCallBacks.onWaitingStatusChanged(false);
                return MAG_STRIPE;
            }

            if (isSupportIC && emvService.IccCheckCard(300) == 0) {
                Log.d(TAG, "ICC card detected");
                classEmvCallBacks.onWaitingStatusChanged(false);
                return ICC;
            }

            if (isSupportNfc && emvService.NfcCheckCard(1000) == 0) {
                Log.d(TAG, "NFC card detected");
                classEmvCallBacks.onWaitingStatusChanged(false);
                return NFC;
            }

            // Small delay to prevent busy waiting
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        classEmvCallBacks.onWaitingStatusChanged(false);
        Log.w(TAG, "Card detection timeout or cancelled");
        return -1;
    }

    private int configureEmvParameters() {
        try {
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

            Log.d(TAG, "EMV parameters configured successfully");
            return 0;
        } catch (Exception e) {
            Log.e(TAG, "EMV parameter configuration failed", e);
            return -1;
        }
    }

    private void cleanupResources() {
        Log.d(TAG, "Cleaning up resources");
        classEmvCallBacks.onStopLoading();

        try {
            if (emvExecutor != null && !emvExecutor.isShutdown()) {
                emvExecutor.shutdownNow();
            }

            if (emvService != null) {
                if (currentEvent == ICC) {
                    emvService.IccCard_Poweroff();
                }
                emvService.IccCloseReader();
                emvService.MagStripeCloseReader();
                emvService.NfcCloseReader();
                emvService.deviceClose();
            }
            PinpadService.Close();
        } catch (Exception e) {
            Log.e(TAG, "Error during resource cleanup", e);
        }
    }

    // Callback method for the card reader to notify completion
    public void onTransactionCompleted(boolean success, String responseData) {
        Log.d(TAG, "Transaction completed - Success: " + success);
        this.transactionCompleted = success;
        this.transactionData = responseData;

        if (transactionLatch != null && transactionLatch.getCount() > 0) {
            transactionLatch.countDown();
        }

        // Notify the main callback
        if (classEmvCallBacks != null) {
            classEmvCallBacks.onStopLoading();
            if (success) {
                classEmvCallBacks.onTransactionSuccess(responseData);
            } else {
                classEmvCallBacks.onError("Transaction failed");
            }
        }

        // Cleanup after transaction
        new Thread(this::cleanupResources).start();
    }
}