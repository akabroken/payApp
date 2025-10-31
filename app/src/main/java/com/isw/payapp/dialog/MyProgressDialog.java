package com.isw.payapp.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Gravity;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.isw.payapp.R;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

public class MyProgressDialog extends Dialog {

    private TextView tv_title;
    private TextView tv_text;
    private ImageView imageView;
    private Animation operatingAnim;
    private Context mContext;
    private long mTimeOut = 0;
    private OnTimeOutListener mTimeOutListener = null;
    private Timer mTimer = null;

    // Use WeakReference to prevent memory leaks
    private static class SafeHandler extends Handler {
        private final WeakReference<MyProgressDialog> mDialogRef;

        SafeHandler(MyProgressDialog dialog) {
            super(Looper.getMainLooper());
            mDialogRef = new WeakReference<>(dialog);
        }

        @Override
        public void handleMessage(Message msg) {
            MyProgressDialog dialog = mDialogRef.get();
            if (dialog != null && dialog.mTimeOutListener != null) {
                dialog.mTimeOutListener.onTimeOut();
                // Only dismiss if still showing and not destroyed
                if (dialog.isShowing()) {
                    dialog.safeDismiss();
                }
            }
        }
    }

    private final Handler mHandler = new SafeHandler(this);

    public interface OnTimeOutListener {
        void onTimeOut();
    }

    public MyProgressDialog(Context context) {
        this(context, R.style.TelpoProgressDialog, null, null);
    }

    public MyProgressDialog(Context context, CharSequence title, CharSequence text) {
        this(context, R.style.TelpoProgressDialog, title, text);
    }

    public MyProgressDialog(Context context, long time, OnTimeOutListener listener) {
        this(context, R.style.TelpoProgressDialog, null, null);
        mTimeOut = time;
        mTimeOutListener = listener;
    }

    private MyProgressDialog(Context context, int theme, @Nullable CharSequence title, @Nullable CharSequence text) {
        super(context, theme);
        initializeDialog(context, title, text);
    }

    private void initializeDialog(Context context, @Nullable CharSequence title, @Nullable CharSequence text) {
        try {
            setContentView(R.layout.telpo_progress_dialog);

            if (getWindow() != null) {
                getWindow().getAttributes().gravity = Gravity.CENTER;
            }

            setCancelable(false);
            mContext = context;

            initializeViews();
            setContent(title, text);
            setupAnimation(context);

        } catch (Exception e) {
            // Log error or handle initialization failure
            e.printStackTrace();
        }
    }

    private void initializeViews() {
        tv_title = findViewById(R.id.telpoProgress_title);
        tv_text = findViewById(R.id.telpoProgress_text);
        imageView = findViewById(R.id.telpoProgress_image);

        // Validate required views
        if (tv_title == null || tv_text == null || imageView == null) {
            throw new IllegalStateException("Required views not found in layout");
        }
    }

    private void setContent(@Nullable CharSequence title, @Nullable CharSequence text) {
        if (title != null) {
            tv_title.setText(title);
        } else {
            tv_title.setText("");
        }

        if (text != null) {
            tv_text.setText(text);
        } else {
            tv_text.setText("");
        }
    }

    private void setupAnimation(Context context) {
        operatingAnim = AnimationUtils.loadAnimation(context, R.anim.progress);
        LinearInterpolator lin = new LinearInterpolator();
        operatingAnim.setInterpolator(lin);
    }

    @Override
    public void onStart() {
        super.onStart();
        startAnimation();
        startTimeoutTimer();
    }

    @Override
    protected void onStop() {
        super.onStop();
        cleanupResources();
    }

    private void startAnimation() {
        if (imageView != null && operatingAnim != null) {
            try {
                imageView.startAnimation(operatingAnim);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void startTimeoutTimer() {
        if (mTimeOut > 0 && mTimeOutListener != null) {
            cancelExistingTimer();

            mTimer = new Timer();
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    if (mHandler != null) {
                        Message msg = mHandler.obtainMessage();
                        mHandler.sendMessage(msg);
                    }
                }
            };
            mTimer.schedule(timerTask, mTimeOut);
        }
    }

    private void cancelExistingTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
            mTimer = null;
        }
    }

    private void cleanupResources() {
        cancelExistingTimer();

        if (imageView != null && operatingAnim != null) {
            try {
                imageView.clearAnimation();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        if (tv_title != null) {
            tv_title.setText(title != null ? title : "");
        }
    }

    @Override
    public void setTitle(@StringRes int titleId) {
        if (tv_title != null && mContext != null) {
            try {
                tv_title.setText(mContext.getText(titleId));
            } catch (Exception e) {
                tv_title.setText("");
            }
        }
    }

    public void setMessage(CharSequence text) {
        if (tv_text != null) {
            tv_text.setText(text != null ? text : "");
        }
    }

    public void setMessage(@StringRes int titleId) {
        if (tv_text != null && mContext != null) {
            try {
                tv_text.setText(mContext.getText(titleId));
            } catch (Exception e) {
                tv_text.setText("");
            }
        }
    }

    public void setTimeOut(long timeout, @Nullable OnTimeOutListener timeOutListener) {
        mTimeOut = timeout;
        mTimeOutListener = timeOutListener;

        // Restart timer if dialog is currently showing
        if (isShowing()) {
            cancelExistingTimer();
            startTimeoutTimer();
        }
    }

    /**
     * Safely dismiss the dialog handling various edge cases
     */
    public void safeDismiss() {
        try {
            if (isShowing()) {
                dismiss();
            }
        } catch (IllegalArgumentException e) {
            // Dialog was already dismissed or window token invalid
            e.printStackTrace();
        } catch (Exception e) {
            // Any other exception during dismissal
            e.printStackTrace();
        }
    }

    /**
     * Update both title and message at once
     */
    public void updateContent(@Nullable CharSequence title, @Nullable CharSequence message) {
        if (tv_title != null) {
            tv_title.setText(title != null ? title : "");
        }
        if (tv_text != null) {
            tv_text.setText(message != null ? message : "");
        }
    }

    /**
     * Check if timeout is set
     */
    public boolean hasTimeout() {
        return mTimeOut > 0 && mTimeOutListener != null;
    }

    @Override
    public void dismiss() {
        cleanupResources();
        super.dismiss();
    }
}