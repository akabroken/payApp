package com.isw.payapp.devices.feitian.views;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.ftpos.library.smartpos.datautils.BytesTypeValue;
import com.ftpos.library.smartpos.emv.Emv;
import com.ftpos.library.smartpos.emv.IPinBlockFormat;
import com.ftpos.library.smartpos.errcode.ErrCode;
import com.ftpos.library.smartpos.keymanager.KeyType;
import com.ftpos.library.smartpos.pin.OnPinInputListener;
import com.ftpos.library.smartpos.pin.PinSeting;
import com.ftpos.library.smartpos.keymanager.KeyManager;
import com.ftpos.library.smartpos.util.BytesUtils;
import com.isw.payapp.R;

public class FeitianPinPadView extends LinearLayout {

    private static final String TAG = "FeitianPinPadView";
    private Button[] mTvDigits;
    private Button mBtnCancel;
    private View mLlDelete;
    private Button mTvOK;
    private LinearLayout keyboard;
    private EditText mPinDisplay;

    private PinSeting pinSeting;
    private Emv emv;
    private KeyManager keyManager;
    private String pan;
    public OnPayClickListener mPayClickListener;

    // Add dismiss listener
    private OnDismissListener mDismissListener;
    private StringBuilder currentPin = new StringBuilder();

    // Add debugging flag
    private boolean enableDebugLogging = true;

    public interface OnPayClickListener {
        void onCancel();
        void onPayPass();
        void onConfirm(byte[] encryptedPinBlock);
    }

    // Add dismiss listener interface
    public interface OnDismissListener {
        void onDismiss();
    }

    public FeitianPinPadView(Context context, Emv emv, KeyManager keyManager, String pan) {
        super(context);
        this.emv = emv;
        this.keyManager = keyManager;
        this.pan = pan;

        initView((Activity) context);
    }

