package com.isw.payapp.model;


import lombok.Data;

@Data
public class SslModel {

    private Boolean defaultSSL;
    private Boolean encryptData;
    private Boolean usessl14kd;
    private String sslServerCert;
    private String sslMyCert;
    private String sslKeyFile;
    private String sslKeyPassword;
}
