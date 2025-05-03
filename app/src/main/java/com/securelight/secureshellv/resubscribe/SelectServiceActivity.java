package com.securelight.secureshellv.resubscribe;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.securelight.secureshellv.R;
import com.securelight.secureshellv.backend.DataManager;
import com.securelight.secureshellv.backend.DatabaseHandlerSingleton;
import com.securelight.secureshellv.backend.ServicePlan;
import com.securelight.secureshellv.utility.Utilities;

import java.text.NumberFormat;
import java.util.List;

public class SelectServiceActivity extends AppCompatActivity {
    private RecyclerView serviceRecycler;
    private MaterialButton serviceTypeButton;
    private View loadingView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        setContentView(R.layout.activity_select_service);
        serviceRecycler = findViewById(R.id.service_items_recycler);
        serviceTypeButton = findViewById(R.id.service_type_button);
        loadingView = findViewById(R.id.loading);
        PopupMenu plansPopup = new PopupMenu(SelectServiceActivity.this, serviceTypeButton);
        plansPopup.setOnMenuItemClickListener(item -> {
            serviceTypeButton.setText(item.getTitle());
            return true;
        });
        fillShitUp(false);
    }


    private void fillShitUp(boolean gold) {
        loadingView.setVisibility(View.VISIBLE);
        new Thread(() -> {
            List<ServicePlan> cardNumbers = DataManager.getInstance().getServicePlans(gold);

//            servicePlans.forEach(servicePlan -> {
//                if (gold) {
//                    plansPopup.getMenu().add(1, servicePlan.getId(), servicePlan.getId(),
//                            String.format("%s - %s - %s",
//                                    getResources().getQuantityString(R.plurals.users, servicePlan.getUsers(), servicePlan.getUsers()),
//                                    getResources().getQuantityString(R.plurals.gigs, servicePlan.getTraffic(), servicePlan.getTraffic()),
//                                    NumberFormat.getInstance().format(servicePlan.getPrice()) + " " + getResources().getString(R.string.toman)));
//                } else {
//                    plansPopup.getMenu().add(1, servicePlan.getId(), servicePlan.getId(),
//                            String.format("%s - %s",
//                                    getResources().getQuantityString(R.plurals.users, servicePlan.getUsers(), servicePlan.getUsers()),
//                                    NumberFormat.getInstance().format(servicePlan.getPrice()) + " " + getResources().getString(R.string.toman)));
//                }
//            });
            runOnUiThread(() -> {
                if (monthsPopup.getMenu().size() != 0) {
                    selectDurationButton.setText(monthsPopup.getMenu().getItem(0).getTitle());
                    months = 1;
                    loadingView.setVisibility(View.GONE);
                }

            });
        }).start();
    }
}
