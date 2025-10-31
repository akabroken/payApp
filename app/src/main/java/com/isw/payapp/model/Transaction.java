package com.isw.payapp.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;


@Data
@Entity(tableName = "Transaction")
public class Transaction {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String amount;
    private String rate;
    private String settlementFee;
    private String settlementCurrencyCode;
    private String amountSettlement;
    private String surcharge;
    private String cardNumber;
    private String holderName;
    private String dateTime;

    public Transaction( String holderName, String cardNumber, String amount, String dateTime){
        this.amount = amount;
        this.cardNumber = cardNumber;
       // this.id = id;
        this.holderName = holderName;
        this.dateTime = dateTime;
    }


}
