package com.heben.client.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "local_category")
public class LocalCategory {
    @PrimaryKey
    public long id;

    @ColumnInfo(name = "user_id")
    public long userId;

    public String name;

    @ColumnInfo(name = "sort_order")
    public int sortOrder;

    @ColumnInfo(name = "is_preset")
    public boolean isPreset;
}
