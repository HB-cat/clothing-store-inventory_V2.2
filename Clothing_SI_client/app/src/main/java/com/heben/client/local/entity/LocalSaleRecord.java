package com.heben.client.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "local_sale_record")
public class LocalSaleRecord {
    @PrimaryKey(autoGenerate = true)
    public long localId;

    /** 服务端id，同步后填充 */
    public Long serverId;

    @ColumnInfo(name = "user_id")
    public long userId;

    public Long buyerId;
    public String buyerName;
    public double totalAmount;
    public int itemCount;
    public Long draftBillId;

    @ColumnInfo(name = "sale_time")
    public String saleTime;

    @ColumnInfo(name = "local_updated_at")
    public long localUpdatedAt;

    @ColumnInfo(name = "operation_id")
    public String operationId;

    @ColumnInfo(name = "sync_status")
    public int syncStatus; // 0=SYNCED, 1=PENDING, 2=CONFLICT
}
