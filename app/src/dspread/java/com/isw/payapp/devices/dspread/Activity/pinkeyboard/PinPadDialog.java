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

    /**
     * Sets a custom PinPadView for the dialog
     * @param pinPadView the PinPadView instance to set
     * @return the current PinPadDialog instance for method chaining
     */
    public PinPadDialog setPinPadView(PinPadView pinPadView) {
        if (pinPadView == null) {
            throw new IllegalArgumentException("PinPadView cannot be null");
        }

        this.pinPadView = pinPadView;

        // Update the dialog's view if it's already created
        if (mDialog != null) {
            try {
                View dialogLayout = LayoutInflater.from(mContext).inflate(R.layout.view_paypass_dialog, null);
                LinearLayout container = dialogLayout.findViewById(R.id.pay_View);

                if (container != null) {
                    // Remove existing PinPadView if any
                    container.removeAllViews();
                    // Add the new PinPadView
                    container.addView(pinPadView);
                }

                mDialog.setView(dialogLayout);
            } catch (Exception e) {
                // Fallback: reinitialize the dialog with the new PinPadView
                initDialogWithCustomView(pinPadView);
            }
        }

        return this;
    }

    /**
     * Alternative method to set PinPadView with custom layout parameters
     * @param pinPadView the PinPadView instance to set
     * @param layoutParams the layout parameters for the PinPadView
     * @return the current PinPadDialog instance for method chaining
     */
    public PinPadDialog setPinPadView(PinPadView pinPadView, LinearLayout.LayoutParams layoutParams) {
        if (pinPadView == null) {
            throw new IllegalArgumentException("PinPadView cannot be null");
        }

        this.pinPadView = pinPadView;

        if (mDialog != null) {
            try {
                View dialogLayout = LayoutInflater.from(mContext).inflate(R.layout.view_paypass_dialog, null);
                LinearLayout container = dialogLayout.findViewById(R.id.pay_View);

                if (container != null) {
                    container.removeAllViews();
                    if (layoutParams != null) {
                        container.addView(pinPadView, layoutParams);
                    } else {
                        container.addView(pinPadView);
                    }
                }

                mDialog.setView(dialogLayout);
            } catch (Exception e) {
                initDialogWithCustomView(pinPadView);
            }
        }

        return this;
    }

    /**
     * Reinitializes the dialog with a custom PinPadView
     * @param customPinPadView the custom PinPadView to use
     */
    private void initDialogWithCustomView(PinPadView customPinPadView) {
        try {
            // Create a simple container for the custom view
            LinearLayout container = new LinearLayout(mContext);
            container.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            container.setOrientation(LinearLayout.VERTICAL);
            container.addView(customPinPadView);

            mDialog = new AlertDialog.Builder(mContext, R.style.dialog_pay_theme)
                    .setView(container)
                    .setCancelable(true)
                    .create();

            // Set dialog window properties
            window = mDialog.getWindow();
            if (window != null) {
                window.setDimAmount(0.4f);
                window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                window.setGravity(Gravity.BOTTOM);
                window.setWindowAnimations(R.style.dialogOpenAnimation);
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
            }

            mDialog.setCanceledOnTouchOutside(false);

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize PinPadDialog with custom view", e);
        }
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
     * Sets the dialog title (if supported by the layout)
     * @param title the title text
     */
    public void setTitle(String title) {
        // This method can be expanded if your layout supports a title view
        if (mDialog != null) {
            mDialog.setTitle(title);
        }
    }

    /**
     * Sets the dialog message (if supported by the layout)
     * @param message the message text
     */
    public void setMessage(String message) {
        // This method can be expanded if your layout supports a message view
        // Currently, AlertDialog's default message view is not used in custom layout
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
        if (pinPadView != null) {
            pinPadView.cleanup();
            pinPadView = null;
        }
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