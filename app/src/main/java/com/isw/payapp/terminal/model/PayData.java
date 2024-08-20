package com.isw.payapp.terminal.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PayData {
    private String Amount;
    private String AuthCode;
    private String TransCnt;
    private String kimonoData;
    private String paymentApp;
    private String paymentReqTag;
    private String cardType;
    private String posEntryMode;
}
