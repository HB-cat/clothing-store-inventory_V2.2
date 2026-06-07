package com.heben.client.local.sync;

import android.content.Context;

import com.heben.client.local.AppDatabase;
import com.heben.client.local.entity.LocalPurchaseRecord;
import com.heben.client.local.entity.LocalPurchaseRecordItem;
import com.heben.client.local.entity.LocalSaleRecord;
import com.heben.client.local.entity.LocalSaleRecordItem;
import com.heben.client.local.entity.LocalStockChange;

import java.util.List;
import java.util.UUID;

/**
 * 离线辅助工具
 * 供现有 Activity/SaleActivity 等调用：
 *   如果在线 → 直接调 API
 *   如果离线 → 写入 Room（sync_status=1），网络恢复后自动同步
 */
public class OfflineHelper {

    private final AppDatabase db;
    private final Context context;

    public OfflineHelper(Context context) {
        this.context = context.getApplicationContext();
        this.db = AppDatabase.getInstance(this.context);
    }

    /** 是否在线 */
    public boolean isOnline() {
        return NetworkUtil.isConnected(context);
    }

    /** 生成全局唯一操作ID（幂等用） */
    public static String opId() {
        return UUID.randomUUID().toString();
    }

    // ========== 记账（快捷/明细） ==========

    /** 离线保存销售记录，返回本地localId */
    public long saveSaleOffline(long userId, String buyerName, double totalAmount,
                                int itemCount, String saleTime,
                                List<LocalSaleRecordItem> items) {
        LocalSaleRecord record = new LocalSaleRecord();
        record.userId = userId;
        record.buyerName = buyerName;
        record.totalAmount = totalAmount;
        record.itemCount = itemCount;
        record.saleTime = saleTime;
        record.localUpdatedAt = System.currentTimeMillis();
        record.operationId = opId();
        record.syncStatus = 1; // PENDING

        long localId = db.saleRecordDao().insert(record);

        for (LocalSaleRecordItem item : items) {
            item.saleLocalId = localId;
            item.syncStatus = 1;
        }
        db.saleRecordDao().insertItems(items);

        return localId;
    }

    // ========== 进货 ==========

    /** 离线保存进货记录，返回本地localId */
    public long savePurchaseOffline(long userId, int source, double totalAmount,
                                    String supplierName,
                                    List<LocalPurchaseRecordItem> items) {
        LocalPurchaseRecord record = new LocalPurchaseRecord();
        record.userId = userId;
        record.source = source;
        record.totalAmount = totalAmount;
        record.supplierName = supplierName;
        record.localUpdatedAt = System.currentTimeMillis();
        record.operationId = opId();
        record.syncStatus = 1; // PENDING

        long localId = db.purchaseRecordDao().insert(record);

        for (LocalPurchaseRecordItem item : items) {
            item.purchaseLocalId = localId;
            item.syncStatus = 1;
        }
        db.purchaseRecordDao().insertItems(items);

        return localId;
    }

    // ========== 库存变更 ==========

    /** 离线记录库存变更（增量，非绝对值） */
    public void saveStockChangeOffline(long userId, long combinationId, int changeAmount) {
        LocalStockChange change = new LocalStockChange();
        change.userId = userId;
        change.combinationId = combinationId;
        change.changeAmount = changeAmount;
        change.operationId = opId();
        change.syncStatus = 1; // PENDING
        change.createdAt = System.currentTimeMillis();
        db.stockChangeDao().insert(change);
    }
}
