package com.securelight.secureshellv.ui.bottomsheet.serversettings;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.securelight.secureshellv.MainActivityTest;
import com.securelight.secureshellv.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ServerLocationArrayAdapter extends ArrayAdapter<String> {
    private List<String> codes;
    private Context context;
    private int resourceId;
    /**
     * Filter List for handling data changing, range changes and managing data globally
     */
    private ListFilter listFilter = new ListFilter();
    /**
     * Another array used to perform some in-module operations
     */
    private List<String> dataListAllItems;

    /**
     * @param context  Context of your Activity
     * @param resource Single Item Layout Id
     * @param codes    Data List
     */
    public ServerLocationArrayAdapter(@NonNull Context context, int resource, @NonNull List<String> codes) {
        super(context, resource, codes);
        this.codes = codes;
        this.context = context;
        this.resourceId = resource;
    }

    @Override
    public int getViewTypeCount() {
        // TODO: sometimes the list is empty. find why
        if (getCount() <= 1) {
            return 1;
        } else {
            return getCount();
        }
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public int getCount() {
        return codes.size();
    }

    @Nullable
    @Override
    public String getItem(int position) {
        if (position == 0) {
            return context.getString(R.string.auto);
        }
        Locale locale = new Locale("", codes.get(position));
        return locale.getDisplayCountry(locale);
    }

    public String getCode(int position) {
        return codes.get(position);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(resourceId, parent, false);
//            convertView.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
            TextView itemTextView = convertView.findViewById(R.id.server_location_item_text);
            Drawable icon;
            if (codes.get(position).equals("ato")) {
                itemTextView.setPadding(0, 10, 0, 10);
                icon = AppCompatResources.getDrawable(context, R.drawable.auto_server_select);
                icon.setTint(MainActivityTest.colorPrimary);
            } else {
                int drawableId = context.getResources()
                        .getIdentifier("flag_" + codes.get(position), "drawable", context.getPackageName());
                if (drawableId == 0) {
                    icon = AppCompatResources.getDrawable(context, R.drawable.flag_empty);
                } else {
                    icon = AppCompatResources.getDrawable(context, drawableId);
                }
            }
            itemTextView.setText(getItem(position));
            itemTextView.setCompoundDrawablesRelativeWithIntrinsicBounds
                    (null, null, icon, null);
        }
        return convertView;
    }

    public void setAutoCompleteItem(MaterialAutoCompleteTextView autoComplete, String code) {
        if (codes.contains(code)) {
            autoComplete.setText(getItem(codes.indexOf(code)), false);
            Drawable icon;
            if (code.equals("ato")) {
                icon = AppCompatResources.getDrawable(context, R.drawable.auto_server_select);
                icon.setTint(MainActivityTest.colorPrimary);
            } else {
                int drawableId = context.getResources()
                        .getIdentifier("flag_" + code, "drawable", context.getPackageName());
                if (drawableId == 0) {
                    icon = AppCompatResources.getDrawable(context, R.drawable.flag_empty);
                } else {
                    icon = AppCompatResources.getDrawable(context, drawableId);
                }
            }
            autoComplete.setCompoundDrawablesRelativeWithIntrinsicBounds
                    (icon, null, null, null);
        }
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return listFilter;
    }

    public class ListFilter extends Filter {
        private final Object lock = new Object();

        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            FilterResults results = new FilterResults();
            if (dataListAllItems == null) {
                synchronized (lock) {
                    dataListAllItems = new ArrayList<>(codes);
                }
            }
            if (prefix == null || prefix.length() == 0) {
                synchronized (lock) {
                    results.values = dataListAllItems;
                    results.count = dataListAllItems.size();
                }
            } else {
                final String searchStrLowerCase = prefix.toString().toLowerCase();
                ArrayList<String> matchValues = new ArrayList<String>();
                for (String dataItem : dataListAllItems) {
                    if (dataItem.toLowerCase().startsWith(searchStrLowerCase)) {
                        matchValues.add(dataItem);
                    }
                }
                results.values = matchValues;
                results.count = matchValues.size();
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            if (results.values != null) {
                codes = (ArrayList<String>) results.values;
            } else {
                codes = null;
            }
            if (results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }

    }

}
