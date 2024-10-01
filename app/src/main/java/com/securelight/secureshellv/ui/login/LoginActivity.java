package com.securelight.secureshellv.ui.login;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.securelight.secureshellv.statics.Constants;
import com.securelight.secureshellv.R;
import com.securelight.secureshellv.databinding.ActivityLoginBinding;

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
        final LinearLayout loadingProgressBar = (LinearLayout) binding.loading;

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
                    finish();
                }
                loadingProgressBar.setVisibility(View.GONE);

                //Complete and destroy login activity once successful

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
            performLogin(usernameEditText, passwordEditText, loadingProgressBar);
        });
    }

    private void performLogin(EditText usernameEditText, EditText passwordEditText, LinearLayout loadingProgressBar) {
        if (loginViewModel.loginDataChanged(usernameEditText.getText().toString(),
                passwordEditText.getText().toString(), true)) {

            SharedPreferences preferences = getApplicationContext()
                    .getSharedPreferences(Constants.API_CACHE_PREFERENCE_GROUP, Activity.MODE_PRIVATE);
            loadingProgressBar.setVisibility(View.VISIBLE);
            executorService.execute(() -> {
                loginViewModel.login(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString(), this.getApplicationContext());
            });

        }
    }


    private void updateUiWithUser(LoggedInUserView model) {
        String welcome = getString(R.string.welcome) + model.getDisplayName();
        // TODO : initiate successful logged in experience
        Toast.makeText(getApplicationContext(), welcome, Toast.LENGTH_LONG).show();
    }

    private void showLoginFailed(@StringRes Integer errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }
}