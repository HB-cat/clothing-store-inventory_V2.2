package com.heben.client.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.heben.client.local.entity.LocalPurchaseRecord;
import com.heben.client.local.entity.LocalPurchaseRecordItem;

import java.util.List;

@Dao
public interface PurchaseRecordDao {

    @Query("SELECT * FROM local_purchase_record WHERE user_id = :userId ORDER BY local_updated_at DESC")
    List<LocalPurchaseRecord> getAll(long userId);

    @Query("SELECT * FROM local_purchase_record WHERE sync_status = 1 ORDER BY local_updated_at")
    List<LocalPurchaseRecord> getPending();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(LocalPurchaseRecord record);

    @Query("UPDATE local_purchase_record SET serverId = :serverId, sync_status = 0 WHERE localId = :localId")
    void markSynced(long localId, long serverId);

    @Query("UPDATE local_purchase_record SET sync_status = 2 WHERE localId = :localId")
    void markConflict(long localId);

    @Query("DELETE FROM local_purchase_record WHERE localId = :localId")
    void delete(long localId);

    // ===== Items =====

    @Query("SELECT * FROM local_purchase_record_item WHERE purchase_local_id = :purchaseLocalId")
    List<LocalPurchaseRecordItem> getItems(long purchaseLocalId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertItems(List<LocalPurchaseRecordItem> items);

    @Query("DELETE FROM local_purchase_record_item WHERE purchase_local_id = :purchaseLocalId")
    void deleteItems(long purchaseLocalId);
}
