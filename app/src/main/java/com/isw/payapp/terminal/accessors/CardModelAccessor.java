package com.isw.payapp.terminal.accessors;

import java.util.HashMap;

public interface CardModelAccessor {
    int getId();
    String getPan();
    String getExMonth();
    String getExpYear();
    String getTrack2Data();
    HashMap getPinData();
    HashMap getCardData();
    String getPinBlock();
    String getPinType();
    String getKsn();
    String getKsnd();
    HashMap getTrack2();
}
