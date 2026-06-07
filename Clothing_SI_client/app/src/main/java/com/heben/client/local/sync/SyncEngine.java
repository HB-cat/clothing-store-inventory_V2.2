package com.heben.client.local.sync;

import android.content.Context;
import android.util.Log;

import com.heben.client.local.AppDatabase;
import com.heben.client.local.dao.IdMappingDao;
import com.heben.client.local.dao.PurchaseRecordDao;
import com.heben.client.local.dao.SaleRecordDao;
import com.heben.client.local.dao.StockChangeDao;
import com.heben.client.local.entity.LocalIdMapping;
import com.heben.client.local.entity.LocalPurchaseRecord;
import com.heben.client.local.entity.LocalPurchaseRecordItem;
import com.heben.client.local.entity.LocalSaleRecord;
import com.heben.client.local.entity.LocalSaleRecordItem;
import com.heben.client.local.entity.LocalStockChange;
import com.heben.client.network.ApiClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import retrofit2.Response;

/**
 * 离线同步引擎
 * 职责：将本地Room中的脏数据(PENDING)按时间顺序推送到服务端
 * 规则：追加型操作（记账/进货）直接POST；库存用增量同步
 */
public class SyncEngine {

    private static final String TAG = "SyncEngine";
    private final AppDatabase db;
    private final Context context;

    public SyncEngine(Context context) {
        this.context = context.getApplicationContext();
        this.db = AppDatabase.getInstance(this.context);
    }

    /**
     * 执行完整同步
     * 按顺序：草稿账单 → 销售记录 → 进货记录 → 库存变更
     * 返回是否全部成功
     */
    public boolean syncAll() {
        if (!NetworkUtil.isConnected(context)) return false;

        boolean allOk = true;
        try {
            allOk &= syncDraftBills();
            allOk &= syncSaleRecords();
            allOk &= syncPurchaseRecords();
            allOk &= syncStockChanges();
        } catch (Exception e) {
            Log.e(TAG, "syncAll failed", e);
            return false;
        }
        return allOk;
    }

    /** 同步草稿账单（PENDING的draft_bill→服务器） */
    private boolean syncDraftBills() {
        // 草稿是先提交到服务端再操作，同步时跳过已有服务端id的PENDING记录
        return true;
    }

