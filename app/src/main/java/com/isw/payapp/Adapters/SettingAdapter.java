package com.isw.payapp.Adapters;


import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.isw.payapp.R;

import java.util.List;

public class SettingAdapter extends RecyclerView.Adapter<SettingAdapter.ViewHolder> {

    private List<Integer> imageList;
    private List<String> title;

    public void setImageList(List<Integer> imageList, List<String> title) {
        this.imageList = imageList;
        this.title = title;
        notifyDataSetChanged();
    }


    @NonNull
    @Override
    public SettingAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.layout_images_horizontal, parent, false);
        return new SettingAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SettingAdapter.ViewHolder holder, int position) {
        if (imageList != null && position < imageList.size() && position < title.size()) {
            int imageResource = imageList.get(position);
            Log.e("ERRRME","TEST RESOURCE:" + imageResource);
            holder.imageView.setImageResource(imageResource);
            holder.titles.setText(title.get(position));

            // Set click listener for the image
            holder.imageView.setOnClickListener(v -> {
                // Handle click event here
                switch (title.get(position)){
                    case "Key Download":
                        Navigation.findNavController(v).navigate(R.id.settings_to_keydownload);
                        break;
                    case "Terminal Setting":
                        Navigation.findNavController(v).navigate(R.id.settings_to_terminal);
                        break;
                    case "Network":
                        Navigation.findNavController(v).navigate(R.id.setting_to_network);
                        break;
                }

            });
        }
    }

    @Override
    public int getItemCount()
    {
        return imageList != null ? imageList.size() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        TextView titles;
        ImageView imageView;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.horizontalImageView);
            titles = itemView.findViewById(R.id.horizontalTextView);
        }
    }
}
