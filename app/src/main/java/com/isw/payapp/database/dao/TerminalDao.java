package com.isw.payapp.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.isw.payapp.database.entities.Terminal;

import java.util.List;

@Dao
public interface TerminalDao {

    @Query("SELECT * FROM Terminal")
    List<Terminal> getAll();

    @Query("SELECT * FROM Terminal WHERE termId = :termId")
    List<Terminal> getOne(String termId);

    //Insert
    @Insert
    void createTerminal(Terminal ...terminals);

    //Update
    @Update
    void updateTerminal(Terminal terminal);

    //Delete
    @Delete
    void deleteTerminal(Terminal terminal);

}
