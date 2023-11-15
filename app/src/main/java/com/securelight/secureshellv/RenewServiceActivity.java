package com.securelight.secureshellv;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RenewServiceActivity extends Activity {


    private static final String ROOT_URL = "http://seoforworld.com/api/v1/file-upload.php";
    private static final int REQUEST_PERMISSIONS = 100;
    private static final int PICK_IMAGE_REQUEST = 1;
    private Bitmap bitmap;
    private TextView cardNumberText;
    private TextView amountText;
    private MaterialButton selectReceiptButton;
    private MaterialButton submitReceiptButton;
    private MaterialButton selectDurationButton;
    private MaterialButton selectUsersButton;
    private ImageView receiptImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_renew_service);

        cardNumberText = findViewById(R.id.card_number_text);
        amountText = findViewById(R.id.amount_text);
        selectReceiptButton = findViewById(R.id.select_receipt_image_button);
        submitReceiptButton = findViewById(R.id.submit_receipt_image_button);
        selectDurationButton = findViewById(R.id.duration_dropdown_button);
        selectUsersButton = findViewById(R.id.users_dropdown_button);
        cardNumberText.setClickable(true);
        amountText.setClickable(true);

        selectDurationButton.setOnClickListener(v -> {
            //Creating the instance of PopupMenu
            PopupMenu popup = new PopupMenu(RenewServiceActivity.this, selectDurationButton);
            popup.getMenu().add(1, 15, 1, getResources().getQuantityString(R.plurals.months, 1, 1));
            popup.getMenu().add(1, 22, 2, getResources().getQuantityString(R.plurals.months, 2, 2));
            popup.getMenu().add(1, 3, 3, getResources().getQuantityString(R.plurals.months, 3, 3));
            popup.getMenu().add(2, 43, 4, getResources().getQuantityString(R.plurals.months, 6, 6));
            popup.getMenu().add(2, 5, 5, getResources().getQuantityString(R.plurals.years, 1, 1));
            //Inflating the Popup using xml file
            popup.getMenuInflater()
                    .inflate(R.menu.duration_menu, popup.getMenu());

            //registering popup with OnMenuItemClickListener
            popup.setOnMenuItemClickListener(item -> {
                Toast.makeText(RenewServiceActivity.this, "You Clicked : " + item.getItemId(), Toast.LENGTH_SHORT).show();
                selectDurationButton.setText(item.getTitle());
                return true;
            });
            popup.show(); //showing popup menu
        });

        //initializing views
        receiptImageView = findViewById(R.id.receipt_image_view);
        //adding click listener to button
        findViewById(R.id.select_receipt_image_button).setOnClickListener(view -> {
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