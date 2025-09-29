package com.isw.payapp.devices.dspread;

import static com.isw.payapp.utils.DUKPTKeyDerivation.bytesToHex;
import static com.isw.payapp.utils.DUKPTKeyDerivation.hexStringToByteArray;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.dspread.print.util.TRACE;
import com.dspread.xpos.QPOSService;
import com.isw.payapp.R;
import com.isw.payapp.devices.callbacks.EmvServiceCallback;
import com.isw.payapp.devices.dspread.callbacks.IConnectionServiceCallback;
import com.isw.payapp.devices.dspread.callbacks.IPaymentServiceCallback;
import com.isw.payapp.devices.dspread.callbacks.PaymentResult;
import com.isw.payapp.devices.dspread.utils.DUKPK2009_CBC;
import com.isw.payapp.devices.dspread.utils.DeviceUtils;
import com.isw.payapp.devices.dspread.utils.EMVTLVParser;
import com.isw.payapp.devices.dspread.utils.EmvTLVTags;
import com.isw.payapp.devices.dspread.utils.ICCDecryptor;
import com.isw.payapp.devices.dspread.utils.IccTLVDataDecoder;
import com.isw.payapp.devices.dspread.utils.QPOSUtil;
import com.isw.payapp.devices.dspread.utils.TLVParser;
import com.isw.payapp.devices.dspread.utils.XMLUtils;
import com.isw.payapp.devices.interfaces.IEmvProcessor;
import com.isw.payapp.devices.dspread.Activity.pinkeyboard.PinPadDialog;
import com.isw.payapp.devices.dspread.Activity.pinkeyboard.KeyboardUtil;
import com.isw.payapp.devices.dspread.Activity.pinkeyboard.MyKeyboardView;
import com.isw.payapp.devices.services.NetworkService;
import com.isw.payapp.dialog.PrinterPreviewDialog;
import com.isw.payapp.model.CardModel;
import com.isw.payapp.model.EmvModel;
import com.isw.payapp.model.Receipt;
import com.isw.payapp.model.TransactionData;
import com.isw.payapp.devices.dspread.Activity.pinkeyboard.PinPadView;
import com.isw.payapp.devices.dspread.utils.HandleTxnsResultUtils;
import com.isw.payapp.paymentsRequests.KsmgRequest;
import com.isw.payapp.terminal.config.TerminalConfig;
import com.isw.payapp.terminal.processors.GetwayProcessor;
import com.isw.payapp.utils.DUKPTKeyDerivation;
import com.isw.payapp.utils.EmvTlvParser;
import com.isw.payapp.utils.NetworkExecutor;
import com.isw.payapp.utils.ThreeDES;
import com.isw.payapp.utils.TripleDESBackendProcessor;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import me.goldze.mvvmhabit.utils.ToastUtils;

public class DSpreadEmvService implements IEmvProcessor {

    private static final String TAG = "DSpreadEmvService";
    private static final int PIN_INPUT_TIMEOUT_MS = 30000;
    private static final int NETWORK_TIMEOUT_SECONDS = 30;

    private final WeakReference<Activity> classActivityRef;
    private final TransactionData classTransactionData;
    private final EmvServiceCallback classEmvCallBacks;
    private final Handler mainHandler;
    private final ExecutorService transactionExecutor;
    private CardModel cardModel;
    private IPaymentServiceCallback paymentServiceCallback;
    private KeyboardUtil keyboardUtil;
    private PinPadDialog pinPadDialog;
    private PinPadView pinPadView; // Proper PINPAD initialization
    private boolean isChangePin = false;
    private boolean isPinBack = false;
    private int timeOfPinInput = 0;
    private int changePinTimes = 0;

    // UI Components
    private WeakReference<EditText> pinpadEditTextRef;
    private WeakReference<View> scvTextRef;
    private WeakReference<View> tvReceiptRef;
    private WeakReference<View> btnSendReceiptRef;

    private String responseMessage;
    private volatile boolean isTransactionActive = false;
    private NetworkService networkService;

    public DSpreadEmvService(Activity classActivity, TransactionData classTransactionData,
                             EmvServiceCallback classEmvCallBacks) {
        this.classActivityRef = new WeakReference<>(classActivity);
        this.classTransactionData = classTransactionData;
        this.classEmvCallBacks = classEmvCallBacks;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.transactionExecutor = Executors.newSingleThreadExecutor();
        this.responseMessage = "";
        initializePinPad(); // Initialize PINPAD in constructor
    }

    private Activity getActivity() {
        return classActivityRef != null ? classActivityRef.get() : null;
    }

