package com.heben.client.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "local_id_mapping")
public class LocalIdMapping {
    @PrimaryKey(autoGenerate = true)
    public long id;

    /** 实体类型，如 "sale_record", "draft_bill", "purchase_record" */
    @ColumnInfo(name = "entity_type")
    public String entityType;

    /** 本地的临时ID (localId from other local tables) */
    @ColumnInfo(name = "local_id")
    public long localId;

    /** 服务端的真实ID */
    @ColumnInfo(name = "server_id")
    public long serverId;

    @ColumnInfo(name = "synced_at")
    public long syncedAt;
}
