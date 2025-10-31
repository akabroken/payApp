package com.isw.payapp.interfaces;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import lombok.Data;

@Entity
@Data
public class TransactionEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String holderName;
    private String cardNum;
    private String amount;
    private String date;

}
