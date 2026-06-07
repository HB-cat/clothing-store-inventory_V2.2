package com.heben.client.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.heben.client.local.entity.LocalCategory;

import java.util.List;

@Dao
public interface CategoryDao {
    @Query("SELECT * FROM local_category WHERE user_id = :userId ORDER BY sort_order")
    List<LocalCategory> getAll(long userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<LocalCategory> categories);

    @Query("DELETE FROM local_category WHERE user_id = :userId")
    void deleteAll(long userId);

    @Query("SELECT COUNT(*) FROM local_category WHERE user_id = :userId")
    int count(long userId);
}
