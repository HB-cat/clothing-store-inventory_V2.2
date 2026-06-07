package com.heben.client.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "local_stock_change")
public class LocalStockChange {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "combination_id")
    public long combinationId;

    /** 正数=入库，负数=出库 */
    @ColumnInfo(name = "change_amount")
    public int changeAmount;

    @ColumnInfo(name = "operation_id")
    public String operationId;

    @ColumnInfo(name = "sync_status")
    public int syncStatus; // 0=SYNCED, 1=PENDING

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "user_id")
    public long userId;
}
