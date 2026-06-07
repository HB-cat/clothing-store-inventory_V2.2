package com.heben.client;

import android.app.Application;

import com.heben.client.local.sync.NetworkMonitor;
import com.heben.client.util.AppContext;

public class ClothingSIApp extends Application {

    private NetworkMonitor networkMonitor;

    @Override
    public void onCreate() {
        super.onCreate();
        AppContext.init(this);

        // 启动网络监听（网络恢复时自动同步离线数据）
        networkMonitor = new NetworkMonitor(this);
        networkMonitor.start();
    }
}
