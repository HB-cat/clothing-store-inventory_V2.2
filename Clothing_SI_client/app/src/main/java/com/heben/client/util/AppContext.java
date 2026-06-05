package com.heben.client.util;

import android.app.Application;

public class AppContext {
    private static Application instance;

    public static void init(Application app) {
        instance = app;
    }

    public static Application get() {
        return instance;
    }
}
