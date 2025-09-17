package com.isw.payapp.devices.dspread.Activity.tests;



import static android.content.Intent.getIntent;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.app.Activity;
import android.os.Bundle;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.dspread.print.util.TRACE;
import com.dspread.xpos.QPOSService;
//import com.isw.payapp.BR;
import com.isw.payapp.R;
//import com.isw.payapp.databinding.ActivityPaymentBinding;
import com.isw.payapp.devices.dspread.Activity.ViewModels.PaymentViewModel;
import com.isw.payapp.devices.dspread.Activity.helpers.PrinterHelper;
import com.isw.payapp.devices.dspread.Activity.models.PaymentModel;
import com.isw.payapp.devices.dspread.Activity.pinkeyboard.KeyboardUtil;
import com.isw.payapp.devices.dspread.Activity.pinkeyboard.MyKeyboardView;
import com.isw.payapp.devices.dspread.Activity.pinkeyboard.PinPadDialog;
import com.isw.payapp.devices.dspread.POSManager;
import com.isw.payapp.devices.dspread.callbacks.IConnectionServiceCallback;
import com.isw.payapp.devices.dspread.callbacks.IPaymentServiceCallback;
import com.isw.payapp.devices.dspread.callbacks.PaymentResult;
import com.isw.payapp.devices.dspread.utils.BitmapReadyListener;
import com.isw.payapp.devices.dspread.utils.DeviceUtils;
import com.isw.payapp.devices.dspread.utils.HandleTxnsResultUtils;
import com.isw.payapp.devices.dspread.utils.LogFileConfig;
import com.isw.payapp.devices.dspread.utils.QPOSUtil;
import com.isw.payapp.devices.dspread.utils.ReceiptGenerator;

import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;

import me.goldze.mvvmhabit.base.BaseActivity;
import me.goldze.mvvmhabit.utils.ToastUtils;

