package com.isw.payapp.devices.feitian.configBean;

import android.text.TextUtils;

import java.util.regex.Pattern;

/**
 * @author GuoJirui.
 * @date 2021/4/26.
 * @desc
 */
public abstract class XmlDataBean {
    public abstract void setTagValue(String name, String... value);

    public abstract byte[] getBytes();

    public abstract int getTlvLens();

    protected String trimSpace(String tmp) {
        return (!TextUtils.isEmpty(tmp)) ? Pattern.compile("\\s*|\t|\r|\n").matcher(tmp).replaceAll("") : "";
    }



}
