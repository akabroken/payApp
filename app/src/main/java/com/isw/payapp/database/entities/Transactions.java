package com.isw.payapp.database.entities;


import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter

@Entity(tableName = "Transactions")
public class Transactions {
    @PrimaryKey(autoGenerate = true)
    public int tranId;
    @ColumnInfo(name = "card_num")
    public String cardnum;
    @ColumnInfo(name = "tran_date")
    public String tranDate;
    @ColumnInfo(name="stan")
    public int stan;
    @ColumnInfo(name = "authId")
    public String authId;
    @ColumnInfo(name ="amount")
    public double amt;

}
