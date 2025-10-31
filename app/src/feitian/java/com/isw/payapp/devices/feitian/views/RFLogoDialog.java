package com.isw.payapp.devices.feitian.views;

import android.app.Activity;
import android.app.Dialog;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;

import com.isw.payapp.R;
import com.isw.payapp.devices.feitian.helpers.SvrHelper;

public class RFLogoDialog implements Handler.Callback{
    private Activity activity;
    private Dialog mDialog;
    private Handler handler = null;


    public RFLogoDialog(Activity activity) {
        this.activity = activity;
        handler = new Handler(activity.getMainLooper(), this);

        mDialog = new Dialog(activity, R.style.BaseDialog);
        View view;
        if (Build.MODEL.equals("F360")) {
            view = LayoutInflater.from(activity)
                    .inflate(R.layout.activity_rf_logo_360, null);
        } else {
            view = LayoutInflater.from(activity)
                    .inflate(R.layout.activity_rf_logo, null);
        }

        ((Button) view.findViewById(R.id.btn_cancel)).setOnClickListener(viewBtn -> {
            SvrHelper.instance().cancelOperation();
            dismiss();
        });
        mDialog.setContentView(view);
        mDialog.setCancelable(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
            mDialog.onAttachedToWindow();
        }
        mDialog.setCanceledOnTouchOutside(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            mDialog.setOnShowListener(dialog -> {
                if (mDialog.isShowing()) {
                    //
                }
            });
        }
    }

    /**
     * show dialog
     */
    public void show() {
        if (mDialog != null) {
            mDialog.show();
        }
    }

    /**
     * dismiss dialog
     */
    public void dismiss() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    @Override
    public boolean handleMessage(@NonNull Message message) {
        return false;
    }
}
