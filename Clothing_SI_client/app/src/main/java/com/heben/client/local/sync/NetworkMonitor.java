package com.heben.client.local.sync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;

/**
 * 网络状态监听器
 * 网络恢复时自动触发数据同步
 */
public class NetworkMonitor {

    private static final String TAG = "NetworkMonitor";
    private final Context context;
    private final SyncEngine syncEngine;
    private ConnectivityManager.NetworkCallback callback;

    public NetworkMonitor(Context context) {
        this.context = context.getApplicationContext();
        this.syncEngine = new SyncEngine(this.context);
    }

    public void start() {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                Log.i(TAG, "网络恢复，开始同步离线数据...");
                boolean ok = syncEngine.syncAll();
                Log.i(TAG, "离线数据同步" + (ok ? "完成 ✅" : "部分失败 ⚠️"));
            }
        };

        cm.registerNetworkCallback(request, callback);
    }

    public void stop() {
        if (callback != null) {
            ConnectivityManager cm = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                cm.unregisterNetworkCallback(callback);
            }
            callback = null;
        }
    }
}
