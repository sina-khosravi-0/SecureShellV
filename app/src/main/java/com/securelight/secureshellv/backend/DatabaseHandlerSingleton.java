package com.securelight.secureshellv.backend;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.collection.LruCache;

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
import com.securelight.secureshellv.statics.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

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
    ExecutorService executorService = Executors.newSingleThreadExecutor();

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

    public static FetchDbResult fetchTokens(String username, String password) {
        ReentrantLock lock = new ReentrantLock();
        Condition condition = lock.newCondition();
        SharedPreferences preferences = ctx.getSharedPreferences(Constants.API_CACHE_PREFERENCES_NAME, Activity.MODE_PRIVATE);

        String url = "http://192.168.19.71:8000/api/token/";
        JSONObject object;
        object = new JSONObject();
        try {
            object.put("username", username);
            object.put("password", password);
        } catch (JSONException ignored) {
        }
        AtomicReference<FetchDbResult> result = new AtomicReference<>();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, object, response -> {
            try {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("access", response.getString("access"));
                editor.putString("refresh", response.getString("refresh"));
                editor.apply();
                result.set(FetchDbResult.SUCCESS);
            } catch (JSONException e) {
                throw new RuntimeException("Unexpected error while fetching tokens", e);
            }
            try {
                lock.lock();
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        }, error -> {
            result.set(parseFetchError(error));
            try {
                lock.lock();
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        });

        instance.addToRequestQueue(jsonObjectRequest);
        try {
            lock.lock();
            // wait until a response is made
            condition.await();
            throw new InterruptedException();
        } catch (InterruptedException ignored) {
        } finally {
            lock.unlock();
        }
        return result.get();
    }

    public static FetchDbResult fetchUserData() {
        ReentrantLock lock = new ReentrantLock();
        Condition condition = lock.newCondition();

        SharedPreferences preferences = ctx.getSharedPreferences(Constants.API_CACHE_PREFERENCES_NAME, Activity.MODE_PRIVATE);
        String accessToken = preferences.getString("access", "");
        String refreshToken = preferences.getString("refresh", "");

        switch (verifyToken(accessToken)) {
            case TOKEN_INVALID:
            case NO_AUTH:
                if (verifyToken(refreshToken) == TokenResult.TOKEN_VALID) {
                    doRefreshToken();
                } else {
                    // Both tokens are invalid. Send broadcast to start sign in again
//                LocalBroadcastManager.getInstance(ctx).sendBroadcast(new Intent(MainActivity.DO_SIGN_IN_BR));
                    return FetchDbResult.AUTH_FAIL;
                }
        }

        AtomicReference<FetchDbResult> result = new AtomicReference<>();
        String accessTokenFinal = preferences.getString("access", "");
        String url = "http://192.168.19.71:8000/api/users/user/";
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
            UserData userData = UserData.getInstance();
            userData = UserData.getInstance();
            userData = UserData.getInstance();
            userData = UserData.getInstance();
            userData = UserData.getInstance();
            userData = UserData.getInstance();


            try {
                JSONObject userCreditInfo = response.getJSONObject("user_credit_info");
                userData.parseData(userCreditInfo.getDouble("remaining_gb"),
                        userCreditInfo.getString("end_credit_date"),
                        userCreditInfo.getLong("total_traffic_b"),
                        userCreditInfo.getLong("total_traffic_b"),
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
            } catch (JSONException e) {
                throw new RuntimeException("error parsing userdata", e);
            }

            try {
                lock.lock();
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        }, error -> {
            result.set(parseFetchError(error));
            try {
                lock.lock();
                condition.signalAll();
            } finally {
                lock.unlock();
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
        try {
            lock.lock();
            // wait until a response is made
            condition.await();
        } catch (InterruptedException ignored) {
        } finally {
            lock.unlock();
        }
        return result.get();
    }

    public static TokenResult verifyToken(String token) {
        if (token.isEmpty()) {
            return TokenResult.NO_AUTH;
        }
        ReentrantLock lock = new ReentrantLock();
        Condition condition = lock.newCondition();
        AtomicReference<TokenResult> result = new AtomicReference<>();


        String url = "http://192.168.19.71:8000/api/token/verify/";
        JSONObject object;
        object = new JSONObject();
        try {
            object.put("token", token);
        } catch (JSONException ignored) {
        }
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, object, response -> {
            try {
                // success only if code == 200
                if (response.getString("code").equals("200")) {
                    result.set(TokenResult.TOKEN_VALID);
                }
            } catch (JSONException ignored) {
            }

            try {
                lock.lock();
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        }, error -> {
            if (error instanceof AuthFailureError) {
                result.set(TokenResult.TOKEN_INVALID);
            } else if (error instanceof TimeoutError) {
                result.set(TokenResult.TIMEOUT);
            }
            // TODO: handle more errors?
            try {
                lock.lock();
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        }) {
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


        try {
            lock.lock();
            // wait until a response is made
            condition.await();
            throw new InterruptedException();
        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }

        return result.get();
    }

    public static TokenResult doRefreshToken() {
        ReentrantLock lock = new ReentrantLock();
        Condition condition = lock.newCondition();
        SharedPreferences preferences = ctx.getApplicationContext().getSharedPreferences(Constants.API_CACHE_PREFERENCES_NAME, Activity.MODE_PRIVATE);

        String url = "http://192.168.19.71:8000/api/token/refresh/";
        JSONObject object;
        object = new JSONObject();
        try {
            object.put("refresh", preferences.getString("refresh", ""));
        } catch (JSONException ignored) {
        }
        AtomicReference<TokenResult> result = new AtomicReference<>();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, object, response -> {
            try {
                SharedPreferences.Editor editor = preferences.edit();
                if (response.has("access")) {
                    editor.putString("access", response.getString("access"));
                }
                if (response.has("refresh")) {
                    editor.putString("refresh", response.getString("refresh"));
                }
                editor.apply();
                result.set(TokenResult.TOKEN_VALID);
            } catch (JSONException ignored) {
            }
            try {
                lock.lock();
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        }, error -> {
            result.set(parseTokenError(error));
            try {
                lock.lock();
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        });

        instance.addToRequestQueue(jsonObjectRequest);

        try {
            lock.lock();
            // wait until a response is made
            condition.await();
        } catch (InterruptedException ignored) {
//            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
        return result.get();
    }

    private static TokenResult parseTokenError(VolleyError error) {
        if (error instanceof NoConnectionError) {
            return TokenResult.NO_CONNECTION;
        }
        if (error instanceof AuthFailureError) {
            return TokenResult.UNAUTHORIZED;
        }
        return TokenResult.TIMEOUT;
    }

    private static FetchDbResult parseFetchError(VolleyError error) {
        if (error instanceof NoConnectionError) {
            return FetchDbResult.NO_CONNECTION;
        }
        if (error instanceof AuthFailureError) {
            return FetchDbResult.AUTH_FAIL;
        }
        return FetchDbResult.TIMEOUT;
    }


}
