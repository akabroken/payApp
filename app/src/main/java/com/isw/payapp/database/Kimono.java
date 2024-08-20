package com.isw.payapp.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.Transaction;

import com.isw.payapp.database.dao.TerminalDao;
import com.isw.payapp.database.dao.TransactionDao;
import com.isw.payapp.database.entities.Terminal;
import com.isw.payapp.database.entities.Transactions;

@Database(entities = {Transactions.class, Terminal.class},version = 2)
public abstract class Kimono extends RoomDatabase {
    //Transaction Table
    public abstract TransactionDao transactionDao();
    //Terminal
    public abstract TerminalDao terminalDao();
}
