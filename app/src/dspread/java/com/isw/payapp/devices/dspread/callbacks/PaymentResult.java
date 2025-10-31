package com.isw.payapp.devices.dspread.callbacks;

import java.io.Serializable;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentResult  implements Serializable {
    private boolean isConnected;
    private String status;
    private String amount;
    private String formatID;
    private String maskedPAN;
    private String expiryDate;
    private String cardHolderName;
    private String serviceCode;
    private String track1Length;
    private String track2Length;
    private String track3Length;
    private String encTracks;
    private String encTrack1;
    private String encTrack2;
    private String encTrack3;
    private String partialTrack;
    private String pinKsn;
    private String trackksn;
    private String pinBlock;
    private String encPAN;
    private String trackRandomNumber;
    private String pinRandomNumber;
    private String tlv;
    private String transactionType;
}
