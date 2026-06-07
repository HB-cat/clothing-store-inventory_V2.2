package com.heben.client.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "local_purchase_record_item")
public class LocalPurchaseRecordItem {
    @PrimaryKey(autoGenerate = true)
    public long localId;

    public Long serverId;

    @ColumnInfo(name = "purchase_local_id")
    public long purchaseLocalId;

    public long productId;
    public Long combinationId;
    public int quantity;
    public double unitPrice;

    @ColumnInfo(name = "sync_status")
    public int syncStatus;
}
