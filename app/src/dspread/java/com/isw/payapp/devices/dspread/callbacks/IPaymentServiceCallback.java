package com.isw.payapp.devices.dspread.callbacks;

import com.dspread.xpos.QPOSService;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;

public interface IPaymentServiceCallback {

    // ==================== Core Transaction Callbacks ====================

    /**
     * Waiting for user operation (insert/swipe/tap card)
     */
    default void onRequestWaitingUser() {}

    /**
     * Request time
     */
    default void onRequestTime() {}

    /**
     * Request to select EMV application
     */
    default void onRequestSelectEmvApp(ArrayList<String> appList) {}

    /**
     * Request online processing
     */
    default void onRequestOnlineProcess(String tlv) {}

    void onRequestBatchData(String tlv);

    /**
     * Request to display message
     */
    default void onRequestDisplay(QPOSService.Display displayMsg) {}

    // ==================== PIN Related Callbacks ====================

    /**
     * PIN request result
     */
    default void onQposRequestPinResult(List<String> dataList, int offlineTime) {}

    /**
     * Request to set PIN
     */
    default void onRequestSetPin(boolean isOfflinePin, int tryNum) {}

    /**
     * Request to set PIN (no parameters)
     */
    default void onRequestSetPin() {}

    /**
     * Return PIN input result
     */
    default void onReturnGetPinInputResult(int num) {}

    /**
     * Get card information result
     */
    default void onGetCardInfoResult(Hashtable<String, String> cardInfo) {}

    default void onTransactionCompleted(PaymentResult result) {}

    void onRequestTransactionResult(QPOSService.TransactionResult transactionResult);

    default void onTransactionFailed(String errorMessage, String data) {}

    default void onReturnCustomConfigResult(boolean isSuccess,String result){}

    default void onDoTradeResult(QPOSService.DoTradeResult result, Hashtable<String, String> decodeData){}

    default void  onReturnGetPinResult(Hashtable<String, String> result){}

    default void  onGetCardNoResult(String cardNo){}

   default void onReturnUpdateIPEKResult(boolean b){}

    default void onRequestUpdateWorkKeyResult(QPOSService.UpdateInformationResult result){}
}
