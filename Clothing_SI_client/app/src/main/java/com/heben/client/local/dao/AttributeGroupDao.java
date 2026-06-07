package com.heben.client.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.heben.client.local.entity.LocalAttributeGroup;
import com.heben.client.local.entity.LocalAttributeValue;

import java.util.List;

@Dao
public interface AttributeGroupDao {
    @Query("SELECT * FROM local_attribute_group WHERE user_id = :userId ORDER BY id")
    List<LocalAttributeGroup> getAll(long userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<LocalAttributeGroup> groups);

    @Query("DELETE FROM local_attribute_group WHERE user_id = :userId")
    void deleteAll(long userId);

    @Query("SELECT COUNT(*) FROM local_attribute_group WHERE user_id = :userId")
    int count(long userId);

    @Query("SELECT * FROM local_attribute_value WHERE group_id IN " +
           "(SELECT id FROM local_attribute_group WHERE user_id = :userId) ORDER BY sort_order")
    List<LocalAttributeValue> getAllValues(long userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllValues(List<LocalAttributeValue> values);

    @Query("DELETE FROM local_attribute_value")
    void deleteAllValues();
}
