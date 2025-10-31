package com.isw.payapp.devices.interfaces;

import com.isw.payapp.model.AmountData;
import com.isw.payapp.model.CandidateApp;
import com.isw.payapp.model.OnlineData;
import com.isw.payapp.model.PinData;

public interface PosCardReader {
    int onInputAmount(AmountData amountData);
    int onInputPin(PinData pinData);
    int onSelectApp(CandidateApp[] apps);
    int onOnlineProcess(OnlineData onlineData);
    int onRequireTagValue(int group, int tag, byte[] buffer);
    int onRequestDateTime(byte[] dateTimeBuffer);

    default int onSelectAppFail(int errorCode) { return 0; }
    default int onFinishReadAppData() { return 0; }
    default int onTradeCancelled() {return 0;}
}
