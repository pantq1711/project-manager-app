package com.example.projectmanager.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.projectmanager.R;

/**
 * Custom ArrayAdapter để fix màu chữ trong dropdown
 */
public class CategorySpinnerAdapter extends ArrayAdapter<String> {

    private Context context;
    private String[] categories;

    public CategorySpinnerAdapter(Context context, String[] categories) {
        super(context, android.R.layout.simple_spinner_item, categories);
        this.context = context;
        this.categories = categories;
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView view = (TextView) super.getView(position, convertView, parent);

        // Thiết lập cho item hiện tại được chọn (hiển thị trong field)
        view.setTextColor(ContextCompat.getColor(context, R.color.textColorPrimary));
        view.setTextSize(16);

        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        TextView view = (TextView) super.getDropDownView(position, convertView, parent);

        // Thiết lập cho items trong dropdown
        view.setTextColor(ContextCompat.getColor(context, android.R.color.black));
        view.setTextSize(16);
        view.setPadding(32, 24, 32, 24);
        view.setBackgroundColor(ContextCompat.getColor(context, android.R.color.white));

        // Thêm divider nhẹ giữa các items
        if (position > 0) {
            view.setCompoundDrawablesWithIntrinsicBounds(null,
                    ContextCompat.getDrawable(context, R.drawable.spinner_divider),
                    null, null);
        }

        return view;
    }
}