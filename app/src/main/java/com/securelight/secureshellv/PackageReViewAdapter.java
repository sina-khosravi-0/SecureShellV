package com.securelight.secureshellv;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.securelight.secureshellv.databinding.FragmentItemBinding;
import com.securelight.secureshellv.placeholder.AppInfoItem;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link AppInfoItem}.
 * TODO: Replace the implementation with code for your data type.
 */
public class PackageReViewAdapter extends RecyclerView.Adapter<PackageReViewAdapter.ViewHolder> {
    private List<AppInfoItem> appInfoList;

    public PackageReViewAdapter(List<AppInfoItem> items) {
        appInfoList = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(FragmentItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.item = appInfoList.get(position);
        holder.itemCheckBox.setChecked(appInfoList.get(position).isChecked());
        holder.itemCheckBox.setText(appInfoList.get(position).getName());
        holder.itemImageView.setImageDrawable(appInfoList.get(position).getIcon());
    }

    public List<AppInfoItem> getAppInfoList() {
        return appInfoList;
    }

    public void updateValues(AppInfoItem item, int position) {
    }

    @Override
    public int getItemCount() {
        return appInfoList.size();
    }

    public void setAppInfoList(List<AppInfoItem> appInfoList) {
        this.appInfoList = appInfoList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final MaterialCheckBox itemCheckBox;
        public final ImageView itemImageView;
        public AppInfoItem item;

        public ViewHolder(FragmentItemBinding binding) {
            super(binding.getRoot());
            itemCheckBox = binding.packageListItemItemCheckbox;
            itemImageView = binding.packageListItemIcon;
            itemCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                item.setChecked(isChecked);
            });
        }

        @Override
        public String toString() {
            return "ViewHolder{" +
                    "itemName=" + itemCheckBox.isChecked() +
                    ", itemImageView=" + itemImageView +
                    ", item=" + item +
                    '}';
        }
    }
}