package com.nkrhelper.carousel;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nkrhelper.mylibrary.carousel.CarouselView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created 09/05/2017.
 */

public class ColorItemAdapter extends CarouselView.Adapter<ColorItemAdapter.ColorViewHolder> {
    private List<ColorItem> mColorItems;

    public ColorItemAdapter(String[] colorNames, String[] colorValues) {
        int nameCount = colorNames.length;
        int valueCount = colorValues.length;

        int itemCount = nameCount > valueCount ? nameCount : valueCount;

        mColorItems = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            mColorItems.add(new ColorItem(colorNames[i], colorValues[i]));
        }
    }

    @Override
    public ColorViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View colorItemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_color_item, parent, false);
        return new ColorViewHolder(colorItemView);
    }

    @Override
    public int getActualItemCount() {
        return mColorItems.size();
    }

    @Override
    public void onBindCarouselViewHolder(ColorViewHolder holder, int index) {
        holder.setColor(mColorItems.get(index));
    }


//    @Override
//    public void onViewRecycled(ColorViewHolder holder) {
//        super.onViewRecycled(holder);
//        Log.d("TAG", "on recycled: " + holder.getAdapterPosition());
//    }

    static class ColorViewHolder extends RecyclerView.ViewHolder {
        private TextView mColorNameTextView;
        private TextView mColorValueTextView;
        private ViewGroup mColorItemPlaceHolder;

        ColorViewHolder(View itemView) {
            super(itemView);

            mColorNameTextView = (TextView) itemView.findViewById(R.id.text_color_name);
            mColorValueTextView = (TextView) itemView.findViewById(R.id.text_color_value);
            mColorItemPlaceHolder = (ViewGroup) itemView.findViewById(R.id.placeholder_color_item);
        }

        void setColor(ColorItem colorItem) {
            mColorNameTextView.setText(colorItem.getColorName());

            String colorValue = colorItem.getColorValue();
            mColorValueTextView.setText(colorValue);
            mColorItemPlaceHolder.setBackgroundColor(Color.parseColor(colorValue));
        }
    }
}