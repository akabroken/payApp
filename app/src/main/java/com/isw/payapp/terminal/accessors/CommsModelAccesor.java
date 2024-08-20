package com.isw.payapp.terminal.accessors;

public interface CommsModelAccesor {

    int id();
    int port();
    String transUrl();
    int transPort();
    String gettransIp();
    String getgprsUsername();
    String getgprsPwd();
    String getgprsPhone();
    String getapn();
    String getsimPin();
    String getgateway();
    String getsubnet();
    String getipMode();
    String getprimaryDNS();
    String getsecondaryDNS();
    String getwifiSSID();
    String getwifiName();
    String getwifiPwd();
    String getfriendlyName();
    String getcommsMedia();
    String getvpnProvider();
    String getvpnConnectionName();
    String getvpnServername();
    String getvpnAddress();
    String getvpnSignInInfo();
    String getvpnUsername();
    String getvpnPwd();
}
