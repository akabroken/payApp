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
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import androidx.core.app.ActivityCompat;

import com.dspread.print.util.TRACE;
import com.dspread.xpos.QPOSService;
import com.isw.payapp.R;
import com.isw.payapp.devices.callbacks.EmvServiceCallback;
import com.isw.payapp.devices.dspread.callbacks.IConnectionServiceCallback;
import com.isw.payapp.devices.dspread.callbacks.IPaymentServiceCallback;
import com.isw.payapp.devices.dspread.callbacks.PaymentResult;
import com.isw.payapp.devices.dspread.utils.DeviceUtils;
import com.isw.payapp.devices.dspread.utils.EMVTLVParser;
import com.isw.payapp.devices.dspread.utils.ICCDecryptor;
import com.isw.payapp.devices.dspread.utils.QPOSUtil;
import com.isw.payapp.devices.dspread.utils.TLVParser;
import com.isw.payapp.devices.interfaces.IEmvProcessor;
import com.isw.payapp.devices.dspread.Activity.pinkeyboard.PinPadDialog;
import com.isw.payapp.devices.dspread.Activity.pinkeyboard.KeyboardUtil;
import com.isw.payapp.devices.dspread.Activity.pinkeyboard.MyKeyboardView;
import com.isw.payapp.model.CardModel;
import com.isw.payapp.model.TransactionData;
import com.isw.payapp.devices.dspread.Activity.pinkeyboard.PinPadView;
import com.isw.payapp.devices.dspread.utils.HandleTxnsResultUtils;
import com.isw.payapp.paymentsRequests.KsmgRequest;
import com.isw.payapp.terminal.processors.GetwayProcessor;
import com.isw.payapp.utils.DUKPTKeyDerivation;
import com.isw.payapp.utils.EmvTlvParser;
import com.isw.payapp.utils.ThreeDES;
import com.isw.payapp.utils.TripleDESBackendProcessor;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import me.goldze.mvvmhabit.utils.ToastUtils;

public class DSpreadEmvService implements IEmvProcessor {

    private static final String TAG = "DSpreadEmvService";
    private static final int PIN_INPUT_TIMEOUT_MS = 30000;

    private final Activity classActivity;
    private final TransactionData classTransactionData;
    private final EmvServiceCallback classEmvCallBacks;
    private final Handler mainHandler;
    private final ExecutorService transactionExecutor;
    private CardModel cardModel;
    private IPaymentServiceCallback paymentServiceCallback;
    private KeyboardUtil keyboardUtil;
    private PinPadDialog pinPadDialog;
    private boolean isChangePin = false;
    private boolean isPinBack = false;
    private int timeOfPinInput = 0;
    private int changePinTimes = 0;

    // UI Components
    private EditText pinpadEditText;
    private View scvText;
    private View tvReceipt;
    private View btnSendReceipt;

    private String responseMessage;
    private boolean isTransactionActive = false;

    public DSpreadEmvService(Activity classActivity, TransactionData classTransactionData,
                             EmvServiceCallback classEmvCallBacks) {
        this.classActivity = classActivity;
        this.classTransactionData = classTransactionData;
        this.classEmvCallBacks = classEmvCallBacks;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.transactionExecutor = Executors.newSingleThreadExecutor();
        this.responseMessage = "";
    }

    @Override
    public void setViews(EditText pinpadEditText, View scvText, View tvReceipt, View btnSendReceipt) {
        this.pinpadEditText = pinpadEditText;
        this.scvText = scvText;
        this.tvReceipt = tvReceipt;
        this.btnSendReceipt = btnSendReceipt;
        Log.d(TAG, "Views set successfully");
    }

    @Override
    public void initializeDevice() throws Exception {
        checkPermissions();
        initializePOSManager();
    }

