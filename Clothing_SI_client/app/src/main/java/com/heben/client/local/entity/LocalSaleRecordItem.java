package com.heben.client.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "local_sale_record_item")
public class LocalSaleRecordItem {
    @PrimaryKey(autoGenerate = true)
    public long localId;

    public Long serverId;

    @ColumnInfo(name = "sale_local_id")
    public long saleLocalId;

    public long productId;
    public String productName;
    public Long combinationId;
    public String attrsJson;
    public int quantity;
    public double unitPrice;
    public double purchasePrice;

    @ColumnInfo(name = "sync_status")
    public int syncStatus;
}
