package com.isw.payapp.devices.dspread.Activity.pinkeyboard;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
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

import com.dspread.xpos.QPOSService;
import com.dspread.xpos.Util;
import com.isw.payapp.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PinPadView extends RelativeLayout {
    private Activity mContext;
    private GridView mGridView;
    private String savePwd = "";
    private List<Integer> listNumber;//1,2,3---0
    private View mPassLayout;
    private boolean isRandom;
    private EditText mEtinputpin;
    private QPOSService pos;
    private String pinData = "";
    private BaseAdapter adapter;

    public static interface OnPayClickListener {
        void onCencel();
        void onPaypass();
        void onConfirm(String password);
    }

    private OnPayClickListener mPayClickListener;

    public void setPayClickListener(QPOSService qPOSService, OnPayClickListener listener) {
        pos = qPOSService;
        mPayClickListener = listener;
    }

    public PinPadView(Context context) {
        super(context);
        init((Activity) context);
    }

    public PinPadView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init((Activity) context);
    }

    public PinPadView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init((Activity) context);
    }

    private void init(Activity context) {
        this.mContext = context;
        initView();
        if (mPassLayout != null) {
            this.addView(mPassLayout);
        }
    }

    private void initView() {
        try {
            mPassLayout = LayoutInflater.from(mContext).inflate(R.layout.view_paypass_layout, null);
            mEtinputpin = mPassLayout.findViewById(R.id.et_inputpin);
            mGridView = mPassLayout.findViewById(R.id.gv_pass);

            if (mEtinputpin != null) {
                mEtinputpin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_UNSPECIFIED
                                || actionId == EditorInfo.IME_ACTION_DONE) {
                            pinData = mEtinputpin.getText().toString().trim();
                            if (mPayClickListener != null) {
                                mPayClickListener.onConfirm(pinData);
                            }
                            return true;
                        }
                        return false;
                    }
                });
            }
            initData();
        } catch (Exception e) {
            // Handle initialization error
        }
    }

    /**
     * Is isRandom enabled for random numbers
     */
    private void initData() {
        if (listNumber == null) {
            listNumber = new ArrayList<>();
        }
        listNumber.clear();

        if (isRandom) {
            for (int i = 0; i <= 10; i++) {
                listNumber.add(i);
            }
            // This method is to disrupt the order
            Collections.shuffle(listNumber);
            for (int i = 0; i <= 10; i++) {
                if (listNumber.get(i) == 10) {
                    listNumber.remove(i);
                    listNumber.add(9, 10);
                }
            }
            listNumber.add(R.mipmap.ic_pay_del0);
            listNumber.add(R.mipmap.ic_pay_del0);
            listNumber.add(R.mipmap.ic_pay_del0);
            listNumber.add(R.mipmap.ic_pay_del0);
        } else {
            for (int i = 1; i <= 9; i++) {
                listNumber.add(i);
            }
            listNumber.add(10);
            listNumber.add(0);
            listNumber.add(R.mipmap.ic_pay_del0);
        }

        if (adapter == null) {
            adapter = createAdapter();
        }
        if (mGridView != null) {
            mGridView.setAdapter(adapter);
        }
    }

    /**
     * Adapters for GridView
     */
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
                final ViewHolder holder;
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

                Integer item = listNumber.get(position);
                if (item != null) {
                    holder.btnNumber.setText(String.valueOf(item));
                }

                if (position == 10) {
                    holder.btnNumber.setBackgroundColor(mContext.getResources().getColor(R.color.graye3));
                }
                if (position == 9) {
                    holder.btnNumber.setText(R.string.delete);
                    holder.btnNumber.setTextSize(15);
                    holder.btnNumber.setBackgroundColor(mContext.getResources().getColor(R.color.graye3));
                }
                if (position == 11) {
                    holder.btnNumber.setText("Clear");
                    holder.btnNumber.setTextSize(15);
                    holder.btnNumber.setBackgroundResource(listNumber.get(position));
                }
                if (position == 12) {
                    holder.btnNumber.setText(R.string.bypass);
                    holder.btnNumber.setTextSize(15);
                    holder.btnNumber.setBackgroundColor(mContext.getResources().getColor(R.color.graye3));
                }
                if (position == 13) {
                    holder.btnNumber.setText(R.string.select_dialog_cancel);
                    holder.btnNumber.setTextSize(15);
                    holder.btnNumber.setBackgroundColor(mContext.getResources().getColor(R.color.graye3));
                }
                if (position == 14) {
                    holder.btnNumber.setText(R.string.select_dialog_confirm);
                    holder.btnNumber.setTextSize(15);
                    holder.btnNumber.setBackgroundColor(mContext.getResources().getColor(R.color.graye3));
                }

                if (position == 11) {
                    holder.btnNumber.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            switch (event.getAction()) {
                                case MotionEvent.ACTION_DOWN:
                                    holder.btnNumber.setBackgroundResource(R.mipmap.ic_pay_del1);
                                    break;
                                case MotionEvent.ACTION_MOVE:
                                    holder.btnNumber.setBackgroundResource(R.mipmap.ic_pay_del1);
                                    break;
                                case MotionEvent.ACTION_UP:
                                    holder.btnNumber.setBackgroundResource(R.mipmap.ic_pay_del0);
                                    break;
                            }
                            return false;
                        }
                    });
                }

                holder.btnNumber.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        handleButtonClick(position);
                    }
                });

                return convertView;
            }
        };
    }

    private void handleButtonClick(int position) {
        if (position < 11 && position != 9) {
            if (savePwd.length() == 12) {
                return;
            } else {
                String SavePwds = String.valueOf(listNumber.get(position));
                if (pos != null && pos.getCvmKeyList() != null && !("").equals(pos.getCvmKeyList())) {
                    String keyList = Util.convertHexToString(pos.getCvmKeyList());
                    for (int j = 0; j < keyList.length(); j++) {
                        if (keyList.charAt(j) == SavePwds.charAt(0)) {
                            savePwd = savePwd + Integer.toHexString(j);
                            break;
                        }
                    }
                }
                if (mEtinputpin != null) {
                    mEtinputpin.setText(savePwd);
                }
            }
        } else if (position == 11) {
            if (savePwd.length() > 0) {
                savePwd = savePwd.substring(0, savePwd.length() - 1);
                if (mEtinputpin != null) {
                    mEtinputpin.setText(savePwd);
                }
            }
        } else if (position == 9) {
            if (savePwd.length() > 0) {
                savePwd = "";
                if (mEtinputpin != null) {
                    mEtinputpin.setText("");
                }
            }
        } else if (position == 12) {// paypass
            if (mPayClickListener != null) {
                mPayClickListener.onPaypass();
            }
        } else if (position == 13) {// cancel
            if (mPayClickListener != null) {
                mPayClickListener.onCencel();
            }
        } else if (position == 14) {// confirm
            pinData = mEtinputpin != null ? mEtinputpin.getText().toString().trim() : "";
            if (pinData.length() >= 4 && pinData.length() <= 12) {
                if (mPayClickListener != null) {
                    mPayClickListener.onConfirm(pinData);
                }
            } else {
                Toast.makeText(mContext, "The length just can input 4 - 12 digits", Toast.LENGTH_SHORT).show();
            }
        }
    }

    static class ViewHolder {
        public TextView btnNumber;
    }

    /***
     * Set random number
     * @param israndom
     */
    public PinPadView setRandomNumber(boolean israndom) {
        isRandom = israndom;
        initData();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        return this;
    }

    /**
     * Cleanup method to prevent memory leaks
     */
    public void cleanup() {
        try {
            // Clear listeners
            if (mEtinputpin != null) {
                mEtinputpin.setOnEditorActionListener(null);
            }

            // Clear adapters and data
            if (mGridView != null) {
                mGridView.setAdapter(null);
            }

            if (adapter != null) {
                // If adapter has any cleanup needs, handle them here
                adapter = null;
            }

            // Clear collections
            if (listNumber != null) {
                listNumber.clear();
                listNumber = null;
            }

            // Clear references
            mPayClickListener = null;
            pos = null;
            mContext = null;

            // Remove views
            if (mPassLayout != null) {
                if (mPassLayout.getParent() == this) {
                    removeView(mPassLayout);
                }
                mPassLayout = null;
            }

            // Clear PIN data for security
            savePwd = "";
            pinData = "";

        } catch (Exception e) {
            // Log cleanup error if needed
        }
    }

    /**
     * Additional method to clear sensitive data
     */
    public void clearSensitiveData() {
        savePwd = "";
        pinData = "";
        if (mEtinputpin != null) {
            mEtinputpin.setText("");
        }
    }

    /**
     * Method to check if the view is properly initialized
     */
    public boolean isInitialized() {
        return mPassLayout != null && mGridView != null && mEtinputpin != null;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // Optional: automatically cleanup when view is detached
        // cleanup();
    }

    /**
     * Finalizer to ensure cleanup if not done explicitly
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            cleanup();
        } finally {
            super.finalize();
        }
    }
}