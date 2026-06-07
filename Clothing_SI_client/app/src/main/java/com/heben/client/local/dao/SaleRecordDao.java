package com.heben.client.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.heben.client.local.entity.LocalSaleRecord;
import com.heben.client.local.entity.LocalSaleRecordItem;

import java.util.List;

@Dao
public interface SaleRecordDao {

    @Query("SELECT * FROM local_sale_record WHERE user_id = :userId ORDER BY local_updated_at DESC")
    List<LocalSaleRecord> getAll(long userId);

    @Query("SELECT * FROM local_sale_record WHERE sync_status = 1 ORDER BY local_updated_at")
    List<LocalSaleRecord> getPending();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(LocalSaleRecord record);

    @Query("UPDATE local_sale_record SET serverId = :serverId, sync_status = 0 WHERE localId = :localId")
    void markSynced(long localId, long serverId);

    @Query("UPDATE local_sale_record SET sync_status = 2 WHERE localId = :localId")
    void markConflict(long localId);

    @Query("DELETE FROM local_sale_record WHERE localId = :localId")
    void delete(long localId);

    // ===== Items =====

    @Query("SELECT * FROM local_sale_record_item WHERE sale_local_id = :saleLocalId")
    List<LocalSaleRecordItem> getItems(long saleLocalId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertItems(List<LocalSaleRecordItem> items);

    @Query("DELETE FROM local_sale_record_item WHERE sale_local_id = :saleLocalId")
    void deleteItems(long saleLocalId);
}