    @Override
    public void setViews(EditText pinpadEditText, View scvText, View tvReceipt, View btnSendReceipt) {
        this.pinpadEditTextRef = new WeakReference<>(pinpadEditText);
        this.scvTextRef = new WeakReference<>(scvText);
        this.tvReceiptRef = new WeakReference<>(tvReceipt);
        this.btnSendReceiptRef = new WeakReference<>(btnSendReceipt);
        Log.d(TAG, "Views set successfully");

        // Re-initialize PINPAD with the new views
        initializePinPad();
    }

    // Initialize PINPAD properly
    private void initializePinPad() {
        Activity activity = getActivity();
        if (activity == null) {
            Log.w(TAG, "Activity is null, cannot initialize PINPAD");
            return;
        }

        try {
            // Initialize PinPadView
            pinPadView = new PinPadView(activity);
            pinPadView.setRandomNumber(true); // Enable random number layout

            // Initialize PinPadDialog
            pinPadDialog = new PinPadDialog(activity);
            pinPadDialog.setPinPadView(pinPadView);

            Log.d(TAG, "PINPAD initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing PINPAD: " + e.getMessage());
        }
    }

    // Helper methods to safely get UI components
    private EditText getPinpadEditText() {
        return pinpadEditTextRef != null ? pinpadEditTextRef.get() : null;
    }

    private View getScvText() {
        return scvTextRef != null ? scvTextRef.get() : null;
    }

    @Override
    public void initializeDevice() throws Exception {
        checkPermissions();
        initializePOSManager();
        initializePinPad(); // Ensure PINPAD is initialized
    }

    @Override
    public void initializeEmvService() throws Exception {
        Log.i(TAG, "Initializing EMV service");
        paymentServiceCallback = new PaymentCallback();

        Activity activity = getActivity();
        if (activity == null) {
            throw new IllegalStateException("Activity is null");
        }

        POSManager.init(activity);
        classEmvCallBacks.onLoading("EMV service initialized successfully");
    }

    @Override
    public void startEmvService() throws Exception {
        Log.i(TAG, "Starting EMV service");
        isTransactionActive = true;
        startTransaction();
        classEmvCallBacks.onLoading("Starting transaction process");
    }

    @Override
    public String getResponse() {
        return responseMessage;
    }

    @Override
    public void cancelTransaction() {
        Log.i(TAG, "Cancelling transaction");
        isTransactionActive = false;

        if (transactionExecutor.isShutdown() || transactionExecutor.isTerminated()) {
            Log.w(TAG, "Executor is already shut down. Skipping cancel task.");
            notifyTransactionCancelled();
            return;
        }

        transactionExecutor.execute(() -> {
            try {
                POSManager.getInstance().cancelTransaction();
            } catch (Exception e) {
                Log.e(TAG, "Error cancelling transaction: " + e.getMessage());
            } finally {
                notifyTransactionCancelled();
            }
        });
    }

