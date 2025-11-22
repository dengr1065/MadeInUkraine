package dengr1065.madeinukraine

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("DELETE FROM Product")
    fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(product: Product)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(products: List<Product>)

    @Query("SELECT COUNT(ean) FROM Product")
    fun getCount(): Int

    @Query("SELECT COUNT(ean) FROM Product")
    fun getLiveCount(): Flow<Int>

    @Query("SELECT * FROM Product WHERE ean = :ean")
    fun getByEan(ean: Long): Product?

    @Query("SELECT * FROM Product WHERE name LIKE '%' || :keyword || '%' OR brand LIKE '%' || :keyword || '%'")
    fun filter(keyword: String): Product?
}