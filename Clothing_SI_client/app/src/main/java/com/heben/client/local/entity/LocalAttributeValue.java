package com.heben.client.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "local_attribute_value")
public class LocalAttributeValue {
    @PrimaryKey
    public long id;

    @ColumnInfo(name = "group_id")
    public long groupId;

    public String value;

    @ColumnInfo(name = "sort_order")
    public int sortOrder;
}
