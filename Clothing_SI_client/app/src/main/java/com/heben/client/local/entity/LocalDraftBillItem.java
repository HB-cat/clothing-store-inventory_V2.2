package com.heben.client.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "local_draft_bill_item")
public class LocalDraftBillItem {
    @PrimaryKey(autoGenerate = true)
    public long localId;

    public Long serverId;

    @ColumnInfo(name = "bill_local_id")
    public long billLocalId;

    public long productId;
    public Long combinationId;
    public String attrsJson;
    public int quantity;
    public double unitPrice;

    @ColumnInfo(name = "sync_status")
    public int syncStatus;
}
