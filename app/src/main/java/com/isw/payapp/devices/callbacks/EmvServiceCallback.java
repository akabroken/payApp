package com.isw.payapp.devices.callbacks;

import java.util.List;

public interface EmvServiceCallback {
    void onWaitingStatusChanged(boolean waiting);
    void onShowPinPad(boolean show);
    void onLoading(String message);
    void onStopLoading();
    void onTransactionSuccess(String content);
    void onTransactionFailed(String message);
    void onTitleTextChanged(String title);
    void onSendDingTalkMessage(boolean success, String data);

    void onShowPinPadWithKeyboard(List<String> dataList, boolean isOnlinePin, boolean isChangePin);
    void onPinInputReceived(String value);

    void onError(String value);

    void onTransactionCancelled();

    void onDeviceConnected(String res);

    void onDeviceDisconnected(String res);

}
