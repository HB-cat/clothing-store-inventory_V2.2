package com.heben.client.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.heben.client.local.dao.AttributeGroupDao;
import com.heben.client.local.dao.CategoryDao;
import com.heben.client.local.dao.DraftBillDao;
import com.heben.client.local.dao.IdMappingDao;
import com.heben.client.local.dao.ProductDao;
import com.heben.client.local.dao.PurchaseRecordDao;
import com.heben.client.local.dao.SaleRecordDao;
import com.heben.client.local.dao.StockChangeDao;
import com.heben.client.local.entity.LocalAttributeGroup;
import com.heben.client.local.entity.LocalAttributeValue;
import com.heben.client.local.entity.LocalCategory;
import com.heben.client.local.entity.LocalDraftBill;
import com.heben.client.local.entity.LocalDraftBillItem;
import com.heben.client.local.entity.LocalIdMapping;
import com.heben.client.local.entity.LocalProduct;
import com.heben.client.local.entity.LocalProductCombination;
import com.heben.client.local.entity.LocalPurchaseRecord;
import com.heben.client.local.entity.LocalPurchaseRecordItem;
import com.heben.client.local.entity.LocalSaleRecord;
import com.heben.client.local.entity.LocalSaleRecordItem;
import com.heben.client.local.entity.LocalStockChange;

@Database(
    entities = {
        // 已有缓存
        LocalCategory.class,
        LocalProduct.class,
        LocalProductCombination.class,
        LocalAttributeGroup.class,
        LocalAttributeValue.class,
        // 新增离线写入
        LocalDraftBill.class,
        LocalDraftBillItem.class,
        LocalSaleRecord.class,
        LocalSaleRecordItem.class,
        LocalPurchaseRecord.class,
        LocalPurchaseRecordItem.class,
        LocalStockChange.class,
        LocalIdMapping.class
    },
    version = 2,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract CategoryDao categoryDao();
    public abstract ProductDao productDao();
    public abstract AttributeGroupDao attributeGroupDao();
    public abstract DraftBillDao draftBillDao();
    public abstract SaleRecordDao saleRecordDao();
    public abstract PurchaseRecordDao purchaseRecordDao();
    public abstract StockChangeDao stockChangeDao();
    public abstract IdMappingDao idMappingDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "clothing_si_cache.db"
                    )
                    .fallbackToDestructiveMigration()
                    .addCallback(new Callback() {
                        @Override
                        public void onDestructiveMigration(@NonNull SupportSQLiteDatabase db) {
                            // 数据库损坏或迁移时，重建
                        }
                    })
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
