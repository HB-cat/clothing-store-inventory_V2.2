package com.heben.client;

import android.app.Application;
import com.heben.client.util.AppContext;

public class ClothingSIApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppContext.init(this);
    }
}
