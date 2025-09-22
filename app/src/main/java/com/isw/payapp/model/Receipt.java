package com.isw.payapp.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Receipt{
    private TransactionData transactionData;
    private String bank;
    private String merchant;
    private String terminalId;
    private String amount;
    private String currency;
    private String dateTime;
    private String transactionType;
    private String cardNumber;
    private String entryMode;
    private String aid;
    private String atc;
    private String tvr;
    private String response;
}
