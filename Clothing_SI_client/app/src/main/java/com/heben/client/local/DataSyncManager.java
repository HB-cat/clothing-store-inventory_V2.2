package com.heben.client.local;

import android.content.Context;
import android.content.SharedPreferences;

import com.heben.client.local.entity.LocalAttributeGroup;
import com.heben.client.local.entity.LocalAttributeValue;
import com.heben.client.local.entity.LocalCategory;
import com.heben.client.local.entity.LocalProduct;
import com.heben.client.local.entity.LocalProductCombination;
import com.heben.client.model.Models;
import com.heben.client.network.ApiClient;
import com.heben.client.util.AppContext;
import com.heben.client.util.TokenManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Response;

/**
 * 数据同步管理器
 * 每日首次打开应用时拉取服务器数据到本地Room缓存
 * 数据库损坏时自动重建 + 重新拉取
 */
public class DataSyncManager {

    private static final String PREF_NAME = "sync_prefs";
    private static final String KEY_LAST_SYNC_DATE = "last_sync_date";

    private final AppDatabase db;
    private final SharedPreferences prefs;

    public DataSyncManager() {
        Context ctx = AppContext.get();
        this.db = AppDatabase.getInstance(ctx);
        this.prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 检查是否需要同步，需要则执行
     * 在 MainActivity.onCreate() 中调用
     */
    public void checkAndSync() {
        if (!TokenManager.isLoggedIn()) return;

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String lastSync = prefs.getString(KEY_LAST_SYNC_DATE, "");

        if (!today.equals(lastSync) || isDatabaseEmpty()) {
            doSync();
            prefs.edit().putString(KEY_LAST_SYNC_DATE, today).apply();
        }
    }

    /**
     * 是否数据库为空（用于损坏后重建判断）
     */
    private boolean isDatabaseEmpty() {
        try {
            Long uid = TokenManager.getUserId();
            return uid != null && db.categoryDao().count(uid) == 0;
        } catch (Exception e) {
            // 数据库表不存在或损坏，返回 true 触发重建
            return true;
        }
    }

    /**
     * 执行同步（拉取服务器数据覆盖本地）
     */
    private void doSync() {
        Long userId = TokenManager.getUserId();
        if (userId == null) return;

        try {
            syncCategories(userId);
            syncProducts(userId);
            syncAttributes(userId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void syncCategories(long userId) throws Exception {
        Response<com.heben.client.model.ApiResponse<List<Models.Category>>> resp =
                ApiClient.getService().getCategories().execute();
        if (resp.isSuccessful() && resp.body() != null && resp.body().isSuccess()) {
            List<Models.Category> data = resp.body().getData();
            if (data != null) {
                List<LocalCategory> local = new ArrayList<>();
                for (Models.Category c : data) {
                    LocalCategory lc = new LocalCategory();
                    lc.id = c.id;
                    lc.userId = userId;
                    lc.name = c.name;
                    lc.sortOrder = c.sortOrder;
                    lc.isPreset = c.isPreset != 0;
                    local.add(lc);
                }
                db.categoryDao().deleteAll(userId);
                db.categoryDao().insertAll(local);
            }
        }
    }

    private void syncProducts(long userId) throws Exception {
        Response<com.heben.client.model.ApiResponse<List<Models.Product>>> resp =
                ApiClient.getService().getProducts(null, "").execute();
        if (resp.isSuccessful() && resp.body() != null && resp.body().isSuccess()) {
            List<Models.Product> data = resp.body().getData();
            if (data != null) {
                List<LocalProduct> products = new ArrayList<>();
                List<LocalProductCombination> combos = new ArrayList<>();
                for (Models.Product p : data) {
                    LocalProduct lp = new LocalProduct();
                    lp.id = p.id;
                    lp.userId = userId;
                    lp.name = p.name;
                    lp.categoryId = p.categoryId;
                    lp.purchasePrice = p.purchasePrice;
                    lp.sellingPrice = p.sellingPrice;
                    lp.imageUrl = p.imageUrl;
                    lp.hasAttributes = p.hasAttributes;
                    lp.status = 1;
                    products.add(lp);

                    if (p.combinations != null) {
                        for (Models.ProductAttrCombination c : p.combinations) {
                            LocalProductCombination lc = new LocalProductCombination();
                            lc.id = c.id;
                            lc.productId = c.productId;
                            lc.attrsJson = c.attrsJson;
                            lc.stock = c.stock;
                            lc.version = c.version;
                            combos.add(lc);
                        }
                    }
                }
                db.productDao().deleteAll(userId);
                db.productDao().insertAll(products);
                db.productDao().deleteAllCombinations();
                db.productDao().insertAllCombinations(combos);
            }
        }
    }

    private void syncAttributes(long userId) throws Exception {
        Response<com.heben.client.model.ApiResponse<List<Models.AttributeGroup>>> resp =
                ApiClient.getService().getAttributeGroups().execute();
        if (resp.isSuccessful() && resp.body() != null && resp.body().isSuccess()) {
            List<Models.AttributeGroup> data = resp.body().getData();
            if (data != null) {
                List<LocalAttributeGroup> groups = new ArrayList<>();
                List<LocalAttributeValue> values = new ArrayList<>();
                for (Models.AttributeGroup g : data) {
                    LocalAttributeGroup lg = new LocalAttributeGroup();
                    lg.id = g.id;
                    lg.userId = userId;
                    lg.name = g.name;
                    lg.isPreset = g.isPreset != 0;
                    groups.add(lg);

                    if (g.values != null) {
                        for (Models.AttributeValue v : g.values) {
                            LocalAttributeValue lv = new LocalAttributeValue();
                            lv.id = v.id;
                            lv.groupId = v.groupId;
                            lv.value = v.value;
                            lv.sortOrder = v.sortOrder;
                            values.add(lv);
                        }
                    }
                }
                db.attributeGroupDao().deleteAll(userId);
                db.attributeGroupDao().insertAll(groups);
                db.attributeGroupDao().deleteAllValues();
                db.attributeGroupDao().insertAllValues(values);
            }
        }
    }
}
