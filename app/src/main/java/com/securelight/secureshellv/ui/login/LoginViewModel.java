package com.securelight.secureshellv.ui.login;

import android.content.SharedPreferences;
import android.util.Patterns;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.securelight.secureshellv.R;
import com.securelight.secureshellv.data.LoginRepository;
import com.securelight.secureshellv.data.Result;
import com.securelight.secureshellv.data.model.LoggedInUser;

public class LoginViewModel extends ViewModel {

    private MutableLiveData<LoginFormState> loginFormState = new MutableLiveData<>();
    private MutableLiveData<LoginResult> loginResult = new MutableLiveData<>();
    private LoginRepository loginRepository;

    LoginViewModel(LoginRepository loginRepository) {
        this.loginRepository = loginRepository;
    }

    LiveData<LoginFormState> getLoginFormState() {
        return loginFormState;
    }

    LiveData<LoginResult> getLoginResult() {
        return loginResult;
    }

    public void login(String username, String password, SharedPreferences apiPreferences) {
        // launched in a separate asynchronous job
        Result<LoggedInUser> result = loginRepository.login(username, password);

        if (result instanceof Result.Success) {
            LoggedInUser data = ((Result.Success<LoggedInUser>) result).getData();
            loginResult.postValue(new LoginResult(new LoggedInUserView(data.getDisplayName())));

        } else {

            loginResult.postValue(new LoginResult(R.string.login_failed));
        }
    }

    public boolean loginDataChanged(String username, String password, boolean isSubmitRequest) {
        if (!isSubmitRequest) {
            loginFormState.postValue(new LoginFormState(true));
            return false;

        }
        if ((isUserNameValid(username) && isPasswordValid(password))) {
            loginFormState.postValue(new LoginFormState(true));
            return true;
        }
        if (!isUserNameValid(username)) {
            loginFormState.postValue(new LoginFormState(R.string.invalid_username, null));
        }
        if (!isPasswordValid(password)) {
            loginFormState.postValue(new LoginFormState(null, R.string.invalid_password));
        }
        return false;
    }

    // A placeholder username validation check
    private boolean isUserNameValid(String username) {
        if (username == null) {
            return false;
        }
        if (username.contains("@")) {
            return Patterns.EMAIL_ADDRESS.matcher(username).matches();
        } else {
            return !username.trim().isEmpty();
        }
    }

    // A placeholder password validation check
    private boolean isPasswordValid(String password) {
        return password != null && password.trim().length() >= 4;
    }
}