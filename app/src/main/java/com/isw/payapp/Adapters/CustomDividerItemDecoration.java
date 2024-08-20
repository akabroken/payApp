package com.isw.payapp.Adapters;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.isw.payapp.R;

public class CustomDividerItemDecoration extends RecyclerView.ItemDecoration {
    private final Paint paint;

    public CustomDividerItemDecoration(Context context) {
        paint = new Paint();
        paint.setColor(context.getResources().getColor(R.color.dividerColor)); // Set your divider color here
        paint.setStrokeWidth(context.getResources().getDimensionPixelSize(R.dimen.fab_margin_custom)); // Set your divider height here
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        outRect.bottom = (int) paint.getStrokeWidth(); // Set the bottom offset to the divider height
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int childCount = parent.getChildCount();
        int width = parent.getWidth();

        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);
            int adapterPosition = parent.getChildAdapterPosition(child);

            if (adapterPosition != parent.getAdapter().getItemCount() - 1) {
                // Draw the bottom line for all items except the last one
                float top = child.getBottom();
                float bottom = top + paint.getStrokeWidth();
                c.drawRect(0, top, width, bottom, paint);
            }
        }
    }
}