    /** 同步销售记录（PENDING的sale_record→POST /api/sales/quick） */
    private boolean syncSaleRecords() {
        List<LocalSaleRecord> pending = db.saleRecordDao().getPending();
        if (pending.isEmpty()) return true;

        boolean allOk = true;
        for (LocalSaleRecord local : pending) {
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("buyerName", local.buyerName);
                body.put("totalAmount", local.totalAmount);
                body.put("itemCount", local.itemCount);

                // 组装明细
                List<LocalSaleRecordItem> items = db.saleRecordDao().getItems(local.localId);
                List<Map<String, Object>> itemList = new ArrayList<>();
                for (LocalSaleRecordItem item : items) {
                    Map<String, Object> im = new HashMap<>();
                    im.put("productId", item.productId);
                    im.put("productName", item.productName);
                    im.put("combinationId", item.combinationId);
                    im.put("attrsJson", item.attrsJson);
                    im.put("quantity", item.quantity);
                    im.put("unitPrice", item.unitPrice);
                    im.put("purchasePrice", item.purchasePrice);
                    itemList.add(im);
                }
                body.put("items", itemList);

                // 同步时间
                String saleTime = local.saleTime != null ? local.saleTime :
                        new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
                                java.util.Locale.getDefault()).format(new java.util.Date());

                // 调用服务端API
                Response<com.heben.client.model.ApiResponse<com.heben.client.model.Models.SaleRecord>> resp =
                        ApiClient.getService().quickSale(body).execute();

                if (resp.isSuccessful() && resp.body() != null && resp.body().isSuccess()) {
                    Long serverId = resp.body().getData() != null ? resp.body().getData().id : null;
                    if (serverId != null) {
                        db.saleRecordDao().markSynced(local.localId, serverId);
                        // 映射本地→服务端id
                        LocalIdMapping mapping = new LocalIdMapping();
                        mapping.entityType = "sale_record";
                        mapping.localId = local.localId;
                        mapping.serverId = serverId;
                        mapping.syncedAt = System.currentTimeMillis();
                        db.idMappingDao().insert(mapping);
                    }
                } else {
                    if (resp.code() == 409) {
                        db.saleRecordDao().markConflict(local.localId);
                    }
                    allOk = false;
                }
            } catch (Exception e) {
                Log.e(TAG, "sync sale failed: " + local.localId, e);
                allOk = false;
            }
        }
        return allOk;
    }

    /** 同步进货记录（PENDING的purchase_record→POST /api/purchases/my-products） */
    private boolean syncPurchaseRecords() {
        List<LocalPurchaseRecord> pending = db.purchaseRecordDao().getPending();
        if (pending.isEmpty()) return true;

        boolean allOk = true;
        for (LocalPurchaseRecord local : pending) {
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("source", local.source);
                body.put("totalAmount", local.totalAmount);
                body.put("supplierName", local.supplierName);

                List<LocalPurchaseRecordItem> items = db.purchaseRecordDao().getItems(local.localId);
                List<Map<String, Object>> itemList = new ArrayList<>();
                for (LocalPurchaseRecordItem item : items) {
                    Map<String, Object> im = new HashMap<>();
                    im.put("productId", item.productId);
                    im.put("combinationId", item.combinationId);
                    im.put("quantity", item.quantity);
                    im.put("unitPrice", item.unitPrice);
                    itemList.add(im);
                }
                body.put("items", itemList);

                Response<com.heben.client.model.ApiResponse<com.heben.client.model.Models.PurchaseRecord>> resp =
                        ApiClient.getService().purchaseMyProducts(body).execute();

                if (resp.isSuccessful() && resp.body() != null && resp.body().isSuccess()) {
                    Long serverId = resp.body().getData() != null ? resp.body().getData().id : null;
                    if (serverId != null) {
                        db.purchaseRecordDao().markSynced(local.localId, serverId);
                        LocalIdMapping mapping = new LocalIdMapping();
                        mapping.entityType = "purchase_record";
                        mapping.localId = local.localId;
                        mapping.serverId = serverId;
                        mapping.syncedAt = System.currentTimeMillis();
                        db.idMappingDao().insert(mapping);
                    }
                } else {
                    if (resp.code() == 409) db.purchaseRecordDao().markConflict(local.localId);
                    allOk = false;
                }
            } catch (Exception e) {
                Log.e(TAG, "sync purchase failed: " + local.localId, e);
                allOk = false;
            }
        }
        return allOk;
    }

    /** 同步库存变更（基于增量，不覆盖绝对值） */
    private boolean syncStockChanges() {
        List<LocalStockChange> pending = db.stockChangeDao().getAllPending();
        if (pending.isEmpty()) return true;

        // 按combination_id聚合变更
        Map<Long, Integer> aggregated = new HashMap<>();
        Map<Long, String> opIds = new HashMap<>();
        for (LocalStockChange c : pending) {
            Integer existing = aggregated.get(c.combinationId);
            aggregated.put(c.combinationId, (existing != null ? existing : 0) + c.changeAmount);
            opIds.put(c.combinationId, c.operationId);
        }

        try {
            // 构建批量请求
            List<Map<String, Object>> changes = new ArrayList<>();
            for (Map.Entry<Long, Integer> entry : aggregated.entrySet()) {
                Map<String, Object> ch = new HashMap<>();
                ch.put("combinationId", entry.getKey());
                ch.put("change", entry.getValue());
                ch.put("operationId", opIds.get(entry.getKey()));
                changes.add(ch);
            }

            Map<String, Object> body = new HashMap<>();
            body.put("changes", changes);

            Response<com.heben.client.model.ApiResponse<Void>> resp =
                    ApiClient.getService().syncStockChanges(body).execute();

            if (resp.isSuccessful()) {
                // 标记所有已同步
                for (LocalStockChange c : pending) {
                    db.stockChangeDao().markSynced(c.id);
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "sync stock failed", e);
            return false;
        }
    }

    // ====== 离线写入辅助方法（供现有代码调用） ======

    public static String newOperationId() {
        return UUID.randomUUID().toString();
    }

    public static boolean isOnline(Context ctx) {
        return NetworkUtil.isConnected(ctx);
    }
}
