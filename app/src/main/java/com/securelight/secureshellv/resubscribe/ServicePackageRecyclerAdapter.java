package com.securelight.secureshellv.resubscribe;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.securelight.secureshellv.R;
import com.securelight.secureshellv.backend.ServicePlan;
import com.securelight.secureshellv.databinding.FragmentServicePackageItemBinding;

import java.util.ArrayList;
import java.util.List;

public class ServicePackageRecyclerAdapter extends RecyclerView.Adapter<ServicePackageRecyclerAdapter.ViewHolder> {
    private List<ServicePlan> items;
    private boolean unlimited;
    private OnServiceClickListener onItemClickListener;
    private Context context;

    public ServicePackageRecyclerAdapter(List<ServicePlan> items,
                                         boolean unlimited,
                                         Context context) {

        this.items = new ArrayList<>(items);
        this.unlimited = unlimited;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(FragmentServicePackageItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.title.setText(context.getResources().getQuantityString(
                R.plurals.months, items.get(position).getMonths(), items.get(position).getMonths()));
        holder.price.setText(String.valueOf(items.get(position).getPrice()));
        if (unlimited) {
            holder.supportText.setText(context.getResources().getQuantityString(R.plurals.users,
                    items.get(position).getUsers(), items.get(position).getUsers()));
        } else {
            holder.supportText.setText(context.getResources().getQuantityString(R.plurals.gigs,
                    items.get(position).getTraffic(), items.get(position).getTraffic()));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void changePackageType(List<ServicePlan> items, boolean unlimited) {
        this.items = new ArrayList<>(items);
        this.unlimited = unlimited;
    }

    public void setOnItemClickListener(OnServiceClickListener listener) {
        this.onItemClickListener = listener;
    }

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
                onItemClickListener.onServiceClicked(items.get(getBindingAdapterPosition()));
            });
        }
    }
}