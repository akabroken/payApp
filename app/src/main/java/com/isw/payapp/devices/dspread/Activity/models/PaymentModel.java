package com.isw.payapp.devices.dspread.Activity.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentModel {
    private String date;
    private String amount;
    private String transType;
    private String transCurrencyCode;
    private String tvr;

    private String cardNo;
    private String cvmResults;
    private String cidData;
}
