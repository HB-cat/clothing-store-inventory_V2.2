package com.heben.client.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "local_product_combination")
public class LocalProductCombination {
    @PrimaryKey
    public long id;

    @ColumnInfo(name = "product_id")
    public long productId;

    @ColumnInfo(name = "attrs_json")
    public String attrsJson;

    public int stock;

    public int version;
}
