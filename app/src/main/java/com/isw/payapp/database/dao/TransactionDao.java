package com.isw.payapp.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.isw.payapp.database.entities.Transactions;

import java.util.List;

@Dao
public interface TransactionDao {
    @Query("SELECT * FROM Transactions")
    List<Transactions> getAll();

    @Query("SELECT * FROM Transactions WHERE tranId IN (:tranId)")
    List<Transactions>getByIds(int[] tranId);

    @Query("SELECT * FROM Transactions WHERE tranId = :tranId")
    List<Transactions>getById(int tranId);

    @Insert
    void createTrans(Transactions ...transaction);

    @Update
    void updateTrans(Transactions transaction);

    @Delete
    void deleteTrans(Transactions transaction);

}
