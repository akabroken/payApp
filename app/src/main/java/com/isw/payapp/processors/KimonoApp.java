package com.isw.payapp.processors;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.room.Room;

import com.isw.payapp.database.Kimono;

public class KimonoApp  {
    private Kimono kimono;
    private Context context;

    public KimonoApp(Context context){
        this.context = context;
    }


    public void onCreate() {

     //   super.onCreate();

        kimono = Room.databaseBuilder(context,Kimono.class,"Kimono").fallbackToDestructiveMigration().build();
        Log.i("DATABASE","DATABASE CREATION SUCCESS");
    }

    public Kimono getKimono(){
        return kimono;
    }

}
