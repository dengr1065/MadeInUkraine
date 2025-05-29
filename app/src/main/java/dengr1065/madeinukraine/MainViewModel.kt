package dengr1065.madeinukraine

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.headersContentLength
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.InputStream

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _product = MutableLiveData<Product?>()
    val product: LiveData<Product?> = _product

    private val db = Room.databaseBuilder(application, AppDatabase::class.java, "products").build()
    private val productDao = db.productDao()

    private val httpClient = OkHttpClient.Builder().build()
    private var isUpdateInProgress = false

    fun checkEan(ean: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = productDao.getByEan(ean) ?: checkOnline(ean)
            _product.postValue(result)
        }
    }

    fun downloadDatabase(): Int {
        if (isUpdateInProgress) {
            return productDao.getCount()
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                isUpdateInProgress = true
                val response = Request.Builder()
                    .url(PRODUCTS_CSV_URL)
                    .build().let { httpClient.newCall(it).execute() }

                if (response.code >= 400) {
                    Log.e(TAG, "downloadDatabase: error code ${response.code}")
                }

                updateDatabase(response.body!!.byteStream())
            } finally {
                isUpdateInProgress = false
            }
        }

        return -1
    }

    private fun checkOnline(ean: Long): Product? {
        return try {
            val response = Request.Builder()
                .url(BARCODE_API_URL + ean.toString())
                .build()
                .let { httpClient.newCall(it).execute() }

            if (response.code >= 400) {
                Log.e(TAG, "checkOnline: request failed with code ${response.code}")
                return null
            }

            val data = response.body?.string()?.let {
                val obj = JSONObject(it).getJSONObject("data")
                if (obj.getInt("total") == 0) {
                    return@let null
                }

                obj.getJSONArray("data").getJSONObject(0)
            } ?: return null

            val productName = data.getString("name")
            val brand = data.getJSONObject("brand").getString("name")

            Product(ean, productName, productName, brand, "API")
        } catch (e: Exception) {
            Log.e(TAG, "checkOnline: exception while checking", e)
            null
        }
    }

    private fun updateDatabase(stream: InputStream) {
        val updater = ProductDatabaseUpdater(productDao)

        Log.i(TAG, "updateDatabase: read started")

        productDao.clear()
        updater.process(stream)

        Log.w(TAG, "updateDatabase: done")
    }

    companion object {
        const val TAG = "MainViewModel"
        const val PRODUCTS_CSV_URL =
            "https://api.madeinukraine.gov.ua/storage/exports/products.csv"
        const val BARCODE_API_URL = "https://api.madeinukraine.gov.ua/api/products?barcode="
    }
}