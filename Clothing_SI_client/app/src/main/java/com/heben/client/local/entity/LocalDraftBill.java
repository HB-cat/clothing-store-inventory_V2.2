package com.heben.client.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "local_draft_bill")
public class LocalDraftBill {
    @PrimaryKey(autoGenerate = true)
    public long localId;

    public Long serverId;

    @ColumnInfo(name = "user_id")
    public long userId;

    public int billIndex;
    public double totalAmount;
    public int itemCount;
    public int status; // 0=草稿, 1=已提交

    @ColumnInfo(name = "local_updated_at")
    public long localUpdatedAt;

    @ColumnInfo(name = "operation_id")
    public String operationId;

    @ColumnInfo(name = "sync_status")
    public int syncStatus;
}
