package com.isw.payapp.terminal.model;

import com.isw.payapp.terminal.accessors.CommsModelAccesor;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public  class CommsModel implements CommsModelAccesor {

    private  int id;
    private int port;
    private String transUrl;
    private int transPort;
    private String transIp;
    private String gprsUsername;
    private String gprsPwd;
    private String gprsPhone;
    private String apn;
    private String simPin;
    private String gateway;
    private String subnet;
    private String ipMode;
    private String primaryDNS;
    private String secondaryDNS;
    private String wifiSSID;
    private String wifiName;
    private String wifiPwd;
    private String friendlyName;
    private String commsMedia;
    private String vpnProvider;
    private String vpnConnectionName;
    private String vpnServername;
    private String vpnAddress;
    private String vpnSignInInfo;
    private String vpnUsername;
    private String vpnPwd;

    @Override
    public int id() {
        return 0;
    }

    @Override
    public int port() {
        return 0;
    }

    @Override
    public String transUrl() {
        return null;
    }

    @Override
    public int transPort() {
        return 0;
    }

    @Override
    public String gettransIp() {
        return null;
    }

    @Override
    public String getgprsUsername() {
        return null;
    }

    @Override
    public String getgprsPwd() {
        return null;
    }

    @Override
    public String getgprsPhone() {
        return null;
    }

    @Override
    public String getapn() {
        return null;
    }

    @Override
    public String getsimPin() {
        return null;
    }

    @Override
    public String getgateway() {
        return null;
    }

    @Override
    public String getsubnet() {
        return null;
    }

    @Override
    public String getipMode() {
        return null;
    }

    @Override
    public String getprimaryDNS() {
        return null;
    }

    @Override
    public String getsecondaryDNS() {
        return null;
    }

    @Override
    public String getwifiSSID() {
        return null;
    }

    @Override
    public String getwifiName() {
        return null;
    }

    @Override
    public String getwifiPwd() {
        return null;
    }

    @Override
    public String getfriendlyName() {
        return null;
    }

    @Override
    public String getcommsMedia() {
        return null;
    }

    @Override
    public String getvpnProvider() {
        return null;
    }

    @Override
    public String getvpnConnectionName() {
        return null;
    }

    @Override
    public String getvpnServername() {
        return null;
    }

    @Override
    public String getvpnAddress() {
        return null;
    }

    @Override
    public String getvpnSignInInfo() {
        return null;
    }

    @Override
    public String getvpnUsername() {
        return null;
    }

    @Override
    public String getvpnPwd() {
        return null;
    }
}
