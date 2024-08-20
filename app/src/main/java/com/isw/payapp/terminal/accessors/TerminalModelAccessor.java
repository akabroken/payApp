package com.isw.payapp.terminal.accessors;

import java.util.HashMap;

public interface TerminalModelAccessor {

    String getterminalId();
    String getserialNumber();
    String getmerchantId();
    String getterminalName();
    String getterminalModel();
    String getmerchantLocation();
    String getaddress();
    String getcurrencyCode();
    String getcountryCode();
    String getposCode();
    String getmerchantType();
    String getmerchantName();
    String getusername();
    String getpassword();
    int getid();
    int getstan();
    int getprinterStatus();
    String getuniqueId();
    int getbatteryInfo();
    String getlanguage();
    HashMap getStructuredData();
    HashMap getterminalInformation();
    HashMap getstructuredDataTag();
}
