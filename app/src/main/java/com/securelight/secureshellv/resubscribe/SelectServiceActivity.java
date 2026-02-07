package com.securelight.secureshellv.resubscribe;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.gson.Gson;
import com.securelight.secureshellv.R;
import com.securelight.secureshellv.backend.DataManager;
import com.securelight.secureshellv.backend.ServicePlan;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SelectServiceActivity extends AppCompatActivity {
    private RecyclerView serviceRecycler;
    private View loadingView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_service);
        serviceRecycler = findViewById(R.id.service_items_recycler);
        RadioGroup typeRadioGroup = findViewById(R.id.typeRadioGroup);
        MaterialRadioButton goldRadio = findViewById(R.id.goldRadio);
        MaterialRadioButton normalRadio = findViewById(R.id.normalRadio);

        loadingView = findViewById(R.id.loading);
        serviceRecycler.setLayoutManager(new LinearLayoutManager(this));

        new Thread(() -> {
            List<ServicePlan> servicePlans = DataManager.getInstance().getServicePlans();
            ServicePackageRecyclerAdapter recyclerAdapter =
                    new ServicePackageRecyclerAdapter(servicePlans.stream()
                            // filter services based on selected type before passing it to the recycler
                            .filter(servicePlan -> normalRadio.isChecked() ? !servicePlan.isGold() : servicePlan.isGold())
                            .collect(Collectors.toList()),
                            normalRadio.isChecked(),
                            this);

            typeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {

                recyclerAdapter.changePackageType(servicePlans.stream()
                                // filter services based on selected type before passing it to the recycler
                                .filter(servicePlan -> normalRadio.isChecked() ? !servicePlan.isGold() : servicePlan.isGold())
                                .collect(Collectors.toList()),
                        normalRadio.isChecked());
                recyclerAdapter.notifyDataSetChanged();
            });

            recyclerAdapter.setOnItemClickListener(servicePlan -> {
                if (DataManager.getInstance().isRenewPending()) {
                    Toast.makeText(SelectServiceActivity.this, R.string.already_submitted, Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent checkoutIntent = new Intent(this, CheckoutActivity.class);
                checkoutIntent.putExtra("service_plan", servicePlan);
                startActivity(checkoutIntent);
            });

            runOnUiThread(() -> {
                serviceRecycler.setAdapter(recyclerAdapter);
                loadingView.setVisibility(View.GONE);
            });
        }).start();
    }

}
