package dengr1065.madeinukraine

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Product(
    @PrimaryKey val ean: Long,
    val name: String,
    val shortName: String,
    val brand: String,
    val dateAdded: String
) {

    companion object {

        @ColorRes
        fun getStatusColor(product: Product?): Int {
            return when (product) {
                null -> R.color.product_status_missing
                else -> R.color.product_status_active
            }
        }

        @StringRes
        fun getStatusText(product: Product?): Int {
            return when (product) {
                null -> R.string.product_status_missing
                else -> R.string.product_status_active
            }
        }

        fun guess(row: List<String>): Product? {
            val ean = row.firstNotNullOfOrNull { it.toLongOrNull() } ?: return null
            return Product(ean, row[1], row[2], row[0], row[8])
        }
    }
}