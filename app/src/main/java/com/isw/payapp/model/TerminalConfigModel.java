package com.isw.payapp.model;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TerminalConfigModel {
    @SerializedName("bank")
    private String bank;

    @SerializedName("mid")
    private String mid;

    @SerializedName("tid")
    private String tid;

    @SerializedName("merchantloc")
    private String merchantloc;

    @SerializedName("address1")
    private String address1;

    @SerializedName("address2")
    private String address2;

    @SerializedName("city")
    private String city;

    @SerializedName("state")
    private String state;

    @SerializedName("zip")
    private String zip;

    @SerializedName("currencycode")
    private String currencycode;

    @SerializedName("posCode")
    private String posCode;

    @SerializedName("mtype")
    private String mtype;

    @SerializedName("transip")
    private String transip;

    @SerializedName("transport")
    private String transport;

    @SerializedName("keysetid")
    private String keysetid;

    @SerializedName("loginurl")
    private String loginurl;

    @SerializedName("loginport")
    private String loginport;
    // Default constructor
    public TerminalConfigModel() {}
}
