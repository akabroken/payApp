package com.isw.payapp.model;

import com.isw.payapp.terminal.accessors.EmvModelAccessor;

import java.util.HashMap;

import lombok.Data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class EmvModel {

    private String posDataCodeDef;
    private int id;
    private String posDataCodeMag;
    private String posDataCodeEmv;
    private String AmountAuthorized;
    private String ApplicationInterchangeProfile;
    private String atc;
    private String Cryptogram;
    private String CryptogramInformationData;
    private String CvmResults;
    private String iad;
    private String TerminalVerificationResult;
    private String TerminalType;
    private String TerminalCapabilities;
    private String TransactionDate;
    private String TransactionType;
    private String UnpredictableNumber;
    private String DedicatedFileName;
    private String PanSequenceNo;
    private String CustomerName;
    private String ApplicationUsageControl;
    private String ApplicationIdentifier;
    private String CvmList;
    private String TerminalApplicationVersionNumber;
    private String TransactionSequenceCounter;
    private HashMap emvData;
    private String AmountOther;
    private String issuerApplicationData;
    private String TransactionCurrencyCode;
    private String TerminalCountryCode;
    private String ServiceCode;

    private String pan;
    private String exMonth;
    private String expYear;
    private String track2data;
    private String track1data;
    private HashMap pinData;
    private HashMap cardData;
    private String pinBlock;
    private String pinType;
    private String CarSeqNo;
    private String ksn;
    private String ksnd;
    private HashMap track2;
    private String kimonoData;
    private HashMap structuredData;

}
