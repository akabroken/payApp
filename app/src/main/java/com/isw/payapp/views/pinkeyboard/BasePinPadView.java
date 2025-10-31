package com.isw.payapp.views.pinkeyboard;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.isw.payapp.R;
import com.isw.payapp.devices.PosServiceFactory;
import com.isw.payapp.devices.interfaces.IPosService;
//import com.jirui.logger.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BasePinPadView extends RelativeLayout {

    private String TAG = "BasePinPadView";

    protected Activity mContext;
    protected GridView mGridView;
    protected String savePwd = "";
    protected List<Integer> listNumber;
    protected View mPassLayout;
    protected boolean isRandom;
    protected EditText mEtinputpin;
    protected IPosService pinPadService;
    protected String pinData = "";
    protected BaseAdapter adapter;
    protected boolean isViewAdded = false;

    public interface OnPayClickListener {
        void onCancel();
        void onPayPass();
        void onConfirm(String password);
    }

    protected OnPayClickListener mPayClickListener;

    public void setPayClickListener(OnPayClickListener listener) {
        mPayClickListener = listener;
    }

    public BasePinPadView(Context context) {
        super(context);
        init((Activity) context);
    }

    public BasePinPadView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init((Activity) context);
    }

    public BasePinPadView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init((Activity) context);
    }

    private void init(Activity context) {
        this.mContext = context;
        this.pinPadService = createPinPadService();
        initView();
    }

    protected IPosService createPinPadService() {
        return PosServiceFactory.createPinPadService(mContext);
    }

    private void initView() {
        try {
            Log.d(TAG,"BasePinPadView: Starting initView");

            // Remove any existing views first
            removeAllViews();

            // Inflate layout directly into this view
            LayoutInflater.from(mContext).inflate(R.layout.view_paypass_layout, this, true);

            // Find views
            mEtinputpin = findViewById(R.id.et_inputpin);
            mGridView = findViewById(R.id.gv_pass);
            mPassLayout = this; // We're using this as the main layout

            if (mEtinputpin == null) {
                Log.e(TAG,"BasePinPadView: mEtinputpin is null after inflation");
                return;
            } else {
                Log.d(TAG,"BasePinPadView: mEtinputpin found successfully");
            }

            if (mGridView == null) {
                Log.e(TAG,"BasePinPadView: mGridView is null after inflation");
                return;
            } else {
                Log.d(TAG,"BasePinPadView: mGridView found successfully");
            }

            setupEditText();
            initData();

            Log.d(TAG,"BasePinPadView: initView completed successfully");

        } catch (Exception e) {
            Log.e(TAG,"BasePinPadView: Error in initView: " + e.getMessage());
            e.printStackTrace();
            createLayoutProgrammatically();
        }
    }

    private void setupEditText() {
        mEtinputpin.setHint("Enter PIN");
        mEtinputpin.setText("");
        mEtinputpin.setEnabled(true);
        mEtinputpin.setFocusable(false);
        mEtinputpin.setFocusableInTouchMode(false);
        mEtinputpin.setClickable(false);

        // Add text watcher for debugging
        mEtinputpin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d(TAG,"BasePinPadView: EditText text changed to: " + s.toString() + " length: " + s.length());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void createLayoutProgrammatically() {
        try {
            Log.d(TAG,"BasePinPadView: Creating layout programmatically");

            // Clear any existing views
            removeAllViews();

            // Create EditText
            mEtinputpin = new EditText(mContext);
            RelativeLayout.LayoutParams editTextParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            editTextParams.setMargins(50, 50, 50, 20);
            mEtinputpin.setLayoutParams(editTextParams);
            mEtinputpin.setId(R.id.et_inputpin);
            mEtinputpin.setHint("Enter PIN");
            mEtinputpin.setTextSize(18);
            mEtinputpin.setEnabled(false);
            mEtinputpin.setFocusable(false);
            setupEditText();

            // Create GridView
            mGridView = new GridView(mContext);
            RelativeLayout.LayoutParams gridParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT
            );
            gridParams.addRule(RelativeLayout.BELOW, mEtinputpin.getId());
            gridParams.setMargins(20, 100, 20, 20);
            mGridView.setLayoutParams(gridParams);
            mGridView.setId(R.id.gv_pass);
            mGridView.setNumColumns(3);
            mGridView.setVerticalSpacing(10);
            mGridView.setHorizontalSpacing(10);

            // Add views to this layout
            addView(mEtinputpin);
            addView(mGridView);

            mPassLayout = this;

            Log.d(TAG,"BasePinPadView: Programmatic layout created successfully");

        } catch (Exception e) {
            Log.e(TAG,"BasePinPadView: Error in createLayoutProgrammatically: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initData() {
        if (listNumber == null) {
            listNumber = new ArrayList<>();
        }
        listNumber.clear();

        // Use standard layout for simplicity
        this.isRandom = false;

        // Standard layout: 1-9, 0, delete, clear, bypass, cancel, confirm
        for (int i = 1; i <= 9; i++) {
            listNumber.add(i);
        }
        listNumber.add(0); // Position 9 - 0
        listNumber.add(-1); // Position 10 - delete
        listNumber.add(-1); // Position 11 - clear
        listNumber.add(-1); // Position 12 - bypass
        listNumber.add(-1); // Position 13 - cancel
        listNumber.add(-1); // Position 14 - confirm

        Log.d(TAG,"BasePinPadView: initData - List size: " + listNumber.size());

        if (adapter == null) {
            adapter = createAdapter();
        }

        if (mGridView != null) {
            mGridView.setAdapter(adapter);
            Log.d(TAG,"BasePinPadView: GridView adapter set");
        } else {
            Log.e(TAG,"BasePinPadView: mGridView is null in initData");
        }
    }

    private BaseAdapter createAdapter() {
        return new BaseAdapter() {
            @Override
            public int getCount() {
                return listNumber != null ? listNumber.size() : 0;
            }

            @Override
            public Object getItem(int position) {
                return listNumber != null && position < listNumber.size() ? listNumber.get(position) : null;
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {
                ViewHolder holder;
                if (convertView == null) {
                    convertView = View.inflate(mContext, R.layout.view_paypass_gridview_item, null);
                    holder = new ViewHolder();
                    holder.btnNumber = convertView.findViewById(R.id.btNumber);
                    convertView.setTag(holder);
                } else {
                    holder = (ViewHolder) convertView.getTag();
                }

                if (listNumber == null || position >= listNumber.size()) {
                    return convertView;
                }

                // Reset button appearance
                holder.btnNumber.setBackgroundResource(android.R.color.transparent);
                holder.btnNumber.setTextColor(getResources().getColor(android.R.color.black));
                holder.btnNumber.setTextSize(20);
                holder.btnNumber.setOnTouchListener(null);

                // Setup button based on position
                setupButton(holder, position);

                holder.btnNumber.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG,"BasePinPadView: Button clicked at position: " + position);
                        handleButtonClick(position);
                    }
                });

                return convertView;
            }
        };
    }

    private void setupButton(ViewHolder holder, int position) {
        int grayColor = getResources().getColor(R.color.graye3);
        Integer item = listNumber.get(position);

        if (position < 10) {
            // Number buttons 1-9 and 0
            holder.btnNumber.setText(String.valueOf(item));
            holder.btnNumber.setBackgroundColor(grayColor);
            holder.btnNumber.setTextSize(24);
        } else {
            switch (position) {
                case 10: // Delete
                    holder.btnNumber.setText("⌫");
                    holder.btnNumber.setTextSize(18);
                    holder.btnNumber.setBackgroundColor(grayColor);
                    break;
                case 11: // Clear
                    holder.btnNumber.setText("Clear");
                    holder.btnNumber.setTextSize(15);
                    holder.btnNumber.setBackgroundColor(grayColor);
                    break;
                case 12: // Bypass
                    holder.btnNumber.setText("Bypass");
                    holder.btnNumber.setTextSize(15);
                    holder.btnNumber.setBackgroundColor(grayColor);
                    break;
                case 13: // Cancel
                    holder.btnNumber.setText("Cancel");
                    holder.btnNumber.setTextSize(15);
                    holder.btnNumber.setBackgroundColor(grayColor);
                    break;
                case 14: // Confirm
                    holder.btnNumber.setText("Confirm");
                    holder.btnNumber.setTextSize(15);
                    holder.btnNumber.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                    holder.btnNumber.setTextColor(getResources().getColor(android.R.color.white));
                    break;
            }
        }
    }

    private void handleButtonClick(int position) {
        Log.d(TAG,"BasePinPadView: handleButtonClick - position: " + position + ", current PIN: " + savePwd);

        if (position < 10) {
            handleNumberClick(position);
        } else {
            switch (position) {
                case 10: // Delete
                    handleDeleteClick();
                    break;
                case 11: // Clear
                    handleClearClick();
                    break;
                case 12: // Bypass
                    if (mPayClickListener != null) {
                        mPayClickListener.onPayPass();
                    }
                    break;
                case 13: // Cancel
                    if (mPayClickListener != null) {
                        mPayClickListener.onCancel();
                    }
                    break;
                case 14: // Confirm
                    handleConfirmClick();
                    break;
            }
        }
    }

    private void handleNumberClick(int position) {
        if (savePwd.length() >= 12) {
            Log.d(TAG,"BasePinPadView: PIN length limit reached");
            Toast.makeText(mContext, "Maximum PIN length is 12 digits", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the digit based on position
        String inputDigit;
        if (position < 9) {
            inputDigit = String.valueOf(position + 1); // 1-9
        } else if (position == 9) {
            inputDigit = "0"; // 0
        } else {
            return;
        }

        Log.d(TAG,"BasePinPadView: Adding digit: " + inputDigit);

        // Use direct input
        savePwd = savePwd + inputDigit;
        Log.d(TAG,"BasePinPadView: New PIN: " + savePwd + " (length: " + savePwd.length() + ")");

        updatePinDisplay();
    }

    private void handleDeleteClick() {
        if (savePwd.length() > 0) {
            savePwd = savePwd.substring(0, savePwd.length() - 1);
            Log.d(TAG,"BasePinPadView: After delete - PIN: " + savePwd);
            updatePinDisplay();
        }
    }

    private void handleClearClick() {
        if (savePwd.length() > 0) {
            savePwd = "";
            Log.d(TAG,"BasePinPadView: After clear - PIN cleared");
            updatePinDisplay();
        }
    }

    private void handleConfirmClick() {
        pinData = savePwd;
        Log.d(TAG,"BasePinPadView: handleConfirmClick - PIN data: '" + pinData + "' length: " + pinData.length());

        if (pinData.length() >= 4 && pinData.length() <= 12) {
            Log.d(TAG,"BasePinPadView: PIN validation passed");
            if (pinPadService != null) {
                pinPadService.processPinEntry(pinData);
            }
            if (mPayClickListener != null) {
                mPayClickListener.onConfirm(pinData);
            }
        } else {
            Log.e(TAG,"BasePinPadView: PIN validation failed - length: " + pinData.length());
            Toast.makeText(mContext, "PIN length must be 4-12 digits. Current: " + pinData.length(), Toast.LENGTH_LONG).show();
        }
    }

    private void updatePinDisplay() {
        if (mEtinputpin != null) {
            String displayText = "•".repeat(savePwd.length());
            mEtinputpin.setText(displayText);
            Log.d(TAG,"BasePinPadView: updatePinDisplay - Display: " + displayText + " (actual PIN: " + savePwd + ")");

            // Move cursor to end
            mEtinputpin.setSelection(displayText.length());
        } else {
            Log.e(TAG,"BasePinPadView: updatePinDisplay - mEtinputpin is null");
        }
    }

    static class ViewHolder {
        TextView btnNumber;
    }

    public BasePinPadView setRandomNumber(boolean isRandom) {
        this.isRandom = isRandom;
        initData();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        return this;
    }

    public void cleanup() {
        try {
            if (mEtinputpin != null) {
                mEtinputpin.setOnEditorActionListener(null);

                // Remove all text changed listeners
                mEtinputpin.addTextChangedListener(null);
            }

            if (mGridView != null) {
                mGridView.setAdapter(null);
            }

            adapter = null;

            if (listNumber != null) {
                listNumber.clear();
                listNumber = null;
            }

            mPayClickListener = null;
            pinPadService = null;

            // Remove all child views
            removeAllViews();

            mEtinputpin = null;
            mGridView = null;
            mPassLayout = null;
            mContext = null;

            clearSensitiveData();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void clearSensitiveData() {
        savePwd = "";
        pinData = "";
        if (mEtinputpin != null) {
            mEtinputpin.setText("");
        }
    }

    public boolean isInitialized() {
        return mGridView != null && mEtinputpin != null;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cleanup();
    }
}