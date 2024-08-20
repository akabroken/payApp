package com.isw.payapp.terminal.model;

import com.isw.payapp.terminal.accessors.TerminalModelAccessor;

import java.util.HashMap;
import lombok.Data;


public class TerminalModel implements TerminalModelAccessor {


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

    @Override
    public String getterminalId() {
        return terminalId;
    }

    @Override
    public String getserialNumber() {
        return serialNumber;
    }

    @Override
    public String getmerchantId() {
        return merchantId;
    }

    @Override
    public String getterminalName() {
        return terminalName;
    }

    @Override
    public String getterminalModel() {
        return terminalModel;
    }

    @Override
    public String getmerchantLocation() {
        return merchantLocation;
    }

    @Override
    public String getaddress() {
        return address;
    }

    @Override
    public String getcurrencyCode() {
        return currencyCode;
    }

    @Override
    public String getcountryCode() {
        return countryCode;
    }

    @Override
    public String getposCode() {
        return posCode;
    }

    @Override
    public String getmerchantType() {
        return merchantType;
    }

    @Override
    public String getmerchantName() {
        return merchantName;
    }

    @Override
    public String getusername() {
        return username;
    }

    @Override
    public String getpassword() {
        return password;
    }

    @Override
    public int getid() {
        return id;
    }

    @Override
    public int getstan() {
        return stan;
    }

    @Override
    public int getprinterStatus() {
        return printerStatus;
    }

    @Override
    public String getuniqueId() {
        return uniqueId;
    }

    @Override
    public int getbatteryInfo() {
        return batteryInfo;
    }

    @Override
    public String getlanguage() {
        return language;
    }

    @Override
    public HashMap getStructuredData() {
        return StructuredData;
    }

    @Override
    public HashMap getterminalInformation() {
        return terminalInformation;
    }

    @Override
    public HashMap getstructuredDataTag() {
        return structuredDataTag;
    }

    // Getter methods


}
