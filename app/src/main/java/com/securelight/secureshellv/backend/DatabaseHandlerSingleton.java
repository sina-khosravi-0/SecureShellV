package com.securelight.secureshellv.backend;


import static com.securelight.secureshellv.statics.Constants.apiAddress;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;

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
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.securelight.secureshellv.statics.Intents;
import com.securelight.secureshellv.ui.homepage.HomepageActivity;
import com.securelight.secureshellv.statics.Constants;
import com.securelight.secureshellv.utility.SharedPreferencesSingleton;
import com.securelight.secureshellv.vpnservice.SSVpnService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public class DatabaseHandlerSingleton {
    private static DatabaseHandlerSingleton instance;
    private final ImageLoader imageLoader;
    private final Context context;
    private RequestQueue requestQueue;

    private DatabaseHandlerSingleton(@NonNull Context context) {
        // getApplicationContext() is key, it keeps you from leaking the
        // Activity or BroadcastReceiver if someone passes one in.
        this.context = context.getApplicationContext();
        requestQueue = getRequestQueue();

        imageLoader = new ImageLoader(requestQueue, new ImageLoader.ImageCache() {
            private final LruCache<String, Bitmap> cache = new LruCache<>(20);

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
            requestQueue = Volley.newRequestQueue(context);
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
        String url = apiAddress + "api/token/";
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
        SharedPreferencesSingleton preferences = SharedPreferencesSingleton.getInstance(context);
        String accessToken = preferences.getAccessToken();
        String url = apiAddress + "api/account/details/";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, null, null) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", "Bearer " + accessToken);
                return params;
            }

            @Override
            protected VolleyError parseNetworkError(VolleyError error) {
                if (error instanceof AuthFailureError) {
                    handleAuthFailureError(error);
                }
                return error;
            }

            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                try {
                    String jsonString = new String(response.data,
                            HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));
                    DataManager dataManager = DataManager.getInstance();

                    try {
                        dataManager.parseData(new JSONObject(jsonString));
                        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Intents.UPDATE_USER_DATA_INTENT));
                    } catch (JSONException e) {
                        Log.d("DatabaseHandler", "error parsing userdata", e);
                    }

                    return Response.success(new JSONObject(jsonString), HttpHeaderParser.parseCacheHeaders(response));
                } catch (UnsupportedEncodingException | JSONException e) {
                    return Response.error(new ParseError(e));
                }
            }
        };
        instance.addToRequestQueue(jsonObjectRequest);
    }

    /**
     * handles volley request auth failures
     */
    private void handleAuthFailureError(VolleyError error) {
        // refresh tokens and fetch user data again
        String errorData = new String(error.networkResponse.data);
        if (errorData.contains(Constants.TOKEN_INVALID_CODE_STRING)) {
            new Thread(() -> {
                if (requestTokenRefresh()) {
                    fetchUserData();
                }
            }).start();
        } else if (errorData.contains("password_changed") || errorData.contains("user_inactive")) {
            broadcastSignIn();
        } else if (errorData.contains(Constants.OUT_OF_TRAFFIC_CODE_STRING)) {
            broadcastTrafficUsageLimit();
        } else if (errorData.contains(Constants.CREDIT_EXPIRED_CODE_STRING)) {
            broadcastCreditExpired();
        }
        System.out.println(errorData);
    }

    public void verifyToken(String token, Response.Listener<JSONObject> responseListener,
                            Response.ErrorListener errorListener) {
        if (token.isEmpty()) {
            broadcastSignIn();
        }
        AtomicReference<TokenResult> result = new AtomicReference<>();

        String url = apiAddress + "api/token/verify/";
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

    public boolean requestTokenRefresh() {
        SharedPreferencesSingleton preferences = SharedPreferencesSingleton.getInstance(context);
        String url = apiAddress + "api/token/refresh/";

        JSONObject object = new JSONObject();
        try {
            object.put("refresh", preferences.getRefreshToken());
        } catch (JSONException ignored) {
        }

        RequestFuture<JSONObject> future = RequestFuture.newFuture();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, object, future, future);
        instance.addToRequestQueue(jsonObjectRequest);

        try {
            JSONObject response = future.get(10, TimeUnit.SECONDS);
            try {
                if (response.has("access")) {
                    preferences.saveAccessToken(response.getString("access"));
                }
                if (response.has("refresh")) {
                    preferences.saveRefreshToken(response.getString("refresh"));
                }
            } catch (JSONException ignored) {
            }
        } catch (InterruptedException | TimeoutException ignored) {

        } catch (ExecutionException e) {
            if (e.getCause() instanceof AuthFailureError) {
                handleAuthFailureError((VolleyError) e.getCause());
            }
        }
        return true;
    }

    public boolean sendTrafficIncrement(long trafficBytes) {
        if (trafficBytes == 0) {
            return true;
        }
        String accessToken = SharedPreferencesSingleton.getInstance(context).getAccessToken();
        String url = apiAddress + "api/account/increment_datausage/";

        JSONObject object = new JSONObject();
        try {
            object.put("used_data_b", trafficBytes);
        } catch (JSONException ignored) {
        }
        RequestFuture<JSONObject> future = RequestFuture.newFuture();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.PUT, url, object,
                future, future) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", "Bearer " + accessToken);
                return params;
            }
        };

        instance.addToRequestQueue(jsonObjectRequest);
        try {
            future.get(10, TimeUnit.SECONDS);
            return true;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            return false;
        }
    }

    public String retrievePassword(int serverId, boolean reset) {
        String accessToken = SharedPreferencesSingleton.getInstance(context).getAccessToken();

        String url = apiAddress + "api/pass/" + serverId;
        if (reset) {
            url = apiAddress + "api/reset_pass/" + serverId;
        }

        RequestFuture<JSONObject> future = RequestFuture.newFuture();
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, future, future) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", "Bearer " + accessToken);
                return params;
            }
        };
        instance.addToRequestQueue(request);
        try {
            JSONObject object = future.get(10, TimeUnit.SECONDS);
            return object.getString("password");
        } catch (InterruptedException | TimeoutException | JSONException e) {
            return "";
        } catch (ExecutionException e) {
            if (e.getCause() instanceof AuthFailureError) {
                NetworkResponse networkResponse = ((AuthFailureError) (e.getCause())).networkResponse;
                if (networkResponse.statusCode == 403) {
                    if (new String(networkResponse.data).contains(Constants.OUT_OF_TRAFFIC_CODE_STRING)) {
                        LocalBroadcastManager.getInstance(context).sendBroadcast(
                                new Intent(Intents.STOP_VPN_SERVICE_ACTION)
                                        .putExtra(Constants.OUT_OF_TRAFFIC_CODE_STRING, true));
                    }
                    if (new String(networkResponse.data).contains(Constants.CREDIT_EXPIRED_CODE_STRING)) {
                        LocalBroadcastManager.getInstance(context).sendBroadcast(
                                new Intent(Intents.STOP_VPN_SERVICE_ACTION)
                                        .putExtra(Constants.CREDIT_EXPIRED_CODE_STRING, true));
                    }
                }
            }
        }
        return "";
    }

    public void sendHeartbeat() {
        String accessToken = SharedPreferencesSingleton.getInstance(context).getAccessToken();
        String url = apiAddress + "api/heartbeat/";
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, null, null) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", "Bearer " + accessToken);
                return params;
            }

            @Override
            protected VolleyError parseNetworkError(VolleyError error) {
                if (error instanceof AuthFailureError) {
                    handleAuthFailureError(error);
                }
                return error;
            }
        };
        instance.addToRequestQueue(request);
    }

    public JSONArray fetchServerList(String location) {
        String accessToken = SharedPreferencesSingleton.getInstance(context).getAccessToken();
        String url = apiAddress + "api/servers/location/" + location;

        RequestFuture<JSONArray> future = RequestFuture.newFuture();
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null, future, future) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", "Bearer " + accessToken);
                return params;
            }
        };
        instance.addToRequestQueue(request);
        try {
            return future.get(20, TimeUnit.SECONDS);
//            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(MainActivity.UPDATE_USER_DATA_INTENT));
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            Log.d("DatabaseHandler", ex.getMessage(), ex);
            return new JSONArray();
        }
    }

    public JSONArray fetchConfigs(String location) {
        String accessToken = SharedPreferencesSingleton.getInstance(context).getAccessToken();
        String url = apiAddress + "api/servers/config/" + location;

        RequestFuture<JSONArray> future = RequestFuture.newFuture();
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null, future, future) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", "Bearer " + accessToken);
                return params;
            }
        };
        instance.addToRequestQueue(request);

        try {
            return future.get(20, TimeUnit.SECONDS);
//            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(MainActivity.UPDATE_USER_DATA_INTENT));
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            Log.d("DatabaseHandler", ex.getMessage(), ex);
            return new JSONArray();
        }
    }

    public JSONArray fetchServicePlans(boolean gold) {
        String accessToken = SharedPreferencesSingleton.getInstance(context).getAccessToken();
        String url = apiAddress + "api/account/services/";
        if (gold) {
            url += "gold/";
        }

        RequestFuture<JSONArray> future = RequestFuture.newFuture();
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null, future, future) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", "Bearer " + accessToken);
                return params;
            }
        };
        instance.addToRequestQueue(request);
        try {
            return future.get(20, TimeUnit.SECONDS);
//            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(MainActivity.UPDATE_USER_DATA_INTENT));
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            Log.d("DatabaseHandler", ex.getMessage(), ex);
            return new JSONArray();
        }
    }

    public List<Integer> fetchPlanDurations() {
        String accessToken = SharedPreferencesSingleton.getInstance(context).getAccessToken();
        String url = apiAddress + "api/account/durations/";

        RequestFuture<JSONArray> future = RequestFuture.newFuture();
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null, future, future) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", "Bearer " + accessToken);
                return params;
            }
        };
        instance.addToRequestQueue(request);
        try {
            JSONArray response = future.get(20, TimeUnit.SECONDS);
            List<Integer> durations = new ArrayList<>();
            for (int i = 0; i < response.length(); i++) {
                durations.add(response.getJSONObject(i).getInt("months"));
            }
            System.out.println(durations);
            return durations;
//            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(MainActivity.UPDATE_USER_DATA_INTENT));
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            Log.d("DatabaseHandler", ex.getMessage(), ex);
        } catch (JSONException ignored) {
        }
        return new ArrayList<>();
    }

    public List<String> fetchCardNumbers() {
        String accessToken = SharedPreferencesSingleton.getInstance(context).getAccessToken();
        String url = apiAddress + "api/card_numbers";

        RequestFuture<JSONArray> future = RequestFuture.newFuture();
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null, future, future) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", "Bearer " + accessToken);
                return params;
            }
        };
        instance.addToRequestQueue(request);
        try {
            JSONArray response = future.get(20, TimeUnit.SECONDS);
            List<String> cardNumbers = new ArrayList<>();
            for (int i = 0; i < response.length(); i++) {
                cardNumbers.add(response.getJSONObject(i).getString("card_number"));
            }
            return cardNumbers;
//            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(MainActivity.UPDATE_USER_DATA_INTENT));
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            Log.d("DatabaseHandler", ex.getMessage(), ex);
        } catch (JSONException ignored) {
        }
        return new ArrayList<>();
    }

    public void sendRenewRequest(final Bitmap bitmap, int servicePlanId, Response.Listener<NetworkResponse> responseListener,
                                 Response.ErrorListener errorListener) {
        String accessToken = SharedPreferencesSingleton.getInstance(context).getAccessToken();
        String url = apiAddress + "api/account/renew/";

        VolleyMultipartRequest volleyMultipartRequest = new VolleyMultipartRequest(Request.Method.PUT, url,
                responseListener, errorListener) {

            @NonNull
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("current_service_plan", String.valueOf(servicePlanId));
                return params;
            }

            @Override
            protected Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();
                long imageName = System.currentTimeMillis();
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 80, byteArrayOutputStream);
                params.put("payment_receipt", new DataPart(imageName + ".png", byteArrayOutputStream.toByteArray()));
                return params;
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", "Bearer " + accessToken);
                return params;
            }
        };
        instance.addToRequestQueue(volleyMultipartRequest);
    }

    private void broadcastSignIn() {
        LocalBroadcastManager.getInstance(context)
                .sendBroadcast(new Intent(Intents.SIGN_IN_ACTION)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    private void broadcastCreditExpired() {
        LocalBroadcastManager.getInstance(context)
                .sendBroadcast(new Intent(Intents.STOP_VPN_SERVICE_ACTION)
                        .putExtra(Constants.CREDIT_EXPIRED_CODE_STRING, true));
    }

    private void broadcastTrafficUsageLimit() {
        LocalBroadcastManager.getInstance(context)
                .sendBroadcast(new Intent(Intents.STOP_VPN_SERVICE_ACTION)
                        .putExtra(Constants.OUT_OF_TRAFFIC_CODE_STRING, true));
    }

    private void broadcastIpLimit() {
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

    enum TokenResult {
        TOKEN_VALID, TOKEN_INVALID, NO_AUTH, NO_CONNECTION, TIMEOUT, UNAUTHORIZED;

    }

    public enum FetchDbResult {
        SUCCESS, AUTH_FAIL, NO_CONNECTION, TIMEOUT,

    }
}
