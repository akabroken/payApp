package com.isw.payapp.devices.interfaces;

import android.view.View;
import android.widget.EditText;

public interface IEmvProcessor {

    void initializeDevice() throws Exception;
    void initializeEmvService() throws Exception;
    void startEmvService() throws Exception;

    String getResponse();
    void cancelTransaction();

    // Add view setting methods
    void setViews(EditText pinpadEditText, View scvText, View tvReceipt, View btnSendReceipt);

}
