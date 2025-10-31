package com.isw.payapp.model;

import com.isw.payapp.terminal.accessors.TerminalModelAccessor;

import java.util.HashMap;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TerminalModel  {


    private String terminalId;
    private String serialNumber;
    private String merchantId;
    private String terminalName;
    private String terminalModel;
    private String merchantLocation;
    private String address;
    private String currencyCode;
    private String countryCode;
    private String posCode;
    private String merchantType;
    private String merchantName;
    private String username;
    private String password;
    private int id;
    private int stan;
    private int printerStatus;
    private String uniqueId;
    private int batteryInfo;
    private String language;
    private HashMap StructuredData;
    private HashMap terminalInformation;
    private HashMap structuredDataTag;


    // Getter methods


}