    public void releaseResources() {
        isTransactionActive = false;

        // Clean up PINPAD resources
        if (pinPadView != null) {
            pinPadView.cleanup();
            pinPadView = null;
        }

        // Shutdown executor gracefully
        if (!transactionExecutor.isShutdown()) {
            transactionExecutor.shutdown();
            try {
                if (!transactionExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    transactionExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                transactionExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Clean up UI components
        mainHandler.post(() -> {
            if (keyboardUtil != null) {
                keyboardUtil.hide();
                keyboardUtil = null;
            }

            if (pinPadDialog != null && pinPadDialog.isShowing()) {
                pinPadDialog.dismiss();
                pinPadDialog = null;
            }
        });
    }

    private void checkPermissions() {
        Activity activity = getActivity();
        if (activity == null) return;

        String[] requiredPermissions = {
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
        };

        List<String> missingPermissions = new ArrayList<>();
        for (String permission : requiredPermissions) {
            if (ActivityCompat.checkSelfPermission(activity, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }

        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(activity,
                    missingPermissions.toArray(new String[0]), 1001);
        }
    }

    private void initializePOSManager() {
        Activity activity = getActivity();
        if (activity != null) {
            POSManager.init(activity);
        }
    }

    private void startTransaction() {
        if (!isTransactionActive) {
            Log.w(TAG, "Transaction not active, skipping start");
            return;
        }

        transactionExecutor.execute(() -> {
            try {
                connectDevice();
//                if (!POSManager.getInstance().isDeviceReady()) {
//                    connectDevice();
//                } else {
//                    executeTransaction();
//                }
            } catch (Exception e) {
                notifyError("Transaction start failed: " + e.getMessage());
            }
        });
    }

    private void connectDevice() {
        POSManager.getInstance().connect("", new IConnectionServiceCallback() {
            @Override
            public void onRequestNoQposDetected() {
                notifyError("No QPOS device detected");
            }

            @Override
            public void onRequestQposConnected() {
                notifyDeviceConnected();
                executeTransaction();
            }

            @Override
            public void onRequestQposDisconnected() {
                try {
                    Activity activity = getActivity();
                    if (activity != null) {
                        POSManager.init(activity);
                    }
                    POSManager.getInstance().close();
                    POSManager.getInstance().unregisterCallbacks();
                } catch (Exception e) {
                    Log.e(TAG, "Error during disconnect cleanup: " + e.getMessage());
                }
                notifyDeviceDisconnected();
            }
        });
    }

    private void executeTransaction() {
        if (!isTransactionActive) {
            Log.w(TAG, "Transaction cancelled, skipping execution");
            return;
        }

        Log.i(TAG, "Executing transaction");
        classEmvCallBacks.onLoading("Processing transaction");

        try {
            POSManager.getInstance().startTransaction(
                    classTransactionData.getAmount(),
                    paymentServiceCallback
            );
        } catch (Exception e) {
            notifyError("Transaction execution failed: " + e.getMessage());
        }
    }

    private void notifyDeviceConnected() {
        mainHandler.post(() -> {
            ToastUtils.showLong("Device connected successfully");
            classEmvCallBacks.onDeviceConnected("Device connected");
        });
    }

    private void notifyDeviceDisconnected() {
        mainHandler.post(() -> {
            ToastUtils.showLong("Device disconnected");
            classEmvCallBacks.onDeviceDisconnected("Device disconnected");
        });
    }

    private void notifyError(String message) {
        TRACE.e("KENLOGS::" + message);
        mainHandler.post(() -> classEmvCallBacks.onError(message));
    }

    private void notifyTransactionCancelled() {
        mainHandler.post(() -> {
            try {
                if (POSManager.getInstance().isDeviceReady()) {
                    POSManager.getInstance().cancelTransaction();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in cancel transaction: " + e.getMessage());
            }
            classEmvCallBacks.onTransactionCancelled();
        });
    }

    private void setupPinPad(List<String> dataList) {
        mainHandler.post(() -> {
            try {
                Activity activity = getActivity();
                if (activity == null) return;

                if (keyboardUtil != null) {
                    keyboardUtil.hide();
                }

                EditText pinpadEditText = getPinpadEditText();
                View scvText = getScvText();

                if (pinpadEditText != null) {
                    pinpadEditText.setText("");
                    pinpadEditText.setVisibility(View.VISIBLE);
                    pinpadEditText.requestFocus();
                    pinpadEditText.setGravity(Gravity.CENTER);
                    pinpadEditText.setBackgroundColor(Color.WHITE);
                    pinpadEditText.setTextColor(Color.BLUE);
                }

                MyKeyboardView.setKeyBoardListener(value -> {
                    if (POSManager.getInstance().isDeviceReady()) {
                        TRACE.d("PIN VALUE::" + value);
                        POSManager.getInstance().pinMapSync(value, 20);
                    }
                });

                if (POSManager.getInstance().isDeviceReady() && scvText != null && pinpadEditText != null) {
                    keyboardUtil = new KeyboardUtil(activity, scvText, dataList);
                    keyboardUtil.initKeyboard(MyKeyboardView.KEYBOARDTYPE_Only_Num_Pwd, pinpadEditText);
                } else {
                    notifyError("PIN pad initialization failed - required views not available");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error setting up PIN pad: " + e.getMessage());
                notifyError("PIN pad setup failed");
            }
        });
    }

    private class PaymentCallback implements IPaymentServiceCallback {

        @Override
        public void onRequestWaitingUser() {
            mainHandler.post(() -> {
                classEmvCallBacks.onWaitingStatusChanged(true);
                classEmvCallBacks.onLoading("Waiting for user action");
            });
        }

        @Override
        public void onRequestTime() {
            String terminalTime = new SimpleDateFormat("yyyyMMddHHmmss")
                    .format(Calendar.getInstance().getTime());
            TRACE.d("onRequestTime: " + terminalTime);
            classEmvCallBacks.onLoading("Time requested: " + terminalTime);

            try {
                POSManager.getInstance().sendTime(terminalTime);
            } catch (Exception e) {
                Log.e(TAG, "Error sending time: " + e.getMessage());
            }
        }

        @Override
        public void onRequestSelectEmvApp(ArrayList<String> appList) {
            TRACE.d("onRequestSelectEmvApp(): " + appList.toString());
            mainHandler.post(() -> showEmvAppSelectionDialog(appList));
        }

        private void showEmvAppSelectionDialog(ArrayList<String> appList) {
            Activity activity = getActivity();
            if (activity == null || activity.isFinishing()) return;

            try {
                Dialog dialog = new Dialog(activity);
                dialog.setContentView(R.layout.emv_app_dialog);
                dialog.setTitle(R.string.please_select_app);

                String[] appNameList = appList.toArray(new String[0]);
                ListView appListView = dialog.findViewById(R.id.appList);
                appListView.setAdapter(new ArrayAdapter<>(
                        activity, android.R.layout.simple_list_item_1, appNameList));

                appListView.setOnItemClickListener((parent, view, position, id) -> {
                    try {
                        POSManager.getInstance().selectEmvApp(position);
                        TRACE.d("select emv app position = " + position);
                    } catch (Exception e) {
                        Log.e(TAG, "Error selecting EMV app: " + e.getMessage());
                    }
                    dialog.dismiss();
                });

                dialog.findViewById(R.id.cancelButton).setOnClickListener(v -> {
                    try {
                        POSManager.getInstance().cancelSelectEmvApp();
                    } catch (Exception e) {
                        Log.e(TAG, "Error cancelling EMV app selection: " + e.getMessage());
                    }
                    dialog.dismiss();
                });

                dialog.show();
            } catch (Exception e) {
                Log.e(TAG, "Error showing EMV app dialog: " + e.getMessage());
                // Auto-select first app as fallback
                if (!appList.isEmpty()) {
                    try {
                        POSManager.getInstance().selectEmvApp(0);
                    } catch (Exception ex) {
                        Log.e(TAG, "Error auto-selecting EMV app: " + ex.getMessage());
                    }
                }
            }
        }

        @Override
        public void onQposRequestPinResult(List<String> dataList, int offlineTime) {
            TRACE.d("onQposRequestPinResult = " + dataList + "\nofflineTime: " + offlineTime);
            mainHandler.post(() -> handlePinRequest(dataList, offlineTime));
        }

        private void handlePinRequest(List<String> dataList, int offlineTime) {
            TRACE.d("handlePinRequest() " + offlineTime);
            if (!POSManager.getInstance().isDeviceReady()) return;

            classEmvCallBacks.onStopLoading();
            setupPinPad(dataList);
        }

        @Override
        public void onRequestSetPin(boolean isOfflinePin, int tryNum) {
            TRACE.d("onRequestSetPin = " + isOfflinePin + "\ntryNum: " + tryNum);
            mainHandler.post(() -> handleSetPinRequest(isOfflinePin, tryNum));
        }

        private void handleSetPinRequest(boolean isOfflinePin, int tryNum) {
            Activity activity = getActivity();
            if (activity == null) return;

            isPinBack = true;
            classEmvCallBacks.onShowPinPad(true);

            String message;
            if (POSManager.getInstance().getTransType() == QPOSService.TransactionType.UPDATE_PIN) {
                changePinTimes++;
                if (changePinTimes == 1) {
                    message = activity.getString(R.string.input_pin_old);
                } else if (changePinTimes == 2 || changePinTimes == 4) {
                    message = activity.getString(R.string.input_pin_new);
                } else {
                    message = activity.getString(R.string.input_new_pin_confirm);
                }
            } else {
                message = isOfflinePin ?
                        activity.getString(R.string.input_offlinePin) :
                        activity.getString(R.string.input_onlinePin);
            }

            classEmvCallBacks.onTitleTextChanged(message);
        }

        @Override
        public void onRequestSetPin() {
            TRACE.i("onRequestSetPin()");
            mainHandler.post(this::showPinPadDialog);
        }

        private void showPinPadDialog() {
            Activity activity = getActivity();
            if (activity == null) return;

            try {
                classEmvCallBacks.onTitleTextChanged(activity.getString(R.string.input_pin));

                // Ensure PINPAD is initialized
                if (pinPadDialog == null) {
                    initializePinPad();
                }

                if (pinPadDialog == null) {
                    throw new IllegalStateException("PINPAD dialog not initialized");
                }

                pinPadDialog.getPayViewPass()
                        .setRandomNumber(true)
                        .setPayClickListener(POSManager.getInstance().getQPOSService(),
                                new PinPadView.OnPayClickListener() {
                                    @Override
                                    public void onCencel() {
                                        try {
                                            POSManager.getInstance().cancelPin();
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error cancelling PIN: " + e.getMessage());
                                        }
                                        pinPadDialog.dismiss();
                                    }

                                    @Override
                                    public void onPaypass() {
                                        try {
                                            POSManager.getInstance().bypassPin();
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error bypassing PIN: " + e.getMessage());
                                        }
                                        pinPadDialog.dismiss();
                                    }

                                    @Override
                                    public void onConfirm(String password) {
                                        try {
                                            String pinBlock = QPOSUtil.buildCvmPinBlock(
                                                    POSManager.getInstance().getEncryptData(), password);
                                            TRACE.d("PIN BLOCK-->" + pinBlock);
                                            POSManager.getInstance().sendCvmPin(pinBlock, true);
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error processing PIN: " + e.getMessage());
                                        }
                                        pinPadDialog.dismiss();
                                    }
                                });

                pinPadDialog.show();
            } catch (Exception e) {
                Log.e(TAG, "Error showing PIN pad dialog: " + e.getMessage());
                notifyError("PIN input failed: " + e.getMessage());
            }
        }

        @Override
        public void onGetCardNoResult(String cardNo) {
            TRACE.d("onGetCardNoResult(String cardNo):" + cardNo);
        }

        @Override
        public void onRequestBatchData(String tlv) {
            TRACE.d("onRequestBatchData()");
            EditText pinpadEditText = getPinpadEditText();
            if (pinpadEditText != null) {
                pinpadEditText.setVisibility(View.GONE);
            }

            TRACE.d("ICC trade finished");
            String content = "Batch data: " + tlv;
            TRACE.d("ICC trade finished +" + content);
        }

        @Override
        public void onRequestDisplay(QPOSService.Display displayMsg) {
            TRACE.d("onRequestDisplay(Display displayMsg):" + displayMsg.toString());
            mainHandler.post(() -> handleDisplayRequest(displayMsg));
        }

        private void handleDisplayRequest(QPOSService.Display displayMsg) {
            Activity activity = getActivity();
            if (activity == null) return;

            String message = HandleTxnsResultUtils.getDisplayMessage(displayMsg, activity);

            if (displayMsg == QPOSService.Display.MSR_DATA_READY) {
                showMsrDataReadyDialog();
            } else if (displayMsg == QPOSService.Display.INPUT_NEW_PIN) {
                isChangePin = true;
                timeOfPinInput++;
            } else if (displayMsg == QPOSService.Display.INPUT_NEW_PIN_CHECK_ERROR) {
                message = activity.getString(R.string.input_new_pin_check_error);
                timeOfPinInput = 0;
            }

            classEmvCallBacks.onLoading(message);
        }

        private void showMsrDataReadyDialog() {
            Activity activity = getActivity();
            if (activity == null || activity.isFinishing()) return;

            try {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle("Audio");
                builder.setMessage("Success, Continue ready");
                builder.setPositiveButton("Confirm", null);
                builder.show();
            } catch (Exception e) {
                Log.e(TAG, "Error showing MSR dialog: " + e.getMessage());
            }
        }

        @Override
        public void onTransactionCompleted(PaymentResult result) {
            mainHandler.post(() -> handleTransactionCompleted(result));
        }

        private void handleTransactionCompleted(PaymentResult result) {
            isTransactionActive = false;
            classEmvCallBacks.onShowPinPad(false);
            isChangePin = false;

            String transType = result.getTransactionType();
            if (transType != null) {
                if (QPOSService.DoTradeResult.MCR.name().equals(transType)) {
                    handleMCRResult(result);
                } else if (QPOSService.DoTradeResult.NFC_OFFLINE.name().equals(transType) ||
                        QPOSService.DoTradeResult.NFC_ONLINE.name().equals(transType)) {
                    handleNFCResult(result);
                } else {
                    handleICCResult(result);
                }
            }
        }

        private void handleICCResult(PaymentResult result) {
            Activity activity = getActivity();
            if (activity == null) return;

            String content = activity.getString(R.string.batch_data) + result.getTlv();
            TRACE.d("handleICCResult() " + content);

            classEmvCallBacks.onTransactionSuccess(content);
        }

        private void handleMCRResult(PaymentResult result) {
            classEmvCallBacks.onTransactionSuccess("Magnetic card transaction completed");
        }

        private void handleNFCResult(PaymentResult result) {
            classEmvCallBacks.onTransactionSuccess("NFC transaction completed");
        }

        @Override
        public void onRequestTransactionResult(QPOSService.TransactionResult transactionResult) {
            Activity activity = getActivity();
            if (activity == null) return;

            String message = HandleTxnsResultUtils.getTransactionResultMessage(transactionResult, activity);
            TRACE.d("onRequestTransactionResult: " + message);

            String msg = "";
            if (transactionResult == QPOSService.TransactionResult.APPROVED) {
                TRACE.d("TransactionResult.APPROVED");
            } else if (transactionResult == QPOSService.TransactionResult.TERMINATED) {
                msg = "Transaction terminated";
            } else if (transactionResult == QPOSService.TransactionResult.DECLINED) {
                msg = "Transaction declined";
            } else if (transactionResult == QPOSService.TransactionResult.CANCEL) {
                msg = "Transaction cancelled";
            } else if (transactionResult == QPOSService.TransactionResult.CAPK_FAIL) {
                msg = "CAPK failed";
            } else if (transactionResult == QPOSService.TransactionResult.NOT_ICC) {
                msg = "Not ICC card";
            } else if (transactionResult == QPOSService.TransactionResult.SELECT_APP_FAIL) {
                msg = "App selection failed";
            } else if (transactionResult == QPOSService.TransactionResult.DEVICE_ERROR) {
                msg = "Device error";
            } else if (transactionResult == QPOSService.TransactionResult.TRADE_LOG_FULL) {
                msg = "Trade log full";
            } else if (transactionResult == QPOSService.TransactionResult.CARD_NOT_SUPPORTED) {
                msg = "Card not supported";
            } else if (transactionResult == QPOSService.TransactionResult.MISSING_MANDATORY_DATA) {
                msg = "Missing mandatory data";
            } else if (transactionResult == QPOSService.TransactionResult.CARD_BLOCKED_OR_NO_EMV_APPS) {
                msg = "Card blocked or no EMV apps";
            } else if (transactionResult == QPOSService.TransactionResult.INVALID_ICC_DATA) {
                msg = "Invalid ICC data";
            } else if (transactionResult == QPOSService.TransactionResult.FALLBACK) {
                msg = "Transaction fallback";
                try {
                    POSManager.getInstance().updateEMVConfig("emv_profile_tlv.xml");
                    TRACE.d("EMV Updated");
                } catch (Exception e) {
                    Log.e(TAG, "Error updating EMV config: " + e.getMessage());
                }
            } else if (transactionResult == QPOSService.TransactionResult.NFC_TERMINATED) {
                msg = "NFC Terminated";
            } else if (transactionResult == QPOSService.TransactionResult.CARD_REMOVED) {
                msg = "Card removed";
            } else if (transactionResult == QPOSService.TransactionResult.CONTACTLESS_TRANSACTION_NOT_ALLOW) {
                msg = "Contactless transaction not allowed";
            } else if (transactionResult == QPOSService.TransactionResult.CARD_BLOCKED) {
                msg = "Card blocked";
            } else if (transactionResult == QPOSService.TransactionResult.TRANS_TOKEN_INVALID) {
                msg = "Token invalid";
            } else if (transactionResult == QPOSService.TransactionResult.APP_BLOCKED) {
                msg = "App blocked";
            } else {
                msg = transactionResult.name();
            }
        }

        @Override
        public void onTransactionFailed(String errorMessage, String data) {
            mainHandler.post(() -> handleTransactionFailed(errorMessage, data));
        }

        private void handleTransactionFailed(String errorMessage, String data) {
            isTransactionActive = false;
            classEmvCallBacks.onShowPinPad(false);

            if (keyboardUtil != null) {
                keyboardUtil.hide();
            }

            classEmvCallBacks.onTransactionFailed(errorMessage != null ?
                    errorMessage : "Transaction failed");
        }

        @Override
        public void onRequestOnlineProcess(final String tlv) {
            TRACE.d("onRequestOnlineProcess::\n" + tlv);
            transactionExecutor.execute(() -> handleOnlineProcessRequest(tlv));
        }

        @Override
        public void onReturnGetPinResult(Hashtable<String, String> result) {
            TRACE.d("onReturnGetPinResult(Hashtable<String, String> result):" + result.toString());
            String pinBlock = result.get("pinBlock");
            String pinKsn = result.get("pinKsn");
            String content = "get pin result\n";
            content += "pinKsn: " + pinKsn + "\n";
            content += "pinBlock: " + pinBlock + "\n";
            TRACE.i(content);
        }

        @Override
        public void onReturnCustomConfigResult(boolean isSuccess, String result) {
            TRACE.d("onReturnCustomConfigResult " + "isSuccess:" + isSuccess + " result:" + result);
        }

        @Override
        public void onRequestUpdateWorkKeyResult(QPOSService.UpdateInformationResult result) {
            TRACE.d("onRequestUpdateWorkKeyResult(UpdateInformationResult result):" + result);
        }

        @Override
        public void onReturnUpdateIPEKResult(boolean arg0) {
            TRACE.d("onReturnUpdateIPEKResult(boolean arg0):" + arg0);
        }

        private void handleOnlineProcessRequest(String tlv) {
            mainHandler.post(() -> {
                classEmvCallBacks.onShowPinPad(false);
                classEmvCallBacks.onLoading("Processing online authorization");
            });

            try {
                Activity activity = getActivity();
                if (activity == null) return;

                Hashtable<String, String> decodeData = POSManager.getInstance().anlysEmvIccData(tlv);
                System.out.println("=== EMV ICC Data ===");
                for (Map.Entry<String, String> entry : decodeData.entrySet()) {
                    System.out.println(entry.getKey() + " = " + entry.getValue());
                }
                System.out.println("====================");

                EMVTLVParser emvtlvParser = new EMVTLVParser();
                String pinBlock = emvtlvParser.extractTag(tlv, EmvTLVTags.ProprietaryC7);
                TRACE.d("pinBlock-C7:" + pinBlock);

                String tagKSNOfOnlineMsg = emvtlvParser.extractTag(tlv, EmvTLVTags.ProprietaryC0);
                TRACE.d("CO:" + tagKSNOfOnlineMsg);
                String tagOnlineMessage = emvtlvParser.extractTag(tlv, EmvTLVTags.ProprietaryC2);
                TRACE.d("C2:" + tagOnlineMessage);

                String clearIccData = null;
                if (!TextUtils.isEmpty(tagKSNOfOnlineMsg) && !TextUtils.isEmpty(tagOnlineMessage)) {
                    clearIccData = DUKPK2009_CBC.getData(tagKSNOfOnlineMsg, tagOnlineMessage, DUKPK2009_CBC.Enum_key.DATA, DUKPK2009_CBC.Enum_mode.CBC);
                    Log.d("clearIccData", clearIccData);
                    System.out.println("====================");
                } else {
                    Log.d("NoClearData", "No Data Found");
                }

                if (clearIccData != null) {
                    Map<String, String> decodedMap = IccTLVDataDecoder.decodeIccData(clearIccData);

                    System.out.println("---- Decoded ICC TLV Contents ----");
                    for (Map.Entry<String, String> entry : decodedMap.entrySet()) {
                        System.out.printf("%-35s : %s%n", EmvTLVTags.decodeTag(entry.getKey()) + "(" + entry.getKey() + ")", entry.getValue());
                    }

                    cardModel = new CardModel();
                    cardModel.setPan(decodedMap.get(EmvTLVTags.PAN));
                    cardModel.setKsn(emvtlvParser.extractTag(tlv, EmvTLVTags.ProprietaryC1));
                    cardModel.setPinBlock(emvtlvParser.extractTag(tlv, EmvTLVTags.ProprietaryC7));

                    EmvModel emvModel = new EmvModel();
                    emvModel.setTrack2data(decodedMap.get(EmvTLVTags.Track2EquivalentData));
                    emvModel.setCarSeqNo(decodedMap.get(EmvTLVTags.PANSequenceNumber));
                    emvModel.setApplicationInterchangeProfile(decodedMap.get(EmvTLVTags.ApplicationInterchangeProfile));
                    emvModel.setAtc(decodedMap.get(EmvTLVTags.ATC));
                    emvModel.setCryptogram(decodedMap.get(EmvTLVTags.ApplicationCryptogram));
                    emvModel.setCryptogramInformationData(decodedMap.get(EmvTLVTags.CryptogramInfoData));
                    emvModel.setCvmResults(decodedMap.get(EmvTLVTags.CVMResults));
                    emvModel.setIssuerApplicationData(decodedMap.get(EmvTLVTags.IssuerApplicationData));
                    emvModel.setTransactionCurrencyCode(decodedMap.get(EmvTLVTags.TransactionCurrencyCode));
                    emvModel.setTerminalVerificationResult(decodedMap.get(EmvTLVTags.TVR));
                    emvModel.setTerminalCountryCode(decodedMap.get(EmvTLVTags.TerminalCountryCode));
                    emvModel.setTerminalType(decodedMap.get(EmvTLVTags.TerminalType));
                    emvModel.setTransactionDate(decodedMap.get(EmvTLVTags.TransactionDate));
                    emvModel.setTransactionType(decodedMap.get(EmvTLVTags.TransactionType));
                    emvModel.setUnpredictableNumber(decodedMap.get(EmvTLVTags.UnpredictableNumber));
                    emvModel.setDedicatedFileName(decodedMap.get(EmvTLVTags.DedicatedFileName));
                    emvModel.setTerminalCapabilities(decodedMap.get(EmvTLVTags.TerminalCapabilities));

                    KsmgRequest pinchangeRequest = new KsmgRequest(emvModel, classTransactionData, cardModel);

                    TRACE.d(pinchangeRequest.Payload());
                    TRACE.d("TTT:" + classTransactionData.getAmount());

                    responseMessage = "Online process initiated: " + decodedMap.toString();
                    processNetworkRequest(pinchangeRequest, emvModel);

                } else {
                    TRACE.d("anlysEmvIccData(tlv): No Clear ICC Data Retrieved");
                    mainHandler.post(() -> {
                        classEmvCallBacks.onStopLoading();
                        classEmvCallBacks.onError("No clear ICC data retrieved");
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Error processing online request: " + e.getMessage());
                mainHandler.post(() -> {
                    classEmvCallBacks.onStopLoading();
                    classEmvCallBacks.onError("Online processing failed: " + e.getMessage());
                });
            }
        }

        private void processNetworkRequest(KsmgRequest pinchangeRequest, EmvModel emvModel) {
            ExecutorService networkExecutor = NetworkExecutor.getExecutor();

            networkExecutor.execute(() -> {
                try {
                    Activity activity = getActivity();
                    if (activity == null) return;

                    String baseUrl = "https://" + TerminalConfig.loadTerminalDataFromJson(activity, "__transip") + ":"
                            + TerminalConfig.loadTerminalDataFromJson(activity, "__transport") + "/";

                    NetworkService.initialize(activity, baseUrl);
                    NetworkService networkService = NetworkService.getInstance();

                    String response = networkService.postPayLoadSync(pinchangeRequest.generatePayload());

                    mainHandler.post(() -> handleNetworkResponse(response, emvModel));

                } catch (Exception e) {
                    Log.e(TAG, "Network error: " + e.getMessage());
                    mainHandler.post(() -> {
                        classEmvCallBacks.onStopLoading();
                        classEmvCallBacks.onError("Network error: " + e.getMessage());
                    });
                }
            });
        }

        private void handleNetworkResponse(String response, EmvModel emvModel) {
            try {
                classEmvCallBacks.onStopLoading();

                String respMessage = XMLUtils.isErrorResponse(response);

                showPrinterPreviewDialog(respMessage, emvModel);

            } catch (Exception e) {
                Log.e(TAG, "Error handling network response: " + e.getMessage());
                classEmvCallBacks.onError("Error processing response");
            }
        }

        private void showPrinterPreviewDialog(String respMessage, EmvModel emvModel) {
            Activity activity = getActivity();
            if (activity == null || activity.isFinishing()) return;

            PrinterPreviewDialog previewDialog = new PrinterPreviewDialog(
                    activity,
                    cardModel,
                    emvModel,
                    classTransactionData,
                    respMessage,
                    new PrinterPreviewDialog.OnPrintClickListener() {
                        @Override
                        public void onPrintClick(String previewContent) {
                            printReceipt(createReceipt(respMessage, emvModel));
                            classEmvCallBacks.onTransactionSuccess(respMessage);
                        }

                        @Override
                        public void onCancelClick() {
                            TRACE.d("Printing cancelled by user");
                        }
                    }
            );

            previewDialog.show();
        }

        private Receipt createReceipt(String respMessage, EmvModel emvModel) {
            Activity activity = getActivity();
            Receipt receipt = new Receipt();

            if (activity != null) {
                receipt.setBank(TerminalConfig.loadTerminalDataFromJson(activity, "__bank"));
                receipt.setMerchant(TerminalConfig.loadTerminalDataFromJson(activity, "__merchantloc"));
                receipt.setTerminalId(TerminalConfig.loadTerminalDataFromJson(activity, "__tid"));
            }

            receipt.setAmount(classTransactionData.getAmount());
            receipt.setCurrency("KES");
            receipt.setDateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            receipt.setTransactionType(classTransactionData.getTransactionType());

            String maskPan = (cardModel.getPan() != null ?
                    cardModel.getPan().substring(0, 6) + "******" +
                            cardModel.getPan().substring(cardModel.getPan().length() - 4) : "N/A");
            receipt.setCardNumber(maskPan);
            receipt.setEntryMode("Chip");
            receipt.setAid(emvModel.getDedicatedFileName());
            receipt.setAtc(emvModel.getAtc());
            receipt.setTvr(emvModel.getTerminalVerificationResult());
            receipt.setResponse(respMessage);

            return receipt;
        }

        private void printReceipt(Receipt receipt) {
            Activity activity = getActivity();
            if (activity == null) return;

            try {
                DSpreadPrinterService printerService = DSpreadPrinterService.getInstance(activity);

                try {
                    if (printerService.isInitialized()) {
                        printerService.printReceipt(receipt);
                    } else {
                        Log.e("PRINTER", "Printer not initialized");
                    }
                } catch (RemoteException e) {
                    Log.e("PRINTER", "Print error: " + e.getMessage());
                }

            } catch (Exception e) {
                TRACE.e("Printing error: " + e.getMessage());
                mainHandler.post(() -> {
                    Toast.makeText(activity, "Printing failed", Toast.LENGTH_SHORT).show();
                });
            }
        }

        @Override
        public void onReturnGetPinInputResult(int num) {
            TRACE.i("onReturnGetPinInputResult  ===" + num);
            mainHandler.post(() -> handlePinInputResult(num));
        }

        private void handlePinInputResult(int num) {
            EditText pinpadEditText = getPinpadEditText();
            if (pinpadEditText != null) {
                if (num == -1) {
                    isPinBack = false;
                    pinpadEditText.setText("");
                    classEmvCallBacks.onShowPinPad(false);

                    if (keyboardUtil != null) {
                        keyboardUtil.hide();
                    }
                } else {
                    StringBuilder s = new StringBuilder();
                    for (int i = 0; i < num; i++) {
                        s.append("*");
                    }
                    pinpadEditText.setText(s.toString());
                    classEmvCallBacks.onPinInputReceived(s.toString());
                }
            } else {
                Log.e(TAG, "pinpadEditText is null in handlePinInputResult");
            }
        }
    }
}