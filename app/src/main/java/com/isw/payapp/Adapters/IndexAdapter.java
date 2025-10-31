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
import com.isw.payapp.helpers.SessionManager;
import com.isw.payapp.model.GridMenuItem;

import java.util.ArrayList;
import java.util.List;

public class IndexAdapter extends RecyclerView.Adapter<IndexAdapter.ViewHolder> {

    private List<GridMenuItem> menuItems = new ArrayList<>();
    private final SessionManager sessionManager;
    private OnItemClickListener itemClickListener;

    public IndexAdapter(Context context) {
        this.sessionManager = new SessionManager(context);
    }

    public interface OnItemClickListener {
        void onItemClick(GridMenuItem menuItem);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    public void setMenuItems(List<GridMenuItem> menuItems) {
        this.menuItems = menuItems != null ? menuItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_images_vertical, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GridMenuItem menuItem = menuItems.get(position);
        holder.bind(menuItem);

        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(menuItem);
            }
        });
    }

    @Override
    public int getItemCount() {
        return menuItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final TextView titleView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            titleView = itemView.findViewById(R.id.titles);
        }

        public void bind(GridMenuItem menuItem) {
            imageView.setImageResource(menuItem.getIconResId());
            titleView.setText(menuItem.getTitle());
        }
    }
}