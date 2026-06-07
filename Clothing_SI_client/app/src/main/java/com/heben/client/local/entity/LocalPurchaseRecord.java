package com.heben.client.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "local_purchase_record")
public class LocalPurchaseRecord {
    @PrimaryKey(autoGenerate = true)
    public long localId;

    public Long serverId;

    @ColumnInfo(name = "user_id")
    public long userId;

    public int source;
    public double totalAmount;
    public String supplierName;

    @ColumnInfo(name = "local_updated_at")
    public long localUpdatedAt;

    @ColumnInfo(name = "operation_id")
    public String operationId;

    @ColumnInfo(name = "sync_status")
    public int syncStatus;
}
