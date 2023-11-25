package com.securelight.secureshellv;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.securelight.secureshellv.backend.DataManager;
import com.securelight.secureshellv.backend.DatabaseHandlerSingleton;
import com.securelight.secureshellv.backend.ServicePlan;
import com.securelight.secureshellv.utility.Utilities;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RenewServiceActivity extends Activity {


    private static final String ROOT_URL = "http://seoforworld.com/api/v1/file-upload.php";
    private static final int REQUEST_PERMISSIONS = 100;
    private static final int PICK_IMAGE_REQUEST = 1;
    private Bitmap bitmap;
    private TextView amountText;
    private MaterialButton selectReceiptButton;
    private MaterialButton submitReceiptButton;
    private MaterialButton selectDurationButton;
    private MaterialButton selectPlanButton;
    private MaterialSwitch goldSwitch;
    private ImageView receiptImageView;
    private LinearLayout loadingView;
    private LinearLayout cardNumberArea;
    private List<ServicePlan> servicePlans;
    private int months = 1;
    private ServicePlan selectedServicePlan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_renew_service);

        amountText = findViewById(R.id.amount_text);
        ImageButton goldInfoButton = findViewById(R.id.gold_info_button);
        selectReceiptButton = findViewById(R.id.select_receipt_image_button);
        submitReceiptButton = findViewById(R.id.submit_receipt_image_button);
        selectDurationButton = findViewById(R.id.duration_dropdown_button);
        selectPlanButton = findViewById(R.id.users_dropdown_button);
        cardNumberArea = findViewById(R.id.card_number_area);
        receiptImageView = findViewById(R.id.receipt_image_view);
        goldSwitch = findViewById(R.id.gold_switch);
        loadingView = findViewById(R.id.loading);
        amountText.setClickable(true);

        goldInfoButton.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.gold_plans)
                .setMessage(R.string.gold_plan_alert_info)
                .setNeutralButton(R.string.ok, null).show());

        PopupMenu monthsPopup = new PopupMenu(RenewServiceActivity.this, selectDurationButton);
        PopupMenu plansPopup = new PopupMenu(RenewServiceActivity.this, selectPlanButton);
        fillShitUp(monthsPopup, plansPopup, goldSwitch.isChecked());

        monthsPopup.setOnMenuItemClickListener(item -> {
            selectDurationButton.setText(item.getTitle());
            months = item.getItemId();
            if (selectedServicePlan != null) {
                amountText.setText(NumberFormat.getInstance().format((long) selectedServicePlan.getPrice() * months));
            }
            return true;
        });
        plansPopup.setOnMenuItemClickListener(item -> {
            try {
                ServicePlan servicePlan = servicePlans.stream()
                        .filter(plan -> plan.getId() == item.getItemId()).collect(Collectors.toList()).get(0);
                if (goldSwitch.isChecked()) {
                    selectPlanButton.setText(String.format("%s - %s",
                            getResources().getQuantityString(R.plurals.users, servicePlan.getUsers(), servicePlan.getUsers()),
                            getResources().getQuantityString(R.plurals.gigs, servicePlan.getTraffic(), servicePlan.getTraffic())));
                } else {
                    selectPlanButton.setText(String.format("%s - %s",
                            getResources().getQuantityString(R.plurals.users, servicePlan.getUsers(), servicePlan.getUsers()),
                            getResources().getString(R.string.unlimited)));
                }
                selectedServicePlan = servicePlan;
                amountText.setText(NumberFormat.getInstance().format((long) servicePlan.getPrice() * months));
            } catch (IndexOutOfBoundsException ignored) {
            }
            return true;
        });


        goldSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.silver_plans)
                        .setMessage(R.string.silver_plan_alert)
                        .setNeutralButton(R.string.ok, null).show();
            }
            selectedServicePlan = null;
            selectPlanButton.setText(getString(R.string.please_select));
            amountText.setText("");
            monthsPopup.getMenu().clear();
            plansPopup.getMenu().clear();
            fillShitUp(monthsPopup, plansPopup, isChecked);
        });

        selectDurationButton.setOnClickListener(v -> {
            monthsPopup.getMenuInflater().inflate(R.menu.duration_menu, monthsPopup.getMenu());
            monthsPopup.show();
        });
        selectPlanButton.setOnClickListener(v -> {
            plansPopup.getMenuInflater().inflate(R.menu.duration_menu, plansPopup.getMenu());
            plansPopup.show();
        });

        selectReceiptButton.setOnClickListener(view -> {
            List<String> requiredPermissions = new ArrayList<>(Arrays.asList(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE));
            List<String> missingPermissions = new ArrayList<>();
            requiredPermissions.forEach(perm -> {
                if (ActivityCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                    missingPermissions.add(perm);
                }
            });
            if (!missingPermissions.isEmpty()) {
                ActivityCompat.requestPermissions(
                        this, missingPermissions/*requiredPermissions*/.toArray(new String[0]), 0);
            }

            showFileChooser();
        });

        submitReceiptButton.setOnClickListener(v -> {
            if (selectedServicePlan != null) {
                loadingView.setVisibility(View.VISIBLE);
                DatabaseHandlerSingleton.getInstance(null).sendRenewRequest(bitmap,
                        selectedServicePlan.getId(),
                        response -> {
                            Toast.makeText(this, R.string.submit_successful, Toast.LENGTH_SHORT).show();
                            loadingView.setVisibility(View.GONE);
                            finish();
                        },
                        error -> {
                            Log.e("DatabaseHandlerSingleton", "submit receipt error" + new String(error.networkResponse.data));
                            if (new String(error.networkResponse.data).contains("renew_pending")){
                                Toast.makeText(this, R.string.already_requested_renewal, Toast.LENGTH_SHORT).show();
                            }
                            loadingView.setVisibility(View.GONE);
                        });
            } else {
                Toast.makeText(this, R.string.please_select_a_plan_first, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fillShitUp(PopupMenu monthsPopup, PopupMenu plansPopup, boolean gold) {
        loadingView.setVisibility(View.VISIBLE);
        cardNumberArea.removeAllViews();
        new Thread(() -> {
            List<String> cardNumbers = DatabaseHandlerSingleton.getInstance(this).fetchCardNumbers();
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

            cardNumbers.forEach(cardNumber -> {
                TextView cardNumberTextView = new TextView(this);
                cardNumberTextView.setLayoutParams(layoutParams);
                cardNumberTextView.setText(String.format("%s", cardNumber));
                cardNumberTextView.setTypeface(Typeface.DEFAULT_BOLD);
                cardNumberTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
                cardNumberTextView.setBackgroundResource(R.drawable.rounded_container_background);
                cardNumberTextView.setClickable(true);
                int padding_in_px = Utilities.convertDPtoPX(getResources(), 10);
                cardNumberTextView.setPaddingRelative(padding_in_px, padding_in_px, padding_in_px, padding_in_px);

                TypedValue typedValue = new TypedValue();
                this.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true);
                cardNumberTextView.setForeground(AppCompatResources.getDrawable(this, typedValue.resourceId));
                runOnUiThread(() -> cardNumberArea.addView(cardNumberTextView));
            });

            servicePlans = DataManager.getInstance().getServicePlans(gold);
            List<Integer> durations = DatabaseHandlerSingleton.getInstance(this).fetchPlanDurations();

            durations.forEach(duration -> {
                if (duration / 12 == 0) {
                    monthsPopup.getMenu().add(1, duration, duration, getResources().getQuantityString(R.plurals.months, duration, duration));
                } else {
                    monthsPopup.getMenu().add(1, duration, duration, getResources().getQuantityString(R.plurals.years, duration / 12,
                            duration / 12));
                }

            });
            servicePlans.forEach(servicePlan -> {
                if (gold) {
                    plansPopup.getMenu().add(1, servicePlan.getId(), servicePlan.getId(),
                            String.format("%s - %s - %s",
                                    getResources().getQuantityString(R.plurals.users, servicePlan.getUsers(), servicePlan.getUsers()),
                                    getResources().getQuantityString(R.plurals.gigs, servicePlan.getTraffic(), servicePlan.getTraffic()),
                                    NumberFormat.getInstance().format(servicePlan.getPrice()) + " " + getResources().getString(R.string.toman)));
                } else {
                    plansPopup.getMenu().add(1, servicePlan.getId(), servicePlan.getId(),
                            String.format("%s - %s",
                                    getResources().getQuantityString(R.plurals.users, servicePlan.getUsers(), servicePlan.getUsers()),
                                    NumberFormat.getInstance().format(servicePlan.getPrice()) + " " + getResources().getString(R.string.toman)));
                }
            });
            runOnUiThread(() -> {
                selectDurationButton.setText(monthsPopup.getMenu().getItem(0).getTitle());
                months = 1;
                loadingView.setVisibility(View.GONE);
            });
        }).start();
    }

    private void showFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri picUri = data.getData();
            File file = new File(picUri.getPath());
            try {
                Log.d("filePath", file.getPath());
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), picUri);
                receiptImageView.setImageBitmap(bitmap);
                receiptImageView.setVisibility(View.VISIBLE);
                submitReceiptButton.setVisibility(View.VISIBLE);
            } catch (IOException e) {
                Log.d("RenewServiceActivity", e.getMessage(), e);
            }
        }
    }
}