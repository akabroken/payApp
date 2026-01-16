package com.isw.payapp.devices.newtelpo.EMV;

import android.content.Context;

import com.telpo.emv.PureListener;

public class VervePure {

    private final Context context;
    private final EMVHandler emvHandler;

    public VervePure(Context context) {
        this.context = context;
        this.emvHandler = new EMVHandler(context);
    }

    PureListener pureListener = new PureListener() {

        @Override
        public int onPure_InitApp() {
            return 0;
        }

        @Override
        public int onPure_Check_Exception(int i, String s) {
            return 0;
        }

        @Override
        public int onPure_SendMsg(int i, int i1) {
            return 0;
        }

        @Override
        public int onPure_2ndTap(int i) {
            return 0;
        }
    };

}
