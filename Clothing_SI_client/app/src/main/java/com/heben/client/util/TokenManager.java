package com.heben.client.util;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {
    private static final String PREF_NAME = "clothing_si_prefs";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_SHOP_NAME = "shop_name";
    private static final String KEY_ROLE = "role";
    private static final String KEY_NICKNAME = "nickname";

    private static SharedPreferences getPrefs() {
        return AppContext.get().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void saveLogin(String token, String userId, String username, String shopName, int role, String nickname) {
        getPrefs().edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_USER_ID, userId)
                .putString(KEY_USERNAME, username)
                .putString(KEY_SHOP_NAME, shopName)
                .putInt(KEY_ROLE, role)
                .putString(KEY_NICKNAME, nickname)
                .apply();
    }

    public static String getToken() { return getPrefs().getString(KEY_TOKEN, null); }
    public static Long getUserId() {
        String id = getPrefs().getString(KEY_USER_ID, null);
        return id != null ? Long.parseLong(id) : null;
    }
    public static String getUsername() { return getPrefs().getString(KEY_USERNAME, null); }
    public static String getShopName() { return getPrefs().getString(KEY_SHOP_NAME, null); }
    public static int getRole() { return getPrefs().getInt(KEY_ROLE, 0); }
    public static String getNickname() { return getPrefs().getString(KEY_NICKNAME, null); }

    public static boolean isLoggedIn() { return getToken() != null; }

    public static void logout() {
        getPrefs().edit().clear().apply();
    }
}
