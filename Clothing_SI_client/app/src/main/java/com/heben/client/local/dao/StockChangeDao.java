package com.heben.client.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.heben.client.local.entity.LocalStockChange;

import java.util.List;

@Dao
public interface StockChangeDao {

    @Query("SELECT * FROM local_stock_change WHERE user_id = :userId AND sync_status = 1 ORDER BY created_at")
    List<LocalStockChange> getPending(long userId);

    @Query("SELECT * FROM local_stock_change WHERE sync_status = 1 ORDER BY created_at")
    List<LocalStockChange> getAllPending();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(LocalStockChange change);

    @Query("UPDATE local_stock_change SET sync_status = 0 WHERE id = :id")
    void markSynced(long id);

    @Query("DELETE FROM local_stock_change WHERE id = :id")
    void delete(long id);

    @Query("DELETE FROM local_stock_change WHERE user_id = :userId")
    void deleteAll(long userId);
}
