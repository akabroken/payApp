package com.isw.payapp.terminal.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransactionModel {
    private String amount;
    private String rate;
    private String settlementFee;
    private String settlementCurrencyCode;
    private String amountSettlement;
    private String surcharge;

}
