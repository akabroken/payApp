package com.isw.payapp.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.StringRes;

import com.isw.payapp.R;

import java.util.Timer;
import java.util.TimerTask;

public class MyProgressDialog extends Dialog {

    private TextView tv_title;
    private TextView tv_text;
    private ImageView imageView;
    private Animation operatingAnim;
    private Context mContext;
    private long mTimeOut = 0;//
    private OnTimeOutListener mTimeOutListener = null;
    private Timer mTimer = null;//

    public interface OnTimeOutListener {
        void onTimeOut();
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (mTimeOutListener != null) {
                mTimeOutListener.onTimeOut();
                dismiss();
            }
        }
    };

    public MyProgressDialog(Context context) {
        this(context, R.style.TelpoProgressDialog,null,null);
    }

    public MyProgressDialog(Context context, CharSequence title, CharSequence text) {
        this(context, R.style.TelpoProgressDialog,null,null);
    }

    public MyProgressDialog(Context context, long time, OnTimeOutListener listener) {
        this(context, R.style.TelpoProgressDialog,null,null);
        mTimeOut = time;
        if (listener != null) {
            mTimeOutListener =  listener;
        }
    }

//    public TelpoProgressDialog(@NonNull Context context) {
//        super(context);
//    }

    private MyProgressDialog(Context context, int theme, CharSequence title, CharSequence text) {
        super(context, theme);
        this.setContentView(R.layout.telpo_progress_dialog);
        this.getWindow().getAttributes().gravity = Gravity.CENTER;
//        this.getWindow().getAttributes().rotationAnimation;
        this.setCancelable(false);
        mContext = context;



        tv_title = (TextView) this.findViewById(R.id.telpoProgress_title);
        tv_text = (TextView) this.findViewById(R.id.telpoProgress_text);
        imageView = (ImageView) this.findViewById(R.id.telpoProgress_image);

        if(title!=null){
            tv_title.setText(title);
        }

        if(text!=null){
            tv_text.setText(text);
        }


        operatingAnim = AnimationUtils.loadAnimation(context, R.anim.progress);
        LinearInterpolator lin = new LinearInterpolator();//匀速旋转
        operatingAnim.setInterpolator(lin);
    }
    @Override
    public void onStart() {
//        Log.d("kaiye","---TelpoProgressDialog--- onStart");
        super.onStart();

        imageView.startAnimation(operatingAnim);


        if (mTimeOut != 0) {
            mTimer = new Timer();
            TimerTask timerTast = new TimerTask() {
                @Override
                public void run() {
                    Message msg = mHandler.obtainMessage();
                    mHandler.sendMessage(msg);
                }
            };
            mTimer.schedule(timerTast, mTimeOut);
        }
    }

    @Override
    protected void onStop() {
//        Log.d("kaiye","---TelpoProgressDialog--- onStop");
        super.onStop();
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        if(title == null){
            title = "";
        }
        tv_title.setText(title);
    }
    @Override
    public void setTitle(@StringRes int titleId) {
        tv_title.setText(mContext.getText(titleId));
    }

    public void setMessage(CharSequence text) {
        if(text == null){
            text = "";
        }
        tv_text.setText(text);
    }

    public void setMessage(@StringRes int titleId) {
        tv_text.setText(mContext.getText(titleId));
    }

    public void setTimeOut(long t, OnTimeOutListener timeOutListener) {
        mTimeOut = t;
        if (timeOutListener != null) {
            this.mTimeOutListener =  timeOutListener;
        }
    }


}
