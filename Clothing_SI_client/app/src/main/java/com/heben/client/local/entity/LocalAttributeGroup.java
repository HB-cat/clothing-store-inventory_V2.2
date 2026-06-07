package com.heben.client.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "local_attribute_group")
public class LocalAttributeGroup {
    @PrimaryKey
    public long id;

    @ColumnInfo(name = "user_id")
    public long userId;

    public String name;

    @ColumnInfo(name = "is_preset")
    public boolean isPreset;
}
