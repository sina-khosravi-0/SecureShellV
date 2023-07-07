package com.example.secureshellv;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.VpnService;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.client.session.SessionFactory;
import org.apache.sshd.client.session.forward.PortForwardingTracker;
import org.apache.sshd.common.forward.PortForwardingEventListener;
import org.apache.sshd.common.forward.PortForwardingManager;
import org.apache.sshd.common.future.CancelOption;
import org.apache.sshd.common.util.net.SshdSocketAddress;
import org.apache.sshd.server.forward.AgentForwardingFilter;
import org.apache.sshd.server.forward.ForwardingFilter;
import org.apache.sshd.server.forward.TcpForwardingFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {


    ServerConfig config = new ServerConfig();
    boolean connected = false;

    ApplicationInfo packageInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            packageInfo = this.getPackageManager().getApplicationInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    SshClient sshClient;

    ClientSession session;

    PortForwardingTracker portForwardingTracker;

    public void onBtnClick(View view) {
        if (connected) {
            return;
        }
        Toast myToast = Toast.makeText(this, "connected", Toast.LENGTH_SHORT);
        new Thread(() -> {
            try {
                String user = "sina";
                String host = "test.weary.tech";
                int port = 22;
                System.setProperty("user.home", user);
                sshClient = SshClient.setUpDefaultClient();
                sshClient.start();
                // todo: add port forwarding event listener actions.
//                sshClient.addPortForwardingEventListener(new PortForwardingEventListener() {
//                    @Override
//                    public void establishingDynamicTunnel(org.apache.sshd.common.session.Session session, SshdSocketAddress local) throws IOException {
//                        PortForwardingEventListener.super.establishingDynamicTunnel(session, local);
//                    }
//
//                    @Override
//                    public void establishedDynamicTunnel(org.apache.sshd.common.session.Session session, SshdSocketAddress local, SshdSocketAddress boundAddress, Throwable reason) throws IOException {
//                        PortForwardingEventListener.super.establishedDynamicTunnel(session, local, boundAddress, reason);
//                    }
//
//                    @Override
//                    public void tearingDownDynamicTunnel(org.apache.sshd.common.session.Session session, SshdSocketAddress address) throws IOException {
//                        PortForwardingEventListener.super.tearingDownDynamicTunnel(session, address);
//                    }
//
//                    @Override
//                    public void tornDownDynamicTunnel(org.apache.sshd.common.session.Session session, SshdSocketAddress address, Throwable reason) throws IOException {
//                        PortForwardingEventListener.super.tornDownDynamicTunnel(session, address, reason);
//                    }
//                });
                //Create a session
                session = sshClient.connect(user, host, port)
                        .verify(5, TimeUnit.SECONDS, CancelOption.CANCEL_ON_TIMEOUT).getSession();
                session.addPasswordIdentity("Sina@13112040");
                session.auth().verify(5, TimeUnit.SECONDS, CancelOption.CANCEL_ON_TIMEOUT);
                portForwardingTracker = session.createDynamicPortForwardingTracker(
                        new SshdSocketAddress("10.87.0.37", 10808));
                myToast.setText("10.87.0.37:10808");
                myToast.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static long initialTx = 0;
    public static long newTx = 0;
    long received = 0;

    public void onCheckClicked(View view) throws UnknownHostException, MalformedURLException {
        Toast myToast = Toast.makeText(this, "connected", Toast.LENGTH_SHORT);

        ConnectivityManager connectivityManager = getSystemService(ConnectivityManager.class);
        Network currentNetwork = connectivityManager.getActiveNetwork();
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(currentNetwork);
        LinkProperties linkProperties = connectivityManager.getLinkProperties(currentNetwork);
        String urlString = "https://checkip.amazonaws.com/";
        URL url = new URL(urlString);
        new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))) {
                myToast.setText(br.readLine());
                myToast.show();
            } catch (IOException e) {
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

    public void disconnectOnClick(View view) throws IOException {
        portForwardingTracker.close();
        session.close();
    }

    public void onDestroyClicked(View view) {

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

//                session = jsch.getSession(user, host, port);
//                // Prompt for the user password
//                session.setPassword("Sina@13112040");
//                Properties config = new Properties();
//                config.put("StrictHostKeyChecking", "no");
//                session.setConfig(config);
//                session.connect();
//                connected = true;
//                new Thread(() -> {
//                    try {
//                        while (connected) {
//                            while (session.getPortForwardingL().length == 0) {
//                                session.setPortForwardingL("127.0.0.1", 2000, "127.0.0.1", 3128);
//                                // change ServerConfig too after changing port forwarding
//                                Log.d("SSH", "Local port forwarding on 192.168.1.12:2000");
//                                runOnUiThread(() -> {
//                                    myToast.setText("Port forwarding done, 192.168.1.12:2000");
//                                    myToast.show();
//                                });
//                                Thread.sleep(2000);
//                            }
//                            Thread.sleep(2000);
//                        }
//                    } catch (Exception e) {
//                        throw new RuntimeException(e);
//                    }
//                }).start();

