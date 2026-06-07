package com.heben.client.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "local_product")
public class LocalProduct {
    @PrimaryKey
    public long id;

    @ColumnInfo(name = "user_id")
    public long userId;

    public String name;

    @ColumnInfo(name = "category_id")
    public long categoryId;

    @ColumnInfo(name = "purchase_price")
    public double purchasePrice;

    @ColumnInfo(name = "selling_price")
    public double sellingPrice;

    @ColumnInfo(name = "image_url")
    public String imageUrl;

    @ColumnInfo(name = "has_attributes")
    public boolean hasAttributes;

    public int status;
}
