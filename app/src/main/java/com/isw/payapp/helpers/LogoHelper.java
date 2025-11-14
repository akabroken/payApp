package com.isw.payapp.helpers;

import android.app.Activity;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.isw.payapp.BuildConfig;
import com.isw.payapp.R;

public class LogoHelper {

    public static void setupLogo(Activity activity, int layoutResourceId) {
        ImageView logoImage = activity.findViewById(R.id.imageLogo);
        if (logoImage != null) {
            Glide.with(activity)
                    .load(BuildConfig.APP_LOGO) // Using our constant
                    .into(logoImage);
        }
    }

    // Or if you have multiple activities with included logo
    public static void setupLogo(View parentView) {
        ImageView logoImage = parentView.findViewById(R.id.imageLogo);
        if (logoImage != null) {
            Glide.with(parentView.getContext())
                    .load(BuildConfig.APP_LOGO)
                    .into(logoImage);
        }
    }
}
