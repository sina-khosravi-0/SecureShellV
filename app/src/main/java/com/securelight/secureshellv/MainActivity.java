package com.securelight.secureshellv;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.securelight.secureshellv.ui.homepage.HomepageActivity;
import com.securelight.secureshellv.ui.login.LoginActivity;
import com.securelight.secureshellv.utility.SharedPreferencesSingleton;

public class MainActivity extends AppCompatActivity {
    // launch activity based on user login status

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean isLoggedIn = SharedPreferencesSingleton.getInstance(getApplicationContext()).isLoggedIn();
        // check if user is logged in
        if (isLoggedIn) {
            startActivity(new Intent(getApplicationContext(), HomepageActivity.class));
        } else {
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
        }
        // finish this activity
        finish();
    }
}
