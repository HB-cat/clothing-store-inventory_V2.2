package com.heben.client.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

public class TokenManager {
    private static final String PREF_NAME = "clothing_si_prefs";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_SHOP_NAME = "shop_name";
    private static final String KEY_ROLE = "role";
    private static final String KEY_NICKNAME = "nickname";
    private static final String KEY_PASSWORD = "password_hash"; // 离线登录用

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

    /** 保存密码（Base64编码，供离线登录验证） */
    public static void savePassword(String password) {
        String encoded = Base64.encodeToString(password.getBytes(), Base64.NO_WRAP);
        getPrefs().edit().putString(KEY_PASSWORD, encoded).apply();
    }

    /** 离线验证密码（用保存的密码比对） */
    public static boolean checkPassword(String username, String password) {
        String storedUsername = getUsername();
        if (storedUsername == null || !storedUsername.equals(username)) return false;
        String storedEncoded = getPrefs().getString(KEY_PASSWORD, null);
        if (storedEncoded == null) return false;
        String storedPwd = new String(Base64.decode(storedEncoded, Base64.NO_WRAP));
        return storedPwd.equals(password);
    }

    /** 是否有缓存的密码可用于离线登录 */
    public static boolean hasSavedPassword() {
        return getPrefs().getString(KEY_PASSWORD, null) != null;
    }

    /** 清除缓存的密码 */
    public static void clearPassword() {
        getPrefs().edit().remove(KEY_PASSWORD).apply();
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
    public static void saveNickname(String nickname) {
        getPrefs().edit().putString(KEY_NICKNAME, nickname).apply();
    }
    public static void saveShopName(String shopName) {
        getPrefs().edit().putString(KEY_SHOP_NAME, shopName).apply();
    }

    public static boolean isLoggedIn() { return getToken() != null; }

    /** 离线模式下是否可登录（有缓存的用户信息和密码） */
    public static boolean canOfflineLogin() {
        return getToken() == null && hasSavedPassword();
    }

    public static void logout() {
        getPrefs().edit().clear().apply();
    }
}
