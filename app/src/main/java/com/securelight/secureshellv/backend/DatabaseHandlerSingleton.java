package com.securelight.secureshellv.backend;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.collection.LruCache;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.securelight.secureshellv.MainActivity;
import com.securelight.secureshellv.utility.SharedPreferencesSingleton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class DatabaseHandlerSingleton {
    enum TokenResult {
        TOKEN_VALID, TOKEN_INVALID, NO_AUTH, NO_CONNECTION, TIMEOUT, UNAUTHORIZED;
    }

    public enum FetchDbResult {
        SUCCESS, AUTH_FAIL, NO_CONNECTION, TIMEOUT,
    }

    private static DatabaseHandlerSingleton instance;
    private RequestQueue requestQueue;
    private final ImageLoader imageLoader;
    private static Context ctx;
    private static final String TOKEN_INVALID_CODE_STRING = "token_not_valid";
    private static final String CREDIT_EXPIRED_CODE_STRING = "credit_expired";
    private static final String OUT_OF_TRAFFIC_CODE_STRING = "insufficient_traffic";
//    private String endPoint = "http://192.168.128.71:8000/";
    private String endPoint = "https://api.weary.tech/";

    private DatabaseHandlerSingleton(@NonNull Context context) {
        // getApplicationContext() is key, it keeps you from leaking the
        // Activity or BroadcastReceiver if someone passes one in.
        ctx = context.getApplicationContext();
        requestQueue = getRequestQueue();

        imageLoader = new ImageLoader(requestQueue, new ImageLoader.ImageCache() {
            private final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(20);

            @Override
            public Bitmap getBitmap(String url) {
                return cache.get(url);
            }

            @Override
            public void putBitmap(String url, Bitmap bitmap) {
                cache.put(url, bitmap);
            }
        });
    }

    public static synchronized DatabaseHandlerSingleton getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHandlerSingleton(context);
        }
        return instance;
    }

    public RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(ctx);
        }
        return requestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

    public ImageLoader getImageLoader() {
        return imageLoader;
    }

    public void signIn(String username, String password, Response.Listener<JSONObject> responseListener,
                       Response.ErrorListener errorListener) {
        String url = this.endPoint + "api/token/";
        JSONObject object = new JSONObject();
        try {
            object.put("username", username);
            object.put("password", password);
        } catch (JSONException ignored) {
        }

        AtomicReference<FetchDbResult> result = new AtomicReference<>();
        JsonObjectRequest jsonObjectRequest =
                new JsonObjectRequest(Request.Method.POST,
                        url,
                        object,
                        responseListener,
                        errorListener);

        instance.addToRequestQueue(jsonObjectRequest);
        result.get();
    }

    public void fetchUserData() {
        SharedPreferencesSingleton preferences = SharedPreferencesSingleton.getInstance(ctx);
        String accessToken = preferences.getAccessToken();
        String refreshToken = preferences.getRefreshToken();
        String url = endPoint + "api/account/user/";

        Response.Listener<JSONObject> accessVerifyResponseListener = verifyResponse -> {

            makeUserDataRequest(url, accessToken);
        };


        Response.Listener<JSONObject> refreshTokenResponseListener = refreshResponse -> {
            try {
                if (refreshResponse.has("access")) {
                    preferences.saveAccessToken(refreshResponse.getString("access"));
                }
                if (refreshResponse.has("refresh")) {
                    preferences.saveRefreshToken(refreshResponse.getString("refresh"));
                }
            } catch (JSONException ignored) {
            }
            String accessTokenFinal = preferences.getAccessToken();
            makeUserDataRequest(url, accessTokenFinal);
        };

        Response.Listener<JSONObject> refreshVerifyResponseListener = verifyResponse -> {
            try {
                // success only if code == 200
                if (verifyResponse.getString("code").equals("200")) {
                    // refresh tokens if refresh token is valid
                    doRefreshToken(refreshTokenResponseListener, null);
                }
            } catch (JSONException ignored) {
            }
        };

        Response.ErrorListener refreshErrorListener = error -> {
            if (error instanceof AuthFailureError) {
                // Both tokens are invalid. Send broadcast to sign in again
                sendSignInBroadcast();
            } else if (error instanceof TimeoutError) {

            }
        };

        Response.ErrorListener accessErrorListener = error -> {
            if (error instanceof AuthFailureError) {
                // verify refresh token if access token is invalid
                verifyToken(refreshToken, refreshVerifyResponseListener, refreshErrorListener);
            } else if (error instanceof TimeoutError) {

            }
        };

        // verify access token
        verifyToken(accessToken, accessVerifyResponseListener, accessErrorListener);
        // todo remove return
    }

    private void makeUserDataRequest(String url, String accessTokenFinal) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
            UserData userData = UserData.getInstance();
            try {
                JSONObject userCreditInfo = response.getJSONObject("user_credit_info");
                userData.parseData(userCreditInfo.getDouble("remaining_gb"),
                        userCreditInfo.getString("end_credit_date"),
                        userCreditInfo.getLong("total_traffic_b"),
                        userCreditInfo.getLong("used_traffic_b"),
                        userCreditInfo.getBoolean("unlimited_credit_time"),
                        userCreditInfo.getBoolean("unlimited_traffic"),
                        userCreditInfo.getBoolean("has_paid"),
                        userCreditInfo.getInt("connected_ips"),
                        userCreditInfo.getInt("allowed_ips"),
                        userCreditInfo.getString("payment_receipt"),
                        response.getString("message"),
                        response.getString("message_date"),
                        response.getBoolean("message_pending"),
                        response.getInt("user"));
                LocalBroadcastManager.getInstance(ctx).sendBroadcast(new Intent(MainActivity.UPDATE_USER_DATA_INTENT));
            } catch (JSONException e) {
                throw new RuntimeException("error parsing userdata", e);
            }
        }, error -> {
            if (error instanceof AuthFailureError) {
                sendSignInBroadcast();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", "Bearer " + accessTokenFinal);
                return params;
            }
        };
        instance.addToRequestQueue(jsonObjectRequest);
    }

    public void verifyToken(String token, Response.Listener<JSONObject> responseListener,
                            Response.ErrorListener errorListener) {
        if (token.isEmpty()) {
            sendSignInBroadcast();
        }
        AtomicReference<TokenResult> result = new AtomicReference<>();

        String url = endPoint + "api/token/verify/";
        JSONObject object;
        object = new JSONObject();
        try {
            object.put("token", token);
        } catch (JSONException ignored) {
        }

        JsonObjectRequest jsonObjectRequest =
                new JsonObjectRequest(Request.Method.POST,
                        url,
                        object,
                        responseListener,
                        errorListener) {
                    @Override
                    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                        try {
                            String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));
                            JSONObject jsonResponse = new JSONObject(jsonString);
                            jsonResponse.put("code", response.statusCode);
                            return Response.success(jsonResponse, HttpHeaderParser.parseCacheHeaders(response));
                        } catch (UnsupportedEncodingException | JSONException e) {
                            return Response.error(new ParseError(e));
                        }
                    }
                };

        instance.addToRequestQueue(jsonObjectRequest);
        result.get();
    }

    public void doRefreshToken(Response.Listener<JSONObject> responseListener,
                               Response.ErrorListener errorListener) {
        SharedPreferencesSingleton preferences = SharedPreferencesSingleton.getInstance(ctx);
        String url = endPoint + "api/token/refresh/";

        JSONObject object = new JSONObject();
        try {
            object.put("refresh", preferences.getRefreshToken());
        } catch (JSONException ignored) {
        }
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, object, responseListener, errorListener);

        instance.addToRequestQueue(jsonObjectRequest);
    }

    public void sendTrafficIncrement(long trafficBytes) {
        String accessToken = SharedPreferencesSingleton.getInstance(ctx).getAccessToken();
        String url = endPoint + "api/account/increment_traffic/";

        JSONObject object = new JSONObject();
        try {
            object.put("used_traffic_b", trafficBytes);
        } catch (JSONException ignored) {
        }
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.PUT, url, object,
                null, null) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", "Bearer " + accessToken);
                return params;
            }
        };

        instance.addToRequestQueue(jsonObjectRequest);
    }

    private void sendSignInBroadcast() {
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(new Intent(MainActivity.SIGN_IN_ACTION).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    private TokenResult parseTokenError(VolleyError error) {
        if (error instanceof NoConnectionError) {
            return TokenResult.NO_CONNECTION;
        }
        if (error instanceof AuthFailureError) {
            return TokenResult.UNAUTHORIZED;
        }
        return TokenResult.TIMEOUT;
    }

    private FetchDbResult parseFetchError(VolleyError error) {
        if (error instanceof NoConnectionError) {
            return FetchDbResult.NO_CONNECTION;
        }
        if (error instanceof AuthFailureError) {
            return FetchDbResult.AUTH_FAIL;
        }
        return FetchDbResult.TIMEOUT;
    }


}
