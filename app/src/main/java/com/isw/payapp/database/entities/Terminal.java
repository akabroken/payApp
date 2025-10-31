package com.isw.payapp.database.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

@Entity(tableName = "Terminal")
public class Terminal {
    @PrimaryKey(autoGenerate = true)
    public int tId;

    @ColumnInfo(name = "termId")
    public String terminalId;

    @ColumnInfo(name = "merchId")
    public String merchantId;

    @ColumnInfo(name = "termLocation")
    public String location;

    @ColumnInfo(name = "address1")
    public String address1;

    @ColumnInfo(name = "address2")
    public String address2;

    @ColumnInfo(name = "merchType")
    public String merchType;
}
