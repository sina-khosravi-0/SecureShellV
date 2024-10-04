package com.securelight.secureshellv.ui.homepage.bottomsheet.settings.appfilter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.securelight.secureshellv.databinding.FragmentAppFilterItemBinding;
import com.securelight.secureshellv.placeholder.AppInfoItem;
import com.securelight.secureshellv.utility.Utilities;

import java.util.List;
import java.util.stream.Collectors;

public class PackageReViewAdapter extends RecyclerView.Adapter<PackageReViewAdapter.ViewHolder> {
    private List<AppInfoItem> appInfoList;
    private List<AppInfoItem> originalList;

    public PackageReViewAdapter(List<AppInfoItem> items) {
        originalList = appInfoList = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(FragmentAppFilterItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.item = appInfoList.get(position);
        holder.itemCheckBox.setChecked(appInfoList.get(position).isChecked());
        holder.itemCheckBox.setText(appInfoList.get(position).getName());
        holder.itemImageView.setImageDrawable(appInfoList.get(position).getIcon());
    }

    public List<AppInfoItem> getAppInfoList() {
        return originalList;
    }

    @Override
    public int getItemCount() {
        return appInfoList.size();
    }

    public void applySearch(String text) {
        appInfoList = originalList.stream().filter(item -> Utilities.containsIgnoreCase(item.getName(), text))
                .collect(Collectors.toList());
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final MaterialCheckBox itemCheckBox;
        public final ImageView itemImageView;
        public AppInfoItem item;

        public ViewHolder(FragmentAppFilterItemBinding binding) {
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