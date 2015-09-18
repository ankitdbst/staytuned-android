package com.android.app.tvbuff;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class NavigationAdapter extends BaseAdapter {
    Context mContext;
    List<String> mCategories;

    public NavigationAdapter(Context context, List<String> categories) {
        mContext = context;
        mCategories = categories;
    }

    @Override
    public int getCount() {
        return mCategories.size();
    }

    @Override
    public Object getItem(int position) {
        return mCategories.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private String getStringResourceByName(String aString) {
        String packageName = mContext.getPackageName();
        int resId = mContext.getResources().getIdentifier(aString, "string", packageName);
        return mContext.getResources().getString(resId);
    }

    private Drawable getDrawableResourceByName(String aString) {
        String packageName = mContext.getPackageName();
        int resId = mContext.getResources().getIdentifier(aString, "drawable", packageName);
        return mContext.getResources().getDrawable(resId);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext)
                    .inflate(R.layout.navigation_list_item, parent, false);
        }

        String category = (String)getItem(position);
        TextView categoryView = (TextView) convertView.findViewById(R.id.navigation_list_text_view);
        categoryView.setText(getStringResourceByName("title_" + category));
        Drawable img = getDrawableResourceByName("category_" + category);
        categoryView.setCompoundDrawablesWithIntrinsicBounds(img, null, null, null);
        categoryView.setCompoundDrawablePadding(100);
        return convertView;
    }
}
