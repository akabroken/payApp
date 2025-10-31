package com.isw.payapp.Adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;


import com.isw.payapp.R;

import java.util.ArrayList;

public class CustomAdapter extends ArrayAdapter<ArrayList<Bitmap>> {

    private Context mContext;
    private int mResource;

    public CustomAdapter(@NonNull Context context, int resource, @NonNull ArrayList<ArrayList<Bitmap>> objects) {
        super(context, resource, objects);
        mContext = context;
        mResource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(mResource, parent, false);
        }

        ArrayList<Bitmap> images = getItem(position);
//        ConstraintLayout layout = convertView.findViewById(R.id.constraintLayout); // Assuming the root layout is ConstraintLayout
//
//        if (images != null) {
//            for (Bitmap bitmap : images) {
//                ImageView imageView = new ImageView(mContext);
//                imageView.setLayoutParams(new ConstraintLayout.LayoutParams(
//                        ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
//                        ConstraintLayout.LayoutParams.WRAP_CONTENT
//                ));
//                imageView.setImageBitmap(bitmap);
//                imageView.setPadding(4, 4, 4, 4);
//                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
//                imageView.setBackgroundResource(R.drawable.image_background_rounded); // Set rounded corners
//                layout.addView(imageView);
//            }
//        }

        return convertView;
    }

}
