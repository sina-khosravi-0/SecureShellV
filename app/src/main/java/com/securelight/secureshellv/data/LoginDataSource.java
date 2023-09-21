package com.securelight.secureshellv.data;

import com.securelight.secureshellv.backend.DatabaseHandlerSingleton;
import com.securelight.secureshellv.data.model.LoggedInUser;

import java.io.IOException;

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
public class LoginDataSource {


    public Result<LoggedInUser> login(String username, String password) {

        DatabaseHandlerSingleton.FetchDbResult result = DatabaseHandlerSingleton
                .fetchTokens(username, password);
        // TODO: handle loggedInUser authentication
        if (result == DatabaseHandlerSingleton.FetchDbResult.SUCCESS) {
            LoggedInUser user = new LoggedInUser(
                    java.util.UUID.randomUUID().toString(),
                    username);
            return new Result.Success<LoggedInUser>(user);
        }
        return new Result.Error(new IOException(result.toString()));
    }

    public void logout() {
        // TODO: revoke authentication
    }
}