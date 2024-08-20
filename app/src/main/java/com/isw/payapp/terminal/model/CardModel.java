package com.isw.payapp.terminal.model;

import com.isw.payapp.terminal.accessors.CardModelAccessor;

import java.util.HashMap;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CardModel  {
    private int id;
    private String pan;
    private String exMonth;
    private String expYear;
    private String track2data;
    private HashMap pinData;
    private HashMap cardData;
    private String pinBlock;
    private String pinType;
    private String ksn;
    private String ksnd;
    private HashMap track2;
    private String KSNTag;

    //

//    @Override
//    public int getId() {
//        return id;
//    }
//
//    @Override
//    public String getPan(){
//        return pan;
//    }
//
//    @Override
//    public String getExMonth() {
//        return null;
//    }
//
//    @Override
//    public String getExpYear() {
//        return null;
//    }
//
//    @Override
//    public String getTrack2Data() {
//        return null;
//    }
//
//    @Override
//    public HashMap getPinData() {
//        return null;
//    }
//
//    @Override
//    public HashMap getCardData() {
//        return null;
//    }
//
//    @Override
//    public String getPinBlock() {
//        return null;
//    }
//
//    @Override
//    public String getPinType() {
//        return null;
//    }
//
//    @Override
//    public String getKsn() {
//        return null;
//    }
//
//    @Override
//    public String getKsnd() {
//        return null;
//    }
//
//    @Override
//    public HashMap getTrack2() {
//        return null;
//    }

}
