package com.securelight.secureshellv.vpnservice.v2ray;

import android.content.Context;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.securelight.secureshellv.backend.DataManager;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

import libv2ray.CoreCallbackHandler;
import libv2ray.CoreController;
import libv2ray.Libv2ray;

public class V2rayCoreManager {
    private final CoreController coreController;
    private Context context;
    private CoreCallbackHandler coreCallbackHandler;
    private String TAG = V2rayCoreManager.class.getName();
    private boolean isCoreInit = false;

    public V2rayCoreManager(Context context) {
        this.context = context.getApplicationContext();
        coreCallbackHandler = new CoreCallbackHandler() {
            @Override
            public long onEmitStatus(long l, String s) {
                return 0;
            }

            @Override
            public long shutdown() {
                return 0;
            }

            @Override
            public long startup() {
                return 0;
            }
        };
        coreController = Libv2ray.newCoreController(coreCallbackHandler);
    }

    public void initCore() {
        if (isCoreInit) {
            return;
        }
        Libv2ray.initCoreEnv(getAssetsPath(), getEncodedDeviceId());
        isCoreInit = true;
    }

    public long getDownloadSpeed() {
        return coreController.queryStats("block", "downlink") + coreController.queryStats("proxy", "downlink");
    }

    public long getUploadSpeed() {
        return coreController.queryStats("block", "uplink") + coreController.queryStats("proxy", "uplink");
    }

    private String getAssetsPath() {
        try {
            if (context.getExternalFilesDir("assets") == null) {
                return context.getDir("assets", 0).getAbsolutePath();
            } else {
                return Objects.requireNonNull(context.getExternalFilesDir("assets")).getAbsolutePath();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get user asset path", e);
        }
        return "";
    }

    private String getEncodedDeviceId() {
        byte[] androidId = Settings.Secure.ANDROID_ID.getBytes(StandardCharsets.UTF_8);
        byte[] padded = Arrays.copyOf(androidId, 32);

        return Base64.encodeToString(
                padded,
                Base64.NO_PADDING | Base64.URL_SAFE
        );
    }

    public void startLoop(String config, int fd) throws Exception {
        Log.i(TAG, "V2ray Config: " + config);
        coreController.startLoop(config, fd);
    }

    public void stopLoop() {
        try {
            coreController.stopLoop();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public boolean isCoreRunning() {
        return coreController.getIsRunning();
    }

    public long measureDelay(String s) throws Exception {
        return coreController.measureDelay(s);
    }


    public static long getConfigDelay(final String config) {
        try {
            JSONObject config_json = new JSONObject(config);
            config_json.remove("routing");
            config_json.remove("dns");
            JSONObject routing = new JSONObject();
            routing.put("domainStrategy", "IPIfNonMatch");
            config_json.put("routing", routing);
            config_json.put("dns", new JSONObject("{\n" +
                    "    \"hosts\": {\n" +
                    "        \"domain:googleapis.cn\": \"googleapis.com\"\n" +
                    "    },\n" +
                    "    \"servers\": [\n" +
                    "        \"1.1.1.1\"\n" +
                    "    ]\n" +
                    "}"));
            return Libv2ray.measureOutboundDelay(config, "");
        } catch (Exception json_error) {
            Log.d("MeasureOutboundDelay_V2rayConfig", "getCurrentServerDelay -> ", json_error);
            return -1;
        }
    }
}
