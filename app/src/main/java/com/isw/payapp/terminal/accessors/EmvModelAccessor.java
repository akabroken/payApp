package com.isw.payapp.terminal.accessors;

import java.util.HashMap;

public interface EmvModelAccessor {

    String getPosDataCodeDef();
    int GetId();
    String getposDataCodeMag();
    String getposDataCodeEmv();
    String getAmountAuthorized();
    String getApplicationInterchangeProfile();
    String getatc();
    String getCryptogram();
    String getCryptogramInformationData();
    String getCvmResults();
    String getiad();
    String getTerminalVerificationResult();
    String getTerminalType();
    String getTerminalCapabilities();
    String getTransactionDate();
    String getTransactionType();
    String getUnpredictableNumber();
    String getDedicatedFileName();
    String getPanSequenceNo();
    String getCustomerName();
    String getApplicationUsageControl();
    String getApplicationIdentifier();
    String getCvmList();
    String getTerminalApplicationVersionNumber();
    String getTransactionSequenceCounter();
    HashMap getEmvData();
}
