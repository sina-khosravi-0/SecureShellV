package com.securelight.secureshellv.ui.login;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.securelight.secureshellv.R;
import com.securelight.secureshellv.databinding.ActivityLoginBinding;
import com.securelight.secureshellv.ui.homepage.HomepageActivity;
import com.securelight.secureshellv.utility.SharedPreferencesSingleton;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private LoginViewModel loginViewModel;
    private ActivityLoginBinding binding;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loginViewModel = new ViewModelProvider(this, new LoginViewModelFactory())
                .get(LoginViewModel.class);

        final EditText usernameEditText = binding.username;
        final EditText passwordEditText = binding.password;
        final Button loginButton = binding.login;
        final LinearLayout loadingProgressBar = binding.loading;

        RadioGroup appLanguageRadioGroup = findViewById(R.id.app_language_radio_group);
        SharedPreferencesSingleton preferences = SharedPreferencesSingleton.getInstance(this);
        switch (preferences.getAppLanguage()) {
            case "en":
                appLanguageRadioGroup.check(R.id.english_radio);
                break;
            case "fa":
                appLanguageRadioGroup.check(R.id.persian_radio);
                break;
        }

        appLanguageRadioGroup.setOnCheckedChangeListener(((group, checkedId) -> {
            RadioButton radioButton = group.findViewById(checkedId);
            switch (group.indexOfChild(radioButton)) {
                case 0:
                    preferences.setAppLanguage("en");
                    break;
                case 1:
                    preferences.setAppLanguage("fa");
                    break;
            }
        }));

        loginViewModel.getLoginFormState().observe(this, loginFormState -> {
            if (loginFormState == null) {
                return;
            }
            loginButton.setEnabled(loginFormState.isDataValid());
            if (loginFormState.getUsernameError() != null) {
                usernameEditText.setError(getString(loginFormState.getUsernameError()));
            }
            if (loginFormState.getPasswordError() != null) {
                passwordEditText.setError(getString(loginFormState.getPasswordError()));
            }
        });

        loginViewModel.getLoginResult().observe(this, new Observer<LoginResult>() {
            @Override
            public void onChanged(@Nullable LoginResult loginResult) {
                if (loginResult == null) {
                    return;
                }
                if (loginResult.getError() != null) {
                    showLoginFailed(loginResult.getError());
                }
                if (loginResult.getSuccess() != null) {
                    updateUiWithUser(loginResult.getSuccess());
                    setResult(Activity.RESULT_OK);
                    // set user as logged in and start homepage activity
                    SharedPreferencesSingleton.getInstance(getApplicationContext()).setLoggedIn(true);
                    startActivity(new Intent(getApplicationContext(), HomepageActivity.class));
                    finish();
                }
                loadingProgressBar.setVisibility(View.GONE);
            }
        });

        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            @Override
            public void afterTextChanged(Editable s) {
                loginViewModel.loginDataChanged(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString(), false);
            }
        };
        usernameEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                performLogin(usernameEditText, passwordEditText, loadingProgressBar);
            }
            return false;
        });
        loginButton.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(passwordEditText.getWindowToken(), 0);
            imm.hideSoftInputFromWindow(usernameEditText.getWindowToken(), 0);
            performLogin(usernameEditText, passwordEditText, loadingProgressBar);
        });
    }

    private void performLogin(EditText usernameEditText, EditText passwordEditText, LinearLayout loadingProgressBar) {
        if (loginViewModel.loginDataChanged(usernameEditText.getText().toString(),
                passwordEditText.getText().toString(), true)) {

            loadingProgressBar.setVisibility(View.VISIBLE);
            executorService.execute(() -> {
                loginViewModel.login(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString(), this.getApplicationContext());
            });

        }
    }

    /**
     * this method is called when login is successful
     * */
    private void updateUiWithUser(LoggedInUserView model) {
        String welcome = getString(R.string.welcome) + " " + model.getDisplayName();

        // TODO : initiate successful logged in experience
        Toast.makeText(getApplicationContext(), welcome, Toast.LENGTH_LONG).show();
    }

    private void showLoginFailed(@StringRes Integer errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }
}