    @Override
    public void initializeEmvService() throws Exception {
        Log.i(TAG, "Initializing EMV service");
        paymentServiceCallback = new PaymentCallback();

        POSManager.init(classActivity);
        //classEmvCallBacks.onStopLoading();
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
            notifyTransactionCancelled(); // Still notify UI even if executor is dead
            return;
        }
        transactionExecutor.execute(() -> {
            try {
                POSManager.getInstance().cancelTransaction();
            } catch (Exception e) {
                Log.e(TAG, "Error cancelling transaction: " + e.getMessage());
            }
            notifyTransactionCancelled();
        });
    }

    public void releaseResources() {
        isTransactionActive = false;
        transactionExecutor.shutdown();

        if (keyboardUtil != null) {
            keyboardUtil.hide();
            keyboardUtil = null;
        }

        if (pinPadDialog != null && pinPadDialog.isShowing()) {
            pinPadDialog.dismiss();
            pinPadDialog = null;
        }
    }

    private void checkPermissions() {
        String[] requiredPermissions = {
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
        };

        List<String> missingPermissions = new ArrayList<>();
        for (String permission : requiredPermissions) {
            if (ActivityCompat.checkSelfPermission(classActivity, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }

        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(classActivity,
                    missingPermissions.toArray(new String[0]), 1001);
        }
    }

    private void initializePOSManager() {
        POSManager.init(classActivity);
    }

    private void startTransaction() {
        if (!isTransactionActive) {
            Log.w(TAG, "Transaction not active, skipping start");
            return;
        }

        transactionExecutor.execute(() -> {
            try {
                if (!POSManager.getInstance().isDeviceReady()) {
                    connectDevice();
                } else {
                    executeTransaction();
                }
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
                POSManager.init(classActivity);
                POSManager.getInstance().close();
                POSManager.getInstance().unregisterCallbacks();
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
//            POSManager.getInstance().updateEMVConfig("emv_profile_tlv.xml");
            String ipektw = "33707E4927C4A0D50000000000000001"; //live
            String iksnLive = "FFFF000006DDDDE00000";
            String kcv_live = "10B9824432E458DD";
           // POSManager.getInstance().doUpdateIpekKey(ipektw,iksnLive,kcv_live);
        } catch (Exception e) {
            notifyError("Transaction execution failed: " + e.getMessage());
        }
    }

    private void notifyDeviceConnected() {
        mainHandler.post(() -> {
            ToastUtils.showLong("Device connected successfully");
            classEmvCallBacks.onDeviceConnected("Device connected");

            // Update EMV configuration
            try {
                //POSManager.getInstance().updateEMVConfig("emv_profile_tlv.xml");
              //  POSManager.getInstance().doUpdateIpekKey("","","");

            } catch (Exception e) {
                Log.e(TAG, "Error updating EMV config: " + e.getMessage());
            }
        });
    }

    private void notifyDeviceDisconnected() {
        mainHandler.post(() -> {
            ToastUtils.showLong("Device disconnected");
            classEmvCallBacks.onDeviceDisconnected("Device disconnected");
        });
    }

    private void notifyError(String message) {
        mainHandler.post(() -> {
            classEmvCallBacks.onError(message);
        });
    }

    private void notifyTransactionCancelled() {
        mainHandler.post(() -> {
            if(POSManager.getInstance().isDeviceReady())
                POSManager.getInstance().cancelTransaction();
            classEmvCallBacks.onTransactionCancelled();
        });
    }

    private void setupPinPad(List<String> dataList) {
        mainHandler.post(() -> {
            try {
                if (keyboardUtil != null) {
                    keyboardUtil.hide();
                }

                boolean onlinePin = POSManager.getInstance().isOnlinePin();
                if (pinpadEditText != null) {
                    pinpadEditText.setText("");
                    // Make sure EditText is visible
                    pinpadEditText.setVisibility(View.VISIBLE);
                    pinpadEditText.requestFocus();
                    pinpadEditText.setGravity(Gravity.CENTER);
                    pinpadEditText.setBackgroundColor(Color.WHITE); // Make it obvious
                    //pinpadEditText.setText("");
                    pinpadEditText.setTextColor(Color.BLUE);
                    TRACE.d("PINPAD pinpadEditText NULL");
                }

                MyKeyboardView.setKeyBoardListener(value -> {
                    if (POSManager.getInstance().isDeviceReady()) {
                        TRACE.d("PIN VALUE::"+ value);
                        POSManager.getInstance().pinMapSync(value, 20);
                    }
                });

                if (POSManager.getInstance().isDeviceReady() && scvText != null && pinpadEditText != null) {
                    TRACE.d("PINPAD pinpadEditText NOT NULL");
                    keyboardUtil = new KeyboardUtil(classActivity, scvText, dataList);
                    keyboardUtil.initKeyboard(MyKeyboardView.KEYBOARDTYPE_Only_Num_Pwd, pinpadEditText);
                } else {
                    notifyError("PIN pad initialization failed - required views not available");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error setting up PIN pad: " + e.getMessage());
                notifyError("PIN pad setup failed");
            }
        });
        TRACE.d("PIN VAl:"+pinpadEditText.getText().toString());

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

//

        private void showEmvAppSelectionDialog(ArrayList<String> appList) {
            try {
                Dialog dialog = new Dialog(classActivity);
                dialog.setContentView(R.layout.emv_app_dialog);
                dialog.setTitle(R.string.please_select_app);

                String[] appNameList = appList.toArray(new String[0]);
                ListView appListView = dialog.findViewById(R.id.appList);
                appListView.setAdapter(new ArrayAdapter<>(
                        classActivity, android.R.layout.simple_list_item_1, appNameList));

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
            TRACE.d("onQposRequestPinResult = " + dataList + "\nofflineTime: " + offlineTime );
            mainHandler.post(() -> handlePinRequest(dataList, offlineTime));
        }

        private void handlePinRequest(List<String> dataList, int offlineTime) {
            TRACE.d("handlePinRequest() " + offlineTime);
            if (!POSManager.getInstance().isDeviceReady()) return;

            //classEmvCallBacks.onShowPinPad(true);
            classEmvCallBacks.onStopLoading();
            setupPinPad(dataList);
          //  for (String t : dataList) System.out.println("PINPAD: "+t);

        }

        @Override
        public void onRequestSetPin(boolean isOfflinePin, int tryNum) {
            TRACE.d("onRequestSetPin = " + isOfflinePin + "\ntryNum: " + tryNum);
            mainHandler.post(() -> handleSetPinRequest(isOfflinePin, tryNum));
        }

        private void handleSetPinRequest(boolean isOfflinePin, int tryNum) {
            isPinBack = true;
            classEmvCallBacks.onShowPinPad(true);

            String message;
            if (POSManager.getInstance().getTransType() == QPOSService.TransactionType.UPDATE_PIN) {
                changePinTimes++;
                if (changePinTimes == 1) {
                    message = classActivity.getString(R.string.input_pin_old);
                } else if (changePinTimes == 2 || changePinTimes == 4) {
                    message = classActivity.getString(R.string.input_pin_new);
                } else {
                    message = classActivity.getString(R.string.input_new_pin_confirm);
                }
            } else {
                message = isOfflinePin ?
                        classActivity.getString(R.string.input_offlinePin) :
                        classActivity.getString(R.string.input_onlinePin);
            }

            classEmvCallBacks.onTitleTextChanged(message);
        }

        @Override
        public void onRequestSetPin() {
            TRACE.i("onRequestSetPin()");
            mainHandler.post(this::showPinPadDialog);
        }

        private void showPinPadDialog() {
            try {
                classEmvCallBacks.onTitleTextChanged(classActivity.getString(R.string.input_pin));

                pinPadDialog = new PinPadDialog(classActivity);
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
                                            TRACE.d("PIN BLOCK-->"+ pinBlock);
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
                notifyError("PIN input failed");
            }
        }

        @Override
        public void onGetCardNoResult(String cardNo){
            TRACE.d("onGetCardNoResult(String cardNo):" + cardNo);
        }


        @Override
        public void onRequestBatchData(String tlv) {
            TRACE.d("onRequestBatchData()");
            pinpadEditText.setVisibility(View.GONE);

            TRACE.d("ICC trade finished");
            String content = "getString(R.string.batch_data)";
            content += tlv;
            TRACE.d("ICC trade finished +"+content);
        }

        @Override
        public void onRequestDisplay(QPOSService.Display displayMsg) {
            TRACE.d("onRequestDisplay(Display displayMsg):" + displayMsg.toString());
            mainHandler.post(() -> handleDisplayRequest(displayMsg));
        }

        private void handleDisplayRequest(QPOSService.Display displayMsg) {
            String message = HandleTxnsResultUtils.getDisplayMessage(displayMsg, classActivity);

            if (displayMsg == QPOSService.Display.MSR_DATA_READY) {
                showMsrDataReadyDialog();
            } else if (displayMsg == QPOSService.Display.INPUT_NEW_PIN) {
                isChangePin = true;
                timeOfPinInput++;
            } else if (displayMsg == QPOSService.Display.INPUT_NEW_PIN_CHECK_ERROR) {
                message = classActivity.getString(R.string.input_new_pin_check_error);
                timeOfPinInput = 0;
            }

            classEmvCallBacks.onLoading(message);
        }

        private void showMsrDataReadyDialog() {
            try {
                AlertDialog.Builder builder = new AlertDialog.Builder(classActivity);
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
            String content = classActivity.getString(R.string.batch_data) + result.getTlv();
            TRACE.d("handleICCResult() " + content);

            classEmvCallBacks.onTransactionSuccess(content);
        }

        private void handleMCRResult(PaymentResult result) {
            // Handle magnetic card results
            classEmvCallBacks.onTransactionSuccess("Magnetic card transaction completed");
        }

        private void handleNFCResult(PaymentResult result) {
            // Handle NFC results
            classEmvCallBacks.onTransactionSuccess("NFC transaction completed");
        }

        @Override
        public void onRequestTransactionResult(QPOSService.TransactionResult transactionResult) {
            String message = HandleTxnsResultUtils.getTransactionResultMessage(transactionResult, classActivity);
            TRACE.d("onRequestTransactionResult: "+message);
            if (transactionResult == QPOSService.TransactionResult.CARD_REMOVED) {
            }
            String msg = "";
            if (transactionResult == QPOSService.TransactionResult.APPROVED) {
                TRACE.d("TransactionResult.APPROVED");
//                 msg = getString(R.string.transaction_approved) + "\n" + getString(R.string.amount) + ": $" + amounts + "\n";
//                if (!cashbackAmounts.equals("")) {
//                    msg += getString(R.string.cashback_amount) + ": INR" + cashbackAmounts;
//                }
            } else if (transactionResult == QPOSService.TransactionResult.TERMINATED) {
                msg = "getString(R.string.transaction_terminated)";
            } else if (transactionResult == QPOSService.TransactionResult.DECLINED) {
                msg = "getString(R.string.transaction_declined)";
            } else if (transactionResult == QPOSService.TransactionResult.CANCEL) {
                msg = "getString(R.string.transaction_cancel)";
            } else if (transactionResult == QPOSService.TransactionResult.CAPK_FAIL) {
                msg = "getString(R.string.transaction_capk_fail)";
            } else if (transactionResult == QPOSService.TransactionResult.NOT_ICC) {
                msg = "getString(R.string.transaction_not_icc)";
            } else if (transactionResult == QPOSService.TransactionResult.SELECT_APP_FAIL) {
                msg = "getString(R.string.transaction_app_fail)";
            } else if (transactionResult == QPOSService.TransactionResult.DEVICE_ERROR) {
                msg = "getString(R.string.transaction_device_error)";
            } else if (transactionResult == QPOSService.TransactionResult.TRADE_LOG_FULL) {
                msg = "the trade log has fulled!pls clear the trade log!";
            } else if (transactionResult == QPOSService.TransactionResult.CARD_NOT_SUPPORTED) {
                msg = "getString(R.string.card_not_supported)";
            } else if (transactionResult == QPOSService.TransactionResult.MISSING_MANDATORY_DATA) {
                msg = "getString(R.string.missing_mandatory_data)";
            } else if (transactionResult == QPOSService.TransactionResult.CARD_BLOCKED_OR_NO_EMV_APPS) {
                msg = "getString(R.string.card_blocked_or_no_evm_apps)";
            } else if (transactionResult == QPOSService.TransactionResult.INVALID_ICC_DATA) {
                msg = "getString(R.string.invalid_icc_data)";
            } else if (transactionResult == QPOSService.TransactionResult.FALLBACK) {
                msg = "trans fallback";
                           POSManager.getInstance().updateEMVConfig("emv_profile_tlv.xml");
                           TRACE.d("EMV Updated");
//            POSManager.getInstance().doUpdateIpekKey("","","");
            } else if (transactionResult == QPOSService.TransactionResult.NFC_TERMINATED) {
                msg = "NFC Terminated";
            } else if (transactionResult == QPOSService.TransactionResult.CARD_REMOVED) {
                msg = "CARD REMOVED";
            } else if (transactionResult == QPOSService.TransactionResult.CONTACTLESS_TRANSACTION_NOT_ALLOW) {
                msg = "TRANS NOT ALLOW";
            } else if (transactionResult == QPOSService.TransactionResult.CARD_BLOCKED) {
                msg = "CARD BLOCKED";
            } else if (transactionResult == QPOSService.TransactionResult.TRANS_TOKEN_INVALID) {
                msg = "TOKEN INVALID";
            } else if (transactionResult == QPOSService.TransactionResult.APP_BLOCKED) {
                msg = "APP BLOCKED";
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
            mainHandler.post(() -> handleOnlineProcessRequest(tlv));
        }

        @Override
        public void onReturnGetPinResult(Hashtable<String, String> result){
            TRACE.d("onReturnGetPinResult(Hashtable<String, String> result):" + result.toString());
            String pinBlock = result.get("pinBlock");
            String pinKsn = result.get("pinKsn");
            String content = "get pin result\n";
            content += "getString(R.string.pinKsn) "+ " " + pinKsn + "\n";
            content += "getString(R.string.pinBlock)" + " " + pinBlock + "\n";
            //statusEditText.setText(content);
            TRACE.i(content);
        }
        @Override
        public void onReturnCustomConfigResult(boolean isSuccess,String result){
            TRACE.d("onReturnCustomConfigResult "+ "isSuccess:"+isSuccess +" result:"+result);
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
            classEmvCallBacks.onShowPinPad(false);
            classEmvCallBacks.onLoading("Processing online authorization");

            try {
                Hashtable<String, String> decodeData = POSManager.getInstance().anlysEmvIccData(tlv);
                System.out.println("=== EMV ICC Data ===");
                for (Map.Entry<String, String> entry : decodeData.entrySet()) {
                    System.out.println(entry.getKey() + " = " + entry.getValue());
                }
                System.out.println("====================");
                String requestTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        .format(Calendar.getInstance().getTime());

                //Hashtable<String, String> decodeData = POSManager.anlysEmvIccData(tlv);
              //  TRACE.d("anlysEmvIccData(tlv):" + decodeData.toString());

                String data = "{\"createdAt\": " + requestTime +
                        ", \"deviceInfo\": " + DeviceUtils.getPhoneDetail() +
                        ", \"countryCode\": " + DeviceUtils.getDevieCountry(classActivity) +
                        ", \"tlv\": " + tlv + "}";
                TRACE.d("PAYLOAD:"+data);

                TLVParser tlvParser = new TLVParser();
                ICCDecryptor iccDecryptor = new ICCDecryptor();

                EMVTLVParser emvtlvParser = new EMVTLVParser();
                String tagToExtract = "C0";
                String tagKSNOfOnlineMsg = emvtlvParser.extractTag(tlv, "C0");
                TRACE.d("CO:"+tagKSNOfOnlineMsg);
                String tagOnlineMessage = emvtlvParser.extractTag(tlv, "C2");
//                String onlineMessage = iccDecryptor.decryptIccInfo(tagKSNOfOnlineMsg,tagOnlineMessage);
//                TRACE.d("onlineMessage\nKSN:"+tagKSNOfOnlineMsg+"\nEncypted Online message:"
//                        +tagOnlineMessage+"\nDecrypted Online message:"+onlineMessage);
               // ThreeDES tt = new ThreeDES();
                //33707E4927C4A0D50000000000000000
                SecretKey ipek = new SecretKeySpec(hexStringToByteArray("33707E4927C4A0D50000000000000000"), "DESede");
                 ipek=DUKPTKeyDerivation.deriveSessionKey(ipek,tagKSNOfOnlineMsg);
                String t_ = ThreeDES.tdesECBDecrypt(bytesToHex(ipek.getEncoded()),tagOnlineMessage);
                TRACE.d("TTTTTY: "+t_);

//                TripleDESBackendProcessor processor_ = new TripleDESBackendProcessor();
//                try {
//                    String result = processor_.processOnlineMessage(tagKSNOfOnlineMsg, tagOnlineMessage);
//                    TRACE.d("Decrypted message_: " + result);
//                } catch (Exception e) {
//                    System.err.println("Error: " + e.getMessage()+"\n");
//                    e.printStackTrace();
//                }
//                Hashtable<String, String> table =  POSManager.getInstance().getENCDataBlock();
//                for (Map.Entry<String, String> entry : table.entrySet()) {
//                    System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
//                }



                EmvTlvParser emvTlvParser = new EmvTlvParser();
                byte[] bytes = emvTlvParser.hexToBytes(tlv);
                List<EmvTlvParser.Tlv> list = emvTlvParser.parse(bytes);
                for (EmvTlvParser.Tlv t : list) System.out.println(t);

                //Decrypting data https://dspread.gitlab.io/qpos/#/post-transaction?id=online-request
                cardModel = new CardModel();
                GetwayProcessor processor = new GetwayProcessor(classActivity);
//                EmvTLVExtractor emvTLVExtractor = new EmvTLVExtractor(emvService, classTransactionData);
//                KsmgRequest pinchangeRequest = new KsmgRequest(emvTLVExtractor.extractEmvData(), classTransactionData, cardModel);

                TRACE.d();
                TRACE.d("TTT:"+classTransactionData.getAmount());

                // Process online authorization
                responseMessage = "Online process initiated: " + data;

            } catch (Exception e) {
                Log.e(TAG, "Error processing online request: " + e.getMessage());
                notifyError("Online processing failed");
            }
        }

        @Override
        public void onReturnGetPinInputResult(int num) {
            TRACE.i("onReturnGetPinInputResult  ===" + num);
            mainHandler.post(() -> handlePinInputResult(num));
        }

        private void handlePinInputResult(int num) {
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