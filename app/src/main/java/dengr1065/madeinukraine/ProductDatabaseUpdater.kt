package dengr1065.madeinukraine

import android.util.Log
import com.github.doyaaaaaken.kotlincsv.dsl.context.InsufficientFieldsRowBehaviour
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import java.io.InputStream

class ProductDatabaseUpdater(private val dao: ProductDao) {

    private var rowsInserted = 0
    private val reader = csvReader {
        escapeChar = '"'
        quoteChar = '"'
        charset = "UTF-8"
        delimiter = ';'
        insufficientFieldsRowBehaviour = InsufficientFieldsRowBehaviour.EMPTY_STRING
    }

    fun process(stream: InputStream) {
        rowsInserted = 0
        reader.open(stream) {
            readAllAsSequence(9)
                .mapNotNull(::parseRow)
                .windowed(BUFFER_SIZE, BUFFER_SIZE, true)
                .forEach(dao::insertAll)
        }
    }

    private fun parseRow(row: List<String>): Product? {
        return row[0].toLongOrNull()?.let { ean ->
            Product(ean, row[2], row[3], row[1], row[8])
        } ?: Product.guess(row)
    }

    private fun insertRows(rows: List<Product>) {
        dao.insertAll(rows)
        rowsInserted += rows.size
        Log.d(TAG, "insertRows: $rowsInserted done")
    }

    companion object {
        private const val TAG = "ProductDatabaseUpdater"
        const val BUFFER_SIZE = 1000
    }
}