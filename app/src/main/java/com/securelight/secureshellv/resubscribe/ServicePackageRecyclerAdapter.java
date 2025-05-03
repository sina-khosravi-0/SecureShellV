package com.securelight.secureshellv.resubscribe;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.securelight.secureshellv.databinding.FragmentServicePackageItemBinding;

import java.util.List;

public class ServicePackageRecyclerAdapter extends RecyclerView.Adapter<ServicePackageRecyclerAdapter.ViewHolder> {
    private List<ServicePackageItem> items;
//    private OnItemClickListener onItemClickListener;

    public ServicePackageRecyclerAdapter(List<ServicePackageItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(FragmentServicePackageItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.title.setText(items.get(position).title);
        holder.supportText.setText(items.get(position).supportText);
        holder.price.setText(items.get(position).price);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

//    public void setOnItemClickListener(OnItemClickListener listener) {
//        this.onItemClickListener = listener;
//    }
    public class ViewHolder extends RecyclerView.ViewHolder {
        private ShapeableImageView imageView;
        private TextView title;
        private TextView supportText;
        private TextView price;

        public ViewHolder(FragmentServicePackageItemBinding binding) {
            super(binding.getRoot());
            title = binding.title;
            supportText = binding.supportText;
            price = binding.price;
            itemView.setOnClickListener(v -> {
                if (getAdapterPosition() != RecyclerView.NO_POSITION) {
//                    onItemClickListener.onClick(getAdapterPosition());
                }
            });
        }
    }
}