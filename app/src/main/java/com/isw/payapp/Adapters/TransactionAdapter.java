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

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private List<Integer> imageList;
    private List<String> title;


    public void setImageList(List<Integer> imageList, List<String> title) {
        this.imageList = imageList;
        this.title = title;
        notifyDataSetChanged();
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.layout_images_horizontal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        if (imageList != null && position < imageList.size() && position < title.size()) {
            int imageResource = imageList.get(position);
            Log.i("TRAN", "TEST "+imageResource);
            holder.imageView.setImageResource(imageResource);
            holder.titles.setText(title.get(position));

            // Set click listener for the image
            holder.imageView.setOnClickListener(v -> {
                // Handle click event here
                switch (title.get(position)){
                    case "Purchase":
                        Navigation.findNavController(v).navigate(R.id.transaction_to_purchase);
                        break;
                    case "Reversal":
                        Navigation.findNavController(v).navigate(R.id.transaction_to_reversal);
                        break;
                    case "Refund":
                        Navigation.findNavController(v).navigate(R.id.transaction_to_purchase);
                        break;
                }

            });
        }
    }

    @Override
    public int getItemCount() {
        return imageList != null ? imageList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titles;
        ImageView imageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.horizontalImageView);
            titles = itemView.findViewById(R.id.horizontalTextView);
        }
    }
}
