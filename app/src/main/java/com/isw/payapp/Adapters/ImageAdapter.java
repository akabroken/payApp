package com.isw.payapp.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.isw.payapp.R;
import com.isw.payapp.model.ItemData;

import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {

    private List<ItemData> itemDataList;
    private final Context context;
    private OnItemClickListener clickListener;

    public interface OnItemClickListener {
        void onItemClick(ItemData itemData, int position);
    }

    public ImageAdapter(@NonNull Context context, @NonNull List<ItemData> itemDataList) {
        this.context = context.getApplicationContext(); // Use application context to prevent memory leaks
        this.itemDataList = itemDataList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    public void updateData(@NonNull List<ItemData> newData) {
        this.itemDataList = newData;
        notifyDataSetChanged();
    }

    public void addItem(@NonNull ItemData itemData) {
        itemDataList.add(itemData);
        notifyItemInserted(itemDataList.size() - 1);
    }

    public void removeItem(int position) {
        if (position >= 0 && position < itemDataList.size()) {
            itemDataList.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void clear() {
        int size = itemDataList.size();
        itemDataList.clear();
        notifyItemRangeRemoved(0, size);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.grid_item_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ItemData currentItem = itemDataList.get(position);
        holder.bind(currentItem);

        holder.itemView.setOnClickListener(view -> {
            if (clickListener != null) {
                clickListener.onItemClick(currentItem, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        // Clean up resources if needed
    }

    @Override
    public int getItemCount() {
        return itemDataList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final TextView titleTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            titleTextView = itemView.findViewById(R.id.item_title);
        }

        public void bind(@NonNull ItemData itemData) {
            imageView.setImageResource(itemData.getImageResource());
            titleTextView.setText(itemData.getTitle());

            // Optional: Add content description for accessibility
            imageView.setContentDescription(itemData.getTitle());

            // Optional: Add placeholder or error image handling
            // You can use libraries like Glide or Picasso here
            // Glide.with(itemView.getContext())
            //      .load(itemData.getImageUrl())
            //      .placeholder(R.drawable.placeholder)
            //      .error(R.drawable.error)
            //      .into(imageView);
        }
    }

    // Optional: Add diffutil for better performance with large datasets
    /*
    private static final DiffUtil.ItemCallback<ItemData> DIFF_CALLBACK = new DiffUtil.ItemCallback<ItemData>() {
        @Override
        public boolean areItemsTheSame(@NonNull ItemData oldItem, @NonNull ItemData newItem) {
            return oldItem.getId() == newItem.getId(); // Assuming ItemData has an ID field
        }

        @Override
        public boolean areContentsTheSame(@NonNull ItemData oldItem, @NonNull ItemData newItem) {
            return oldItem.equals(newItem);
        }
    };
    */
}