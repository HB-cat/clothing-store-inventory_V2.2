package com.heben.client.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.heben.client.local.entity.LocalProduct;
import com.heben.client.local.entity.LocalProductCombination;

import java.util.List;

@Dao
public interface ProductDao {
    @Query("SELECT * FROM local_product WHERE user_id = :userId ORDER BY name")
    List<LocalProduct> getAll(long userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<LocalProduct> products);

    @Query("DELETE FROM local_product WHERE user_id = :userId")
    void deleteAll(long userId);

    @Query("SELECT COUNT(*) FROM local_product WHERE user_id = :userId")
    int count(long userId);

    @Query("SELECT * FROM local_product_combination")
    List<LocalProductCombination> getAllCombinations();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllCombinations(List<LocalProductCombination> combinations);

    @Query("DELETE FROM local_product_combination")
    void deleteAllCombinations();
}