    public FeitianPinPadView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView((Activity) context);
    }

    private void initView(Activity context) {
        LayoutInflater.from(context).inflate(R.layout.layout_pin_fetian, this, true);

        // Initialize buttons
        mTvDigits = new Button[10];
        mTvDigits[0] = findViewById(R.id.button0);
        mTvDigits[1] = findViewById(R.id.button1);
        mTvDigits[2] = findViewById(R.id.button2);
        mTvDigits[3] = findViewById(R.id.button3);
        mTvDigits[4] = findViewById(R.id.button4);
        mTvDigits[5] = findViewById(R.id.button5);
        mTvDigits[6] = findViewById(R.id.button6);
        mTvDigits[7] = findViewById(R.id.button7);
        mTvDigits[8] = findViewById(R.id.button8);
        mTvDigits[9] = findViewById(R.id.button9);

        keyboard = findViewById(R.id.container);
        mBtnCancel = findViewById(R.id.btn_cancel);
        mLlDelete = findViewById(R.id.btn_clean);
        mTvOK = findViewById(R.id.btn_confirm);
        mPinDisplay = findViewById(R.id.et_pin_display);

        // Setup button listeners for visual feedback
        setupButtonListeners();
        setupPinSetting();
    }

    private void closeDialog() {
        // Clean up resources
        mTvDigits[0] = null;
        mTvDigits[1] = null;
        mTvDigits[2] = null;
        mTvDigits[3] = null;
        mTvDigits[4] = null;
        mTvDigits[5] = null;
        mTvDigits[6] = null;
        mTvDigits[7] = null;
        mTvDigits[8] = null;
        mTvDigits[9] = null;

        keyboard = null;
        mBtnCancel = null;
        mLlDelete = null;
        mTvOK = null;
        mPinDisplay = null;
    }

    private void setupButtonListeners() {
        // Number buttons
        for (int i = 0; i < mTvDigits.length; i++) {
            final int digit = i;
            mTvDigits[i].setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentPin.length() < 12) {
                        currentPin.append(digit);
                        logPinInput("Button pressed", digit, currentPin.toString());
                        updatePinDisplay();
                    } else {
                        logPinInput("Max PIN length reached", digit, currentPin.toString());
                    }
                }
            });
        }

        // Delete button
        mLlDelete.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPin.length() > 0) {
                    char removedChar = currentPin.charAt(currentPin.length() - 1);
                    currentPin.deleteCharAt(currentPin.length() - 1);
                    logPinInput("Delete pressed", Character.getNumericValue(removedChar), currentPin.toString());
                    updatePinDisplay();
                } else {
                    logPinInput("Delete pressed - PIN already empty", -1, currentPin.toString());
                }
            }
        });

        // Cancel button
        mBtnCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                logPinInput("Cancel button pressed", -1, currentPin.toString());
                // Call cancel listener
                if (mPayClickListener != null) {
                    mPayClickListener.onCancel();
                }
                // Dismiss the dialog
                dismissDialog();
            }
        });

        // OK/Confirm button
        mTvOK.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                logPinInput("Confirm button pressed - Final PIN", -1, currentPin.toString());

                // The actual PIN processing will be handled by Feitian SDK
                // This is just for visual feedback
                if (currentPin.length() >= 4) {
                    // The actual encryption happens in the Feitian SDK
                    // We just show that we're processing
                    Log.i(TAG, "Processing PIN: " + currentPin.toString() + " (Length: " + currentPin.length() + ")");
                    mPinDisplay.setText("Processing...");

                    // Note: The actual dismiss will happen in onSuccess callback
                    // from the Feitian SDK after PIN encryption
                } else {
                    // If PIN is too short, show message but don't dismiss
                    Log.w(TAG, "PIN too short: " + currentPin.length() + " digits (minimum 4 required)");
                    mPinDisplay.setText("PIN too short (min 4 digits)");
                    mPinDisplay.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            updatePinDisplay();
                        }
                    }, 1000);
                }
                // Don't dismiss here - let the Feitian SDK callbacks handle it
                // dismissDialog();
            }
        });
    }

    // Enhanced logging method
    private void logPinInput(String action, int digit, String currentPinValue) {
        if (enableDebugLogging) {
            String digitStr = (digit >= 0) ? String.valueOf(digit) : "N/A";
            String pinDisplay = getMaskedPin(currentPinValue);

            Log.d(TAG, "PIN Input Debug - " +
                    "Action: " + action +
                    ", Digit: " + digitStr +
                    ", Current PIN: " + currentPinValue +
                    ", Display: " + pinDisplay +
                    ", Length: " + currentPinValue.length());
        }
    }

    private String getMaskedPin(String pin) {
        if (pin == null || pin.isEmpty()) return "[Empty]";
        StringBuilder masked = new StringBuilder();
        for (int i = 0; i < pin.length(); i++) {
            masked.append("*");
        }
        return masked.toString();
    }

    // Add this method to programmatically dismiss
    public void dismissDialog() {
        try {
            logPinInput("Dialog dismissed", -1, currentPin.toString());
            if (mDismissListener != null) {
                mDismissListener.onDismiss();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error dismissing dialog", e);
            e.printStackTrace();
        }
    }

    public void setPayClickListener(OnPayClickListener listener) {
        this.mPayClickListener = listener;
    }

    // Add setter for dismiss listener
    public void setOnDismissListener(OnDismissListener listener) {
        this.mDismissListener = listener;
    }

    // Enable/disable debug logging
    public void setEnableDebugLogging(boolean enable) {
        this.enableDebugLogging = enable;
    }

    private void updatePinDisplay() {
        if (mPinDisplay != null) {
            StringBuilder display = new StringBuilder();
            for (int i = 0; i < currentPin.length(); i++) {
                display.append("*");
            }
            mPinDisplay.setText(display.toString());
            Log.v(TAG, "PIN Display updated: " + display.toString() + " (Actual: " + currentPin.toString() + ")");
        }
    }

    private void setupPinSetting() {
        pinSeting = new PinSeting(getContext(), emv);
        pinSeting.setButtonCancel(mBtnCancel);
        pinSeting.setButtonNum(mTvDigits);
        pinSeting.setButtonDel(mLlDelete);
        pinSeting.setButtonOK(mTvOK);
        pinSeting.setButtonKeyboard(keyboard);

       // pinSeting.setRandomkeyboard(true);
        pinSeting.setMaxPinLen(12);
        pinSeting.setMinPinLen(4);

        pinSeting.setOnlinePinBlockFormat(IPinBlockFormat.BLOCK_FORMAT_0);  // Format 0
        pinSeting.setOnlinePinKeyIndex(0x01);  // Key index
        pinSeting.setOnlinePinKeyType(KeyType.KEY_TYPE_IPEK);
        pinSeting.setTimeout(30);
        pinSeting.setOnlinePinByPass(false);

        Log.i(TAG, "CARD NUMBER ::" + pan);
        if (pan != null) {
            pinSeting.setPan(pan);
        }
    }

    public void startPinInput() {
        Log.d(TAG, "Starting PIN input process");
        Log.d(TAG, "PAN: " + (pan != null ? pan : "null"));

        int ret;
        int ipekKeyIndex = 0x01;
        // The current KSN is displayed
        ipekKeyIndex = 0x01;
        BytesTypeValue ksnBytesTypeValue = new BytesTypeValue();
        ret = keyManager.exportDukptKsn(KeyType.KEY_TYPE_IPEK, ipekKeyIndex, ksnBytesTypeValue);
        if (ret == ErrCode.ERR_SUCCESS) {
            // After import , exportDukptKsn will be FFFF9876543210E00001(hex format)
            Log.d(TAG, "Current KSN:" + BytesUtils.byte2HexStr(ksnBytesTypeValue.getData()));
        } else {
            Log.e(TAG, "Failed to export KSN: " + ErrCode.toString(ret));
        }

        if (emv == null || pinSeting == null) {
            Log.e(TAG, "EMV or PinSeting is null - cannot start PIN input");
            if (mPayClickListener != null) {
                mPayClickListener.onCancel();
            }
            dismissDialog();
            return;
        }

        // Clear any previous PIN
        currentPin.setLength(0);
        updatePinDisplay();

        Log.i(TAG, "Starting Feitian SDK PIN input");
        emv.StartPinInput(pinSeting, new OnPinInputListener() {
            @Override
            public void onDispalyPin(int pinLength, int maxPinLength) {
                // This callback is called when PIN display should be updated
                // Feitian SDK handles the actual PIN input security
                Log.i(TAG, "Feitian SDK PIN Update - Current Length: " + pinLength + ", Max Length: " + maxPinLength);

                // Sync our currentPin with the SDK's state
                currentPin.setLength(pinLength);
                logPinInput("SDK PIN Update", -1, "SDK_Length_" + pinLength);
                updatePinDisplay();
            }

            @Override
            public void onSuccess(byte[] encryptedPinBlock) {
                Log.i(TAG, "PIN encryption successful - Encrypted PIN Block: " +
                        (encryptedPinBlock != null ? BytesUtils.byte2HexStr(encryptedPinBlock) : "null"));

                if (mPayClickListener != null) {
                    int ret;
                    int ipekKeyIndex = 0x01;
                    // The current KSN is displayed
                    ret = keyManager.increaseKSN(KeyType.KEY_TYPE_IPEK, ipekKeyIndex);
                    if (ret != ErrCode.ERR_SUCCESS) {
                        Log.e(TAG, "Failed to increase KSN: " + ErrCode.toString(ret));
                    } else {
                        Log.d(TAG, "KSN increased successfully");
                    }
                    Log.d(TAG, "Card Number: " + pinSeting.getPan());
                    mPayClickListener.onConfirm(encryptedPinBlock);
                }

                // Dismiss after successful PIN encryption
                dismissDialog();
            }

            @Override
            public void onError(int errorCode) {
                Log.e(TAG, "PIN input error: " + ErrCode.toString(errorCode) + " (" + errorCode + ")");
                // Reset display on error
                currentPin.setLength(0);
                updatePinDisplay();

                if (mPayClickListener != null) {
                    mPayClickListener.onCancel();
                }
                // Dismiss on error
                dismissDialog();
            }

            @Override
            public void onTimeout() {
                Log.w(TAG, "PIN input timeout");
                // Reset display on timeout
                currentPin.setLength(0);
                updatePinDisplay();

                if (mPayClickListener != null) {
                    mPayClickListener.onCancel();
                }
                // Dismiss on timeout
                dismissDialog();
            }

            @Override
            public void onCancel() {
                Log.i(TAG, "PIN input cancelled by user");
                // Reset display on cancel
                currentPin.setLength(0);
                updatePinDisplay();

                if (mPayClickListener != null) {
                    mPayClickListener.onCancel();
                }
                // Dismiss on cancel
                dismissDialog();
            }

            @Override
            public void onSetDigits(Object o, char c) {
                // Handle digit setting - this is for internal SDK use
                Log.v(TAG, "onSetDigits called - Object: " + o + ", Char: " + c);
            }
        });
    }

    public void cleanup() {
        Log.d(TAG, "Cleaning up FeitianPinPadView - Final PIN state: " + currentPin.toString());
        if (pinSeting != null) {
            dismissDialog();
            // Clean up resources if needed
        }
        currentPin.setLength(0);
        Log.d(TAG, "FeitianPinPadView cleanup completed");
    }
}