package com.isw.payapp.devices.dspread.baseService;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dspread.xpos.CQPOSService;
import com.dspread.xpos.QPOSService;
import com.isw.payapp.devices.dspread.annotations.ICallbackChange;
import com.isw.payapp.devices.dspread.callbacks.IConnectionServiceCallback;
import com.isw.payapp.devices.dspread.callbacks.IPaymentServiceCallback;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class BasePOSService extends CQPOSService {
//    private static final String TAG = "BasePOSService";
//    private final QPOSCallbackManager callbackManager = QPOSCallbackManager.getInstance();
//
//    @ICallbackChange(
//            description = "The callback is in child thread",
//            type = ICallbackChange.ChangeType.MODIFIED
//    )
//    @Override
//    public void onDoTradeResult(@NonNull QPOSService.DoTradeResult result,
//                                @Nullable Hashtable<String, String> decodeData) {
//        Log.d(TAG, "onDoTradeResult: " + result);
//        IPaymentServiceCallback callback = callbackManager.getPaymentCallback();
//        if (callback != null) {
//       //     callback.onDoTradeResult(result, decodeData);
//        }
//    }
//
//    @ICallbackChange(
//            description = "Error callback will now notify all registered listeners",
//            type = ICallbackChange.ChangeType.MODIFIED
//    )
//    @Override
//    public void onError(@NonNull QPOSService.Error errorState) {
//        Log.e(TAG, "onError: " + errorState);
//        IPaymentServiceCallback paymentCallback = callbackManager.getPaymentCallback();
//        if (paymentCallback != null) {
//        //    paymentCallback.onError(errorState);
//        }
//    }
//
//    @Override
//    public void onQposInfoResult(@Nullable Hashtable<String, String> posInfoData) {
//        Log.d(TAG, "onQposInfoResult: " + (posInfoData != null ? posInfoData.toString() : "null"));
//        // Additional processing can be added here if needed
//    }
//
//    @Override
//    public void onRequestTransactionResult(@NonNull QPOSService.TransactionResult transactionResult) {
//        Log.d(TAG, "onRequestTransactionResult: " + transactionResult);
//        IPaymentServiceCallback callback = callbackManager.getPaymentCallback();
//        if (callback != null) {
//        //    callback.onRequestTransactionResult(transactionResult);
//        }
//    }
//
//    @Override
//    public void onRequestBatchData(@Nullable String tlv) {
//        Log.d(TAG, "onRequestBatchData: " + tlv);
//        IPaymentServiceCallback callback = callbackManager.getPaymentCallback();
//        if (callback != null) {
//      //      callback.onRequestBatchData(tlv);
//        }
//    }
//
//    @Override
//    public void onQposIdResult(@Nullable Hashtable<String, String> posIdTable) {
//        Log.d(TAG, "onQposIdResult: " + (posIdTable != null ? posIdTable.toString() : "null"));
//        // Additional processing can be added here if needed
//    }
//
//    @Override
//    public void onRequestSelectEmvApp(@Nullable ArrayList<String> appList) {
//        Log.d(TAG, "onRequestSelectEmvApp: " + (appList != null ? appList.toString() : "null"));
//        IPaymentServiceCallback callback = callbackManager.getPaymentCallback();
//        if (callback != null) {
//            callback.onRequestSelectEmvApp(appList);
//        }
//    }
//
//    @Override
//    public void onRequestWaitingUser() {
//        Log.d(TAG, "onRequestWaitingUser - waiting for user to insert/swipe/tap card");
//        IPaymentServiceCallback callback = callbackManager.getPaymentCallback();
//        if (callback != null) {
//            callback.onRequestWaitingUser();
//        }
//    }
//
//    @Override
//    public void onQposRequestPinResult(@Nullable List<String> dataList, int offlineTime) {
//        Log.d(TAG, "onQposRequestPinResult, offlineTime: " + offlineTime);
//        super.onQposRequestPinResult(dataList, offlineTime);
//        IPaymentServiceCallback callback = callbackManager.getPaymentCallback();
//        if (callback != null) {
//            callback.onQposRequestPinResult(dataList, offlineTime);
//        }
//    }
//
//    @Override
//    public void onReturnGetKeyBoardInputResult(@Nullable String result) {
//        Log.d(TAG, "onReturnGetKeyBoardInputResult: " + result);
//        super.onReturnGetKeyBoardInputResult(result);
//        IPaymentServiceCallback callback = callbackManager.getPaymentCallback();
//        if (callback != null) {
//         //   callback.onReturnGetKeyBoardInputResult(result);
//        }
//    }
//
//    @Override
//    public void onReturnGetPinInputResult(int num) {
//        Log.d(TAG, "onReturnGetPinInputResult: " + num);
//        IPaymentServiceCallback callback = callbackManager.getPaymentCallback();
//        if (callback != null) {
//            callback.onReturnGetPinInputResult(num);
//        }
//    }
//
//    @Override
//    public void onRequestSetAmount() {
//        Log.d(TAG, "onRequestSetAmount");
//        IPaymentServiceCallback callback = callbackManager.getPaymentCallback();
//        if (callback != null) {
//            callback.onRequestSetAmount();
//        }
//    }
//
//    @Override
//    public void onRequestOnlineProcess(@Nullable String tlv) {
//        Log.d(TAG, "onRequestOnlineProcess: " + tlv);
//        IPaymentServiceCallback callback = callbackManager.getPaymentCallback();
//        if (callback != null) {
//            callback.onRequestOnlineProcess(tlv);
//        }
//    }
//
//    @Override
//    public void onRequestTime() {
//        Log.d(TAG, "onRequestTime");
//        IPaymentServiceCallback callback = callbackManager.getPaymentCallback();
//        if (callback != null) {
//            callback.onRequestTime();
//        }
//    }
//
//    @Override
//    public void onRequestDisplay(@NonNull QPOSService.Display displayMsg) {
//        Log.d(TAG, "onRequestDisplay: " + displayMsg);
//        IPaymentServiceCallback callback = callbackManager.getPaymentCallback();
//        if (callback != null) {
//            callback.onRequestDisplay(displayMsg);
//        }
//    }
//
//    @Override
//    public void onRequestNoQposDetected() {
//        Log.d(TAG, "onRequestNoQposDetected");
//        IConnectionServiceCallback callback = callbackManager.getConnectionCallback();
//        if (callback != null) {
//            callback.onRequestNoQposDetected();
//        }
//    }
//
//    @Override
//    public void onRequestQposConnected() {
//        Log.d(TAG, "onRequestQposConnected");
//        IConnectionServiceCallback callback = callbackManager.getConnectionCallback();
//        if (callback != null) {
//            callback.onRequestQposConnected();
//        }
//    }
//
//    @Override
//    public void onRequestQposDisconnected() {
//        Log.d(TAG, "onRequestQposDisconnected");
//        IConnectionServiceCallback callback = callbackManager.getConnectionCallback();
//        if (callback != null) {
//            callback.onRequestQposDisconnected();
//        }
//    }
//
//    @Override
//    public void onReturnReversalData(@Nullable String tlv) {
//        Log.d(TAG, "onReturnReversalData: " + tlv);
//        IPaymentServiceCallback callback = callbackManager.getPaymentCallback();
//        if (callback != null) {
//            callback.onReturnReversalData(tlv);
//        }
//    }
//
//    @Override
//    public void onReturnGetPinResult(@Nullable Hashtable<String, String> result) {
//        Log.d(TAG, "onReturnGetPinResult: " + (result != null ? result.toString() : "null"));
//        IPaymentServiceCallback callback = callbackManager.getPaymentCallback();
//        if (callback != null) {
//            callback.onReturnGetPinResult(result);
//        }
//    }
//
//    @Override
//    public void onGetCardNoResult(@Nullable String cardNo) {
//        Log.d(TAG, "onGetCardNoResult: " + cardNo);
//        IPaymentServiceCallback callback = callbackManager.getPaymentCallback();
//        if (callback != null) {
//            callback.onGetCardNoResult(cardNo);
//        }
//    }
//
//    @Override
//    public void onGetCardInfoResult(@Nullable Hashtable<String, String> cardInfo) {
//        Log.d(TAG, "onGetCardInfoResult: " + (cardInfo != null ? cardInfo.toString() : "null"));
//        super.onGetCardInfoResult(cardInfo);
//        IPaymentServiceCallback callback = callbackManager.getPaymentCallback();
//        if (callback != null) {
//            callback.onGetCardInfoResult(cardInfo);
//        }
//    }
//
//    @Override
//    public void onRequestSetPin(boolean isOfflinePin, int tryNum) {
//        Log.d(TAG, "onRequestSetPin - offline: " + isOfflinePin + ", tryNum: " + tryNum);
//        super.onRequestSetPin(isOfflinePin, tryNum);
//        IPaymentServiceCallback callback = callbackManager.getPaymentCallback();
//        if (callback != null) {
//            callback.onRequestSetPin(isOfflinePin, tryNum);
//        }
//    }
//
//    @Override
//    public void onRequestSetPin() {
//        Log.d(TAG, "onRequestSetPin");
//        IPaymentServiceCallback callback = callbackManager.getPaymentCallback();
//        if (callback != null) {
//            callback.onRequestSetPin();
//        }
//    }
//
//    @SuppressLint("MissingPermission")
//    @Override
//    public void onDeviceFound(@Nullable BluetoothDevice device) {
//        Log.d(TAG, "onDeviceFound: " + (device != null ? device.getName() : "null"));
//        IConnectionServiceCallback callback = callbackManager.getConnectionCallback();
//        if (callback != null) {
//            callback.onDeviceFound(device);
//        }
//    }
//
//    @Override
//    public void onRequestDeviceScanFinished() {
//        Log.d(TAG, "onRequestDeviceScanFinished");
//        // Additional processing can be added here if needed
//    }
//
//    @Override
//    public void onTradeCancelled() {
//        Log.d(TAG, "onTradeCancelled");
//        IPaymentServiceCallback callback = callbackManager.getPaymentCallback();
//        if (callback != null) {
//            callback.onTradeCancelled();
//        }
//    }
//
//    @Override
//    public void onEmvICCExceptionData(@Nullable String tlv) {
//        Log.d(TAG, "onEmvICCExceptionData: " + tlv);
//        super.onEmvICCExceptionData(tlv);
//        IPaymentServiceCallback callback = callbackManager.getPaymentCallback();
//        if (callback != null) {
//            callback.onEmvICCExceptionData(tlv);
//        }
//    }
//
////    @Override
////    public void onReturnApduResult(boolean success, @Nullable String apduResponse, int responseCode) {
////        Log.d(TAG, "onReturnApduResult - success: " + success + ", responseCode: " + responseCode);
////        IPaymentServiceCallback callback = callbackManager.getPaymentCallback();
////        if (callback != null) {
////            callback.onReturnApduResult(success, apduResponse, responseCode);
////        }
////    }
//
//    @Override
//    public void onReturnCustomConfigResult(boolean isSuccess,String result){
//        Log.d(TAG, "onReturnCustomConfigResult - success: " + isSuccess+ ", result: " + result);
//        IPaymentServiceCallback callback = callbackManager.getPaymentCallback();
//        if (callback != null) {
//            callback.onReturnCustomConfigResult(isSuccess,result);
//        }
//    }

//    @Override
//    public void onReturnPowerOnIccResult(boolean isSuccess, String ksn,String atr, int atrLen){
//        Log.d(TAG, "onReturnPowerOnIccResult - success: " + isSuccess+ ", result: " + ksn);
//        IPaymentServiceCallback callback = callbackManager.getPaymentCallback();
//        if (callback != null) {
//            callback.onReturnPowerOnIccResult(isSuccess,ksn,atr,atrLen);
//        }
//    }

//    @Override
//    public void   onReturnPowerOffIccResult(boolean isSuccess){
//        Log.d(TAG, "onReturnPowerOffIccResult - success: " + isSuccess+ ", result: No");
//        IPaymentServiceCallback callback = callbackManager.getPaymentCallback();
//        if (callback != null) {
//            callback.onReturnPowerOffIccResult(isSuccess);
//        }
//    }
}