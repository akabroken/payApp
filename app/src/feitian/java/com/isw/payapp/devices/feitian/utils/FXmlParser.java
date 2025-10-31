package com.isw.payapp.devices.feitian.utils;

import static com.isw.payapp.devices.feitian.constants.IParamType.TYPE_APP_PARAM_EMV;

import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

import com.isw.payapp.devices.feitian.configBean.CAPublicKeyBean;
import com.isw.payapp.devices.feitian.configBean.CRLBean;
import com.isw.payapp.devices.feitian.configBean.EMVAcquirerParamsBean;
import com.isw.payapp.devices.feitian.configBean.EMVAppParamsBean;
import com.isw.payapp.devices.feitian.configBean.EMVCLAppParamsBean;
import com.isw.payapp.devices.feitian.configBean.EMVCLDRLBean;
import com.isw.payapp.devices.feitian.configBean.ExceptionListBean;
import com.isw.payapp.devices.feitian.configBean.XmlDataBean;
import com.isw.payapp.devices.feitian.constants.IParamType;
import com.jirui.logger.Logger;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FXmlParser {
    public static final String TAG = "XmlParser";

    /**
     * Parse XML file data into  a list of objects
     * @param type  Custom file types
     * @param data  data
     * @return list of objects
     * @throws XmlPullParserException XmlPullParserException
     * @throws IOException  IOException
     */
    public static List<XmlDataBean> parseXmlFile(IParamType type, byte[] data)
            throws XmlPullParserException, IOException {

        List<XmlDataBean> datas = null;
        XmlDataBean bean = null;

        XmlPullParser xmlParser = Xml.newPullParser();
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        xmlParser.setInput(bais, "UTF-8");
        Log.d(TAG, "Start parse file... ");

        int event = xmlParser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            switch (event) {
                case XmlPullParser.START_DOCUMENT:
                    datas = new ArrayList<XmlDataBean>();
                    break;
                case XmlPullParser.START_TAG:
                    String tagName = xmlParser.getName();
                    switch (type) {
                        case TYPE_EMV_ACQUIRER_PARAM:
                            if (tagName.equalsIgnoreCase("Body")) {
                                bean = new EMVAcquirerParamsBean();
                            } else {
                                if (xmlParser.next() == XmlPullParser.TEXT) {
                                    bean.setTagValue(tagName, xmlParser.getText());
                                } else {
                                    bean.setTagValue(tagName, "");
                                }
                            }
                            break;
                        case TYPE_APP_PARAM_EMV:
                        case TYPE_APP_PARAM_EMVCL:
                            if (tagName.equalsIgnoreCase("Body")) {
                            } else if (tagName.equalsIgnoreCase("Parameters")) {
                                if (type == TYPE_APP_PARAM_EMV) {
                                    bean = new EMVAppParamsBean();
                                    bean.setTagValue(tagName, xmlParser.getAttributeValue(0));
                                } else {
                                    bean = new EMVCLAppParamsBean();
                                    String parser2 = null;
                                    try {
                                        parser2 = xmlParser.getAttributeValue(2);

                                    } catch (IndexOutOfBoundsException e) {
                                        Logger.e("IndexOutOfBoundsException: no TransTypeGroup");
                                    }finally {
                                        if(TextUtils.isEmpty(parser2)){
                                            bean.setTagValue(tagName, xmlParser.getAttributeValue(0), xmlParser.getAttributeValue(1));
                                        }else{
                                            bean.setTagValue(tagName, xmlParser.getAttributeValue(0), xmlParser.getAttributeValue(1), parser2);
                                        }
                                    }
                                }
                            } else {
                                if (xmlParser.next() == XmlPullParser.TEXT) {
                                    bean.setTagValue(tagName, xmlParser.getText());
                                } else {
                                    bean.setTagValue(tagName, "");
                                }
                            }
                            break;
                        case TYPE_CA_PUBKEY:
                            if (tagName.equalsIgnoreCase("Body")) {
                            } else if (tagName.equalsIgnoreCase("PUBKEY")) {
                                bean = new CAPublicKeyBean();
                                bean.setTagValue(tagName, xmlParser.getAttributeValue(0), xmlParser.getAttributeValue(1));
                            } else {
                                if (xmlParser.next() == XmlPullParser.TEXT) {
                                    bean.setTagValue(tagName, xmlParser.getText());
                                } else {
                                    bean.setTagValue(tagName, "");
                                }
                            }
                            break;
                        case TYPE_EMVCL_DRL:
                            if (tagName.equalsIgnoreCase("Body")) {
                            } else if (tagName.equalsIgnoreCase("DRL")) {
                                bean = new EMVCLDRLBean();
                                bean.setTagValue(tagName, xmlParser.getAttributeValue(0), xmlParser.getAttributeValue(1));
                            } else {
                                if (xmlParser.next() == XmlPullParser.TEXT) {
                                    bean.setTagValue(tagName, xmlParser.getText());
                                } else {
                                    bean.setTagValue(tagName, "");
                                }
                            }
                            break;
                        case TYPE_CRL:
                            if (tagName.equalsIgnoreCase("Body")) {
                            } else if (tagName.equalsIgnoreCase("CRL")) {
                                bean = new CRLBean();
                                bean.setTagValue(tagName, xmlParser.getAttributeValue(0));
                            } else {
                                if (xmlParser.next() == XmlPullParser.TEXT) {
                                    bean.setTagValue(tagName, xmlParser.getText());
                                } else {
                                    bean.setTagValue(tagName, "");
                                }
                            }
                            break;
                        case TYPE_EXCEPTION_LIST:
                            if (tagName.equalsIgnoreCase("Body")) {
                            } else if (tagName.equalsIgnoreCase("EXCEPTIONlIST")) {
                                bean = new ExceptionListBean();
                                bean.setTagValue(tagName, xmlParser.getAttributeValue(0));
                            } else {
                                if (xmlParser.next() == XmlPullParser.TEXT) {
                                    bean.setTagValue(tagName, xmlParser.getText());
                                } else {
                                    bean.setTagValue(tagName, "");
                                }
                            }
                            break;
                        default:
                            break;
                    }
                    break;
                case XmlPullParser.END_TAG:
                    String endTag = xmlParser.getName();
                    switch (type) {
                        case TYPE_EMV_ACQUIRER_PARAM:
                            if (endTag.equalsIgnoreCase("Body")) {
                                datas.add(bean);
                                bean = null;
                            }
                            break;
                        case TYPE_APP_PARAM_EMV:
                        case TYPE_APP_PARAM_EMVCL:
                            if (endTag.equalsIgnoreCase("Parameters")) {
                                datas.add(bean);
                                bean = null;
                            }
                            break;
                        case TYPE_CA_PUBKEY:
                            if (endTag.equalsIgnoreCase("PUBKEY")) {
                                datas.add(bean);
                                bean = null;
                            }
                            break;
                        case TYPE_EMVCL_DRL:
                            if (endTag.equalsIgnoreCase("DRL")) {
                                datas.add(bean);
                                bean = null;
                            }
                            break;
                        case TYPE_CRL:
                            if (endTag.equalsIgnoreCase("CRL")) {
                                datas.add(bean);
                                bean = null;
                            }
                            break;
                        case TYPE_EXCEPTION_LIST:
                            if (endTag.equalsIgnoreCase("EXCEPTIONlIST")) {
                                datas.add(bean);
                                bean = null;
                            }
                            break;
                        default:
                            break;
                    }
                    break;
            }
            event = xmlParser.next();
        }

        return datas;
    }
}