public class PaymentActivity/* extends BaseActivity<ActivityPaymentBinding, PaymentViewModel> implements IPaymentServiceCallback*/ {
//
//    private String amount;
//
//    private Context context;
//    private String deviceAddress;
//    private KeyboardUtil keyboardUtil;
//    private boolean isChangePin = false;
//    private int timeOfPinInput;
//    public PinPadDialog pinPadDialog;
//    private LogFileConfig logFileConfig;
//    private int changePinTimes;
//    private boolean isPinBack = false;
//
//    private  Intent intent;
//
//    private String string;
//
//    private IPaymentServiceCallback paymentServiceCallback;
//
//    @Override
//    public int initContentView(Bundle savedInstanceState) {
//        return R.layout.activity_payment;
//    }
//
//    @Override
//    public int initVariableId() {
//        return BR.viewModel;
//    }
//
//    /**
//     * Return the intent that started this activity.
//     */
////    public Intent getIntent() {
////        return intent;
////    }
////
////    public String getString(){
////        return string;
////    }
////
////    public void setIntent(Intent intent){
////        this.intent = intent;
////    }
////
////    public void setString(String string){
////        this.string = string;
////    }
//
//
//
//    /**
//     * Initialize payment activity data
//     * Sets up initial UI state and starts transaction
//     */
//    @Override
//    public void initData() {
//        //Intent intent = getIntent();
//        logFileConfig = LogFileConfig.getInstance(this);
//        binding.setVariable(BR.viewModel, viewModel);
//        viewModel.setmContext(context.getApplicationContext());
//        binding.pinpadEditText.setText("");
//        viewModel.titleText.set("Paymenting");
//        changePinTimes = 0;
//
//
//        paymentServiceCallback = new PaymentCallback();
//
//            amount = "100.00";
//            deviceAddress = "";//getIntent().getStringExtra("deviceAddress");
//
//
//        viewModel.displayAmount(amount);//ui
//        startTransaction();
//    }
//
//    @Override
//    public void initViewObservable() {
//        super.initViewObservable();
//        viewModel.isOnlineSuccess.observe(this, aBoolean -> {
//            if (aBoolean) {
//                if (DeviceUtils.isPrinterDevices()) {
//                    handleSendReceipt();
//                }
//                viewModel.setTransactionSuccess();
//            } else {
//                viewModel.setTransactionFailed("Transaction failed because of the network!");
//            }
//        });
//    }
//
//    /**
//     * Start payment transaction in background thread
//     * Handles device connection and transaction initialization
//     */
//    private void startTransaction() {
//        new Thread(() -> {
//            if(!POSManager.getInstance().isDeviceReady()){
//                POSManager.getInstance().connect(deviceAddress,new IConnectionServiceCallback() {
//                    @Override
//                    public void onRequestNoQposDetected() {
//                    }
//
//                    @Override
//                    public void onRequestQposConnected() {
//                        ToastUtils.showLong("Device connected");
//                    }
//
//                    @Override
//                    public void onRequestQposDisconnected() {
//                        ToastUtils.showLong("Device disconnected");
//                        finish();
//                    }
//                });
//            }
//            POSManager.getInstance().startTransaction(amount, paymentServiceCallback);
//        }).start();
//    }
//
//    /**
//     * Inner class to handle payment callbacks
//     * Implements all payment related events and UI updates
//     */
//    private class PaymentCallback implements IPaymentServiceCallback{
//
//        @Override
//        public void onRequestWaitingUser() {
//            viewModel.setWaitingStatus(true);
//        }
//
//        @Override
//        public void onRequestTime() {
//            String terminalTime = new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance().getTime());
//            TRACE.d("onRequestTime: " + terminalTime);
//            POSManager.getInstance().sendTime(terminalTime);
//        }
//
//        @Override
//        public void onRequestSelectEmvApp(ArrayList<String> appList) {
//            TRACE.d("onRequestSelectEmvApp():" + appList.toString());
//            Dialog dialog = new Dialog(PaymentActivity.this);
//            dialog.setContentView(R.layout.emv_app_dialog);
//            dialog.setTitle(R.string.please_select_app);
//            String[] appNameList = new String[appList.size()];
//            for (int i = 0; i < appNameList.length; ++i) {
//                appNameList[i] = appList.get(i);
//            }
//            ListView appListView = dialog.findViewById(R.id.appList);
//            appListView.setAdapter(new ArrayAdapter<>(PaymentActivity.this, android.R.layout.simple_list_item_1, appNameList));
//            appListView.setOnItemClickListener((parent, view, position, id) -> {
//                POSManager.getInstance().selectEmvApp(position);
//                TRACE.d("select emv app position = " + position);
//                dialog.dismiss();
//            });
//            dialog.findViewById(R.id.cancelButton).setOnClickListener(v -> {
//                POSManager.getInstance().cancelSelectEmvApp();
//                dialog.dismiss();
//            });
//            dialog.show();
//        }
//
//        /**
//         * Handle PIN input request
//         * Sets up PIN pad and keyboard for user input
//         * @param dataList List of PIN data
//         * @param offlineTime Offline PIN try count
//         */
//        @Override
//        public void onQposRequestPinResult(List<String> dataList, int offlineTime) {
//            TRACE.d("onQposRequestPinResult = " + dataList + "\nofflineTime: " + offlineTime);
//            if (POSManager.getInstance().isDeviceReady()) {
//                viewModel.stopLoading();
//                viewModel.clearErrorState();
//                viewModel.showPinpad.set(true);
//                boolean onlinePin = POSManager.getInstance().isOnlinePin();
//                if (keyboardUtil != null) {
//                    keyboardUtil.hide();
//                }
//                if (isChangePin) {
//                    if (timeOfPinInput == 1) {
//                        viewModel.titleText.set("input_new_pin_first_time");
//                    } else if (timeOfPinInput == 2) {
//                        viewModel.titleText.set("input_new_pin_confirm");
//                        timeOfPinInput = 0;
//                    }
//                } else {
//                    if (onlinePin) {
//                        viewModel.titleText.set("input_onlinePin");
//                    } else {
//                        int cvmPinTryLimit = POSManager.getInstance().getCvmPinTryLimit();
//                        TRACE.d("PinTryLimit:" + cvmPinTryLimit);
//                        if (cvmPinTryLimit == 1) {
//                            viewModel.titleText.set("input_offlinePin_last");
//                        } else {
//                            viewModel.titleText.set("input_offlinePin");
//                        }
//                    }
//                }
//            }
//            binding.pinpadEditText.setText("");
//            MyKeyboardView.setKeyBoardListener(value -> {
//                if (POSManager.getInstance().isDeviceReady()) {
//                    POSManager.getInstance().pinMapSync(value, 20);
//                }
//            });
//            if (POSManager.getInstance().isDeviceReady()) {
//                keyboardUtil = new KeyboardUtil(PaymentActivity.this, binding.scvText, dataList);
//                keyboardUtil.initKeyboard(MyKeyboardView.KEYBOARDTYPE_Only_Num_Pwd, binding.pinpadEditText);//Random keyboard
//            }
//        }
//
//        @Override
//        public void onRequestSetPin(boolean isOfflinePin, int tryNum) {
//            TRACE.d("onRequestSetPin = " + isOfflinePin + "\ntryNum: " + tryNum);
//            isPinBack = true;
//            // Clear previous error state when entering PIN input
//            viewModel.clearErrorState();
//            if (POSManager.getInstance().getTransType() == QPOSService.TransactionType.UPDATE_PIN) {
//                changePinTimes++;
//                if (changePinTimes == 1) {
//                    viewModel.titleText.set("input_pin_old");
//                } else if (changePinTimes == 2 || changePinTimes == 4) {
//                    viewModel.titleText.set("input_pin_new");
//                } else if (changePinTimes == 3 || changePinTimes == 5) {
//                    viewModel.titleText.set("input_new_pin_confirm");
//                }
//            } else {
//                if (isOfflinePin) {
//                    viewModel.titleText.set(input_offlinePin);
//                } else {
//                    viewModel.titleText.set("input_onlinePin");
//                }
//            }
//            viewModel.stopLoading();
//            viewModel.showPinpad.set(true);
//        }
//
//        @Override
//        public void onRequestSetPin() {
//            TRACE.i("onRequestSetPin()");
//            viewModel.clearErrorState();
//            viewModel.titleText.set("input_pin");
//            pinPadDialog = new PinPadDialog(PaymentActivity.this);
//            pinPadDialog.getPayViewPass().setRandomNumber(true).setPayClickListener(POSManager.getInstance().getQPOSService(), new PinPadView.OnPayClickListener() {
//
//                @Override
//                public void onCencel() {
//                    POSManager.getInstance().cancelPin();
//                    pinPadDialog.dismiss();
//                }
//
//                @Override
//                public void onPaypass() {
//                    POSManager.getInstance().bypassPin();
//                    pinPadDialog.dismiss();
//                }
//
//                @Override
//                public void onConfirm(String password) {
//                    String pinBlock = QPOSUtil.buildCvmPinBlock(POSManager.getInstance().getEncryptData(), password);// build the ISO format4 pin block
//                    POSManager.getInstance().sendCvmPin(pinBlock, true);
//                    pinPadDialog.dismiss();
//                }
//            });
//        }
//
//        @Override
//        public void onRequestDisplay(QPOSService.Display displayMsg) {
//            TRACE.d("onRequestDisplay(Display displayMsg):" + displayMsg.toString());
//            String msg = "";
//            if (displayMsg == QPOSService.Display.MSR_DATA_READY) {
//                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(PaymentActivity.this);
//                builder.setTitle("Audio");
//                builder.setMessage("Success,Contine ready");
//                builder.setPositiveButton("Confirm", null);
//                builder.show();
//            } else if (displayMsg == QPOSService.Display.INPUT_NEW_PIN) {
//                isChangePin = true;
//                timeOfPinInput++;
//            } else if (displayMsg == QPOSService.Display.INPUT_NEW_PIN_CHECK_ERROR) {
//                msg = "input_new_pin_check_error";
//                timeOfPinInput = 0;
//            } else {
//                msg = HandleTxnsResultUtils.getDisplayMessage(displayMsg, PaymentActivity.this);
//            }
//            viewModel.startLoading(msg);
//        }
//
//        /**
//         * Handle transaction completion
//         * Updates UI and processes different transaction types (MCR/NFC/ICC)
//         * @param result Payment transaction result
//         */
//        @Override
//        public void onTransactionCompleted(PaymentResult result) {
//            viewModel.showPinpad.set(false);
//            isChangePin = false;
//            String transType = result.getTransactionType();
//            if(transType != null){
//                if(QPOSService.DoTradeResult.MCR.name().equals(transType)){
//                    HandleTxnsResultUtils.handleMCRResult(result, PaymentActivity.this, binding, viewModel);
//                }else if(QPOSService.DoTradeResult.NFC_OFFLINE.name().equals(transType)||QPOSService.DoTradeResult.NFC_ONLINE.name().equals(transType)){
//                    HandleTxnsResultUtils.handleNFCResult(result, PaymentActivity.this, binding, viewModel);
//                }else {//iCC result
//                    String content = "batch_data";
//                    content += result.getTlv();
//                    PaymentModel paymentModel = viewModel.setTransactionSuccess(content);
//                    binding.tvReceipt.setMovementMethod(LinkMovementMethod.getInstance());
//                    Spanned receiptContent = ReceiptGenerator.generateICCReceipt(paymentModel);
//                    binding.tvReceipt.setText(receiptContent);
//                    if (DeviceUtils.isPrinterDevices()) {
//                        handleSendReceipt();
//                    }
//                }
//            }
//        }
//
//        @Override
//        public void onTransactionFailed(String errorMessage, String data) {
//            viewModel.showPinpad.set(false);
//            if (keyboardUtil != null) {
//                keyboardUtil.hide();
//            }
//            if(errorMessage != null){
//                viewModel.setTransactionFailed(errorMessage);
//            }
//        }
//
//        /**
//         * Handle online process request
//         * Sends transaction data to server for online authorization
//         * @param tlv TLV format transaction data
//         */
//        @Override
//        public void onRequestOnlineProcess(final String tlv) {
//            TRACE.d("onRequestOnlineProcess" + tlv);
//            viewModel.showPinpad.set(false);
//            viewModel.startLoading("online_process_requested");
//            Hashtable<String, String> decodeData = POSManager.getInstance().anlysEmvIccData(tlv);
//            String requestTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
//            String data = "{\"createdAt\": " + requestTime + ", \"deviceInfo\": " + DeviceUtils.getPhoneDetail() + ", \"countryCode\": " + DeviceUtils.getDevieCountry(PaymentActivity.this)
//                    + ", \"tlv\": " + tlv + "}";
//            viewModel.requestOnlineAuth(true, data);
//        }
//
//        @Override
//        public void onReturnGetPinInputResult(int num) {
//            TRACE.i("onReturnGetPinInputResult  ===" + num);
//            StringBuilder s = new StringBuilder();
//            if (num == -1) {
//                isPinBack = false;
//                binding.pinpadEditText.setText("");
//                viewModel.showPinpad.set(false);
//                if (keyboardUtil != null) {
//                    keyboardUtil.hide();
//                }
//            } else {
//                for (int i = 0; i < num; i++) {
//                    s.append("*");
//                }
//                binding.pinpadEditText.setText(s.toString());
//            }
//        }
//    }
//
//    @Override
//    public void onBackPressed() {
//        if (!isPinBack) {
//            new Thread(() -> {
//                POSManager.getInstance().cancelTransaction();
//                runOnUiThread(() -> finish());
//            }).start();
//        }
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        LogFileConfig.getInstance(this).readLog();
//        PrinterHelper.getInstance().close();
//        POSManager.getInstance().unregisterCallbacks();
//    }
//
//    /**
//     * Convert receipt TextView to Bitmap for printing
//     * @param listener Callback when bitmap is ready
//     */
//    private void convertReceiptToBitmap(final BitmapReadyListener listener) {
//        binding.tvReceipt.post(new Runnable() {
//            @Override
//            public void run() {
//                if (binding.tvReceipt.getWidth() <= 0) {
//                    binding.tvReceipt.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//                        @Override
//                        public void onGlobalLayout() {
//                            binding.tvReceipt.getViewTreeObserver().removeOnGlobalLayoutListener(this);
//                            Bitmap bitmap = viewModel.convertReceiptToBitmap(binding.tvReceipt);
//                            if (listener != null) {
//                                listener.onBitmapReady(bitmap);
//                            }
//                        }
//                    });
//                } else {
//                    Bitmap bitmap = viewModel.convertReceiptToBitmap(binding.tvReceipt);
//                    if (listener != null) {
//                        listener.onBitmapReady(bitmap);
//                    }
//                }
//            }
//        });
//    }
//
//    /**
//     * Handle receipt printing
//     * Converts receipt view to bitmap and shows print button
//     */
//    private void handleSendReceipt() {
//        convertReceiptToBitmap(bitmap -> {
//            if (bitmap != null) {
//                binding.btnSendReceipt.setVisibility(View.VISIBLE);
//            } else {
//                binding.btnSendReceipt.setVisibility(View.GONE);
//            }
//        });
//    }
}
