package com.heben.client.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.heben.client.local.entity.LocalDraftBill;
import com.heben.client.local.entity.LocalDraftBillItem;

import java.util.List;

@Dao
public interface DraftBillDao {

    @Query("SELECT * FROM local_draft_bill WHERE user_id = :userId ORDER BY local_updated_at DESC")
    List<LocalDraftBill> getAll(long userId);

    @Query("SELECT * FROM local_draft_bill WHERE sync_status = 1 ORDER BY local_updated_at")
    List<LocalDraftBill> getPending();

    @Query("SELECT * FROM local_draft_bill WHERE localId = :localId")
    LocalDraftBill getByLocalId(long localId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(LocalDraftBill bill);

    @Query("UPDATE local_draft_bill SET serverId = :serverId, sync_status = 0 WHERE localId = :localId")
    void markSynced(long localId, long serverId);

    @Query("UPDATE local_draft_bill SET sync_status = 2 WHERE localId = :localId")
    void markConflict(long localId);

    @Query("DELETE FROM local_draft_bill WHERE localId = :localId")
    void delete(long localId);

    // ===== Items =====

    @Query("SELECT * FROM local_draft_bill_item WHERE bill_local_id = :billLocalId")
    List<LocalDraftBillItem> getItems(long billLocalId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertItems(List<LocalDraftBillItem> items);

    @Query("DELETE FROM local_draft_bill_item WHERE bill_local_id = :billLocalId")
    void deleteItems(long billLocalId);
}
