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
import com.isw.payapp.helpers.ConfigManager;
import com.isw.payapp.model.TerminalConfigModel;
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
import java.util.concurrent.atomic.AtomicBoolean;

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
    private final AtomicBoolean userCancel = new AtomicBoolean(false);
    private boolean isSupportIC = true;
    private boolean isSupportMag = true;
    private boolean isSupportNfc = true;

    private static final int MAG_STRIPE = 0;
    private static final int ICC = 1;
    private static final int NFC = 2;

    private int ret;
    private String transactionData;
    private CountDownLatch transactionLatch;
    private final AtomicBoolean transactionCompleted = new AtomicBoolean(false);
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private ExecutorService emvExecutor;

    // Timeout constants
    private static final int CARD_DETECTION_TIMEOUT_MS = 60000; // 60 seconds
    private static final int TRANSACTION_TIMEOUT_MS = 120000; // 120 seconds
    private static final int PROGRESS_UPDATE_INTERVAL_MS = 2000; // 2 seconds

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
        Log.d(TAG, "Initializing device");
        classEmvCallBacks.onDeviceConnected("Please wait...");
        classEmvCallBacks.onLoading("Initializing device...");
    }

    @Override
    public void initializeEmvService() throws Exception {
        Log.d(TAG, "Initializing EMV service");
        classEmvCallBacks.onLoading("Initializing EMV service...");
    }

    @Override
    public void startEmvService() throws Exception {
        Log.d(TAG, "Starting EMV service");

        if (isProcessing.getAndSet(true)) {
            Log.w(TAG, "EMV service already running");
            return;
        }

        classEmvCallBacks.onLoading("Starting payment process...");

        // Run EMV process in background thread to avoid blocking UI
        emvExecutor.execute(() -> {
            try {
                runEmvProcess();
            } catch (Exception e) {
                Log.e(TAG, "EMV process failed", e);
                handleError("EMV process failed: " + e.getMessage());
            } finally {
                isProcessing.set(false);
                cleanupResources();
            }
        });
    }

    private void runEmvProcess() throws Exception {
        // Reset state for new transaction
        userCancel.set(false);
        transactionCompleted.set(false);
        transactionData = null;

        // Initialize EMV Service
        emvService = EmvService.getInstance();
        ret = emvService.Open(context);
        Log.i(TAG, "EMV Service Open result: " + ret);
        if (ret != EmvService.EMV_TRUE) {
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
            Log.w(TAG, "PIN pad needs formatting, formatting now...");
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

        // Create callback with proper reference to this service
        cardReaderCallBack = new IccCardReaderCallBack(context, emvService, payData, ICC, classEmvCallBacks);
        cardReaderCallBack.setCardReader(this);
        cardReaderCallBack.setTransactionLatch(transactionLatch);
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
        if (ret != 1) {
            throw new Exception("Transaction initialization failed with code: " + ret);
        }

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

        Log.d(TAG, "Waiting for transaction completion...");

        // Wait for transaction completion with timeout
        boolean completed = transactionLatch.await(TRANSACTION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        if (!completed) {
            throw new Exception("Transaction timeout - no response within " + (TRANSACTION_TIMEOUT_MS / 1000) + " seconds");
        }

        if (transactionCompleted.get()) {
            Log.d(TAG, "Transaction completed successfully");
            classEmvCallBacks.onStopLoading();
        } else {
            throw new Exception("Transaction failed or was cancelled");
        }
    }

    private void processNFCCard() throws Exception {
        Log.w(TAG, "NFC card processing not yet implemented");
        classEmvCallBacks.onError("NFC card processing not yet implemented");
        throw new Exception("NFC card processing not yet implemented");
    }

    private void processMagneticStripe() throws Exception {
        Log.w(TAG, "Magnetic stripe processing not yet implemented");
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
        userCancel.set(true);
        transactionCompleted.set(false);

        // Release the latch to unblock waiting threads
        if (transactionLatch != null && transactionLatch.getCount() > 0) {
            transactionLatch.countDown();
        }

        // Stop EMV processing
        if (emvExecutor != null && !emvExecutor.isShutdown()) {
            emvExecutor.shutdownNow();
        }

        // Close hardware resources
        closeHardwareResources();

        // Notify callback
        if (classEmvCallBacks != null) {
            classEmvCallBacks.onStopLoading();
            classEmvCallBacks.onTransactionCancelled();
        }

        Log.i(TAG, "Transaction cancelled successfully");
    }

    private void closeHardwareResources() {
        Log.d(TAG, "Closing hardware resources");
        try {
            if (currentEvent == ICC && emvService != null) {
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
            Log.e(TAG, "Error during hardware resource closure", e);
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
        long startTime = System.currentTimeMillis();
        long lastProgressUpdate = 0;

        while (!userCancel.get() && (System.currentTimeMillis() - startTime) < CARD_DETECTION_TIMEOUT_MS) {
            // Update progress every 2 seconds
            if (System.currentTimeMillis() - lastProgressUpdate > PROGRESS_UPDATE_INTERVAL_MS) {
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
                Log.d(TAG, "Card detection interrupted");
                Thread.currentThread().interrupt();
                break;
            }
        }

        classEmvCallBacks.onWaitingStatusChanged(false);
        if (userCancel.get()) {
            Log.w(TAG, "Card detection cancelled by user");
        } else {
            Log.w(TAG, "Card detection timeout");
        }
        return -1;
    }

    private int configureEmvParameters() {
        try {
            EmvParam emvParam = new EmvParam();
            ConfigManager.refreshConfig(context);
            TerminalConfigModel config = ConfigManager.getConfig(context);

            emvParam.MerchName = config.getMerchantloc().getBytes();
            emvParam.MerchId = config.getMid().getBytes();
            emvParam.TermId = config.getTid().getBytes();
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

        try {
            // Close hardware resources
            closeHardwareResources();

            // Shutdown executor
            if (emvExecutor != null && !emvExecutor.isShutdown()) {
                emvExecutor.shutdownNow();
            }

            // Ensure latch is released
            if (transactionLatch != null && transactionLatch.getCount() > 0) {
                transactionLatch.countDown();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error during resource cleanup", e);
        }
    }

    // Callback method for the card reader to notify completion
    public void onTransactionCompleted(boolean success, String responseData) {
        Log.d(TAG, "onTransactionCompleted - Success: " + success + ", Data: " + responseData);

        this.transactionCompleted.set(success);
        this.transactionData = responseData;

        // Release the latch to unblock waiting threads
        if (transactionLatch != null && transactionLatch.getCount() > 0) {
            transactionLatch.countDown();
        }

        // Notify the main callback
        if (classEmvCallBacks != null) {
            classEmvCallBacks.onStopLoading();
            if (success) {
                classEmvCallBacks.onTransactionSuccess(responseData);
            } else {
                classEmvCallBacks.onError("Transaction failed: " + responseData);
            }
        }

        // Cleanup after transaction
        cleanupResources();
    }

    private void handleError(String errorMessage) {
        Log.e(TAG, errorMessage);
        if (classEmvCallBacks != null) {
            classEmvCallBacks.onStopLoading();
            classEmvCallBacks.onError(errorMessage);
        }
        cleanupResources();
    }

    /**
     * Check if transaction is currently processing
     */
    public boolean isProcessing() {
        return isProcessing.get();
    }

    /**
     * Check if transaction was completed successfully
     */
    public boolean isTransactionCompleted() {
        return transactionCompleted.get();
    }

    /**
     * Check if user cancelled the transaction
     */
    public boolean isUserCancelled() {
        return userCancel.get();
    }
}