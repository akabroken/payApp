package com.isw.payapp.helpers;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.isw.payapp.BuildConfig;
import com.isw.payapp.R;

public class LogoHeaderView extends FrameLayout {
    private ImageView imageLogo;

    public LogoHeaderView(Context context) {
        super(context);
        init();
    }

    public LogoHeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.logo_head, this, true);
        imageLogo = findViewById(R.id.imageLogo);

        // Auto-load the logo when view is created
        Glide.with(getContext())
                .load(BuildConfig.APP_LOGO)
                .into(imageLogo);
    }

    //Future use
    /*
    *       <!-- Instead of <include layout="@layout/logo_head" /> -->
            <com.isw.payapp.helpers.LogoHeaderView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content" />
    * */
}