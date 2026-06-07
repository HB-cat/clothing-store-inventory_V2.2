package com.heben.client.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.heben.client.local.entity.LocalIdMapping;

@Dao
public interface IdMappingDao {

    @Query("SELECT server_id FROM local_id_mapping WHERE entity_type = :entityType AND local_id = :localId")
    Long getServerId(String entityType, long localId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(LocalIdMapping mapping);

    @Query("DELETE FROM local_id_mapping WHERE entity_type = :entityType")
    void deleteByType(String entityType);
}
