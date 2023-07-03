package com.example.secureshellv;

import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;


public class MainActivity extends AppCompatActivity {


    ServerConfig config = new ServerConfig();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    JSch jsch = new JSch();
    Session session;


    public void onBtnClick(View view) {
        Toast myToast = Toast.makeText(this, "connected", Toast.LENGTH_SHORT);
        new Thread(() -> {
            try {
                String user = "sina";
                String host = "test.weary.tech";
                int port = 22;
                session = jsch.getSession(user, host, port);
                // Prompt for the user password
                session.setPassword("Sina@13112040");
                Properties config = new Properties();
                config.put("StrictHostKeyChecking", "no");
                session.setConfig(config);
                session.connect();
                session.setPortForwardingL("127.0.0.1", 2000, "127.0.0.1", 3128);

                runOnUiThread(() -> {
                    myToast.setText("PF Connected");
                    myToast.show();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {

        if (result == RESULT_OK) {
            startService(getServiceIntent());
        }
        super.onActivityResult(request, result, data);
    }

    private Intent getServiceIntent() {
        return new Intent(this, SSVpnService.class).putExtra("config", config);
    }

    public void disconnectOnClick(View view) {
        session.disconnect();
        System.exit(0);
    }
    public void onStartClicked(View view) {
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            startActivityForResult(intent, 0);
        } else {
            onActivityResult(0, RESULT_OK, null);
        }
    }
    public void onStopClicked(View view) {
        Intent intent = new Intent("stop_kill");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}