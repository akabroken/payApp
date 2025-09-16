package com.isw.payapp.devices.dspread.Activity.pinkeyboard;

import android.app.AlertDialog;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.isw.payapp.R;

public class PinPadDialog {

    private AlertDialog mDialog;
    private Window window;
    private Context mContext;
    private PinPadView pinPadView;
    private OnDismissListener onDismissListener;

    public interface OnDismissListener {
        void onDismiss();
    }

    public PinPadDialog(@NonNull Context context) {
        this.mContext = context;
        initDialog();
    }

    private void initDialog() {
        try {
            View dialogLayout = LayoutInflater.from(mContext).inflate(R.layout.view_paypass_dialog, null);
            pinPadView = dialogLayout.findViewById(R.id.pay_View);

            mDialog = new AlertDialog.Builder(mContext, R.style.dialog_pay_theme)
                    .setView(dialogLayout)
                    .setCancelable(true)
                    .create();

            // Set dialog window properties
            window = mDialog.getWindow();
            if (window != null) {
                window.setDimAmount(0.4f);
                window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                window.setGravity(Gravity.BOTTOM);
                window.setWindowAnimations(R.style.dialogOpenAnimation);

                // Make dialog focusable and prevent keyboard from pushing it up
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
            }

            mDialog.setCanceledOnTouchOutside(false);

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize PinPadDialog", e);
        }
    }

    public void show() {
        if (mDialog != null && !mDialog.isShowing()) {
            try {
                mDialog.show();
            } catch (Exception e) {
                // Handle window manager bad token exception
                // This can happen if the context is no longer valid
            }
        }
    }

    public PinPadView getPayViewPass() {
        return pinPadView;
    }

    public void dismiss() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
            if (onDismissListener != null) {
                onDismissListener.onDismiss();
            }
        }
    }

    public boolean isShowing() {
        return mDialog != null && mDialog.isShowing();
    }

    public void setOnDismissListener(OnDismissListener listener) {
        this.onDismissListener = listener;
    }

    public void setCancelable(boolean cancelable) {
        if (mDialog != null) {
            mDialog.setCancelable(cancelable);
            mDialog.setCanceledOnTouchOutside(cancelable);
        }
    }

    /**
     * Safely cleanup resources to prevent memory leaks
     */
    public void cleanup() {
        if (mDialog != null) {
            if (mDialog.isShowing()) {
                mDialog.dismiss();
            }
            mDialog = null;
        }
        window = null;
        pinPadView = null;
        onDismissListener = null;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            cleanup();
        } finally {
            super.finalize();
        }
    }
}