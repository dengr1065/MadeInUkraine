package dengr1065.madeinukraine

import android.app.Application
import android.util.Log
import android.widget.Toast
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
import java.time.Duration

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(application, AppDatabase::class.java, "products").build()
    private val productDao = db.productDao()

    private val _product = MutableLiveData<Product?>()
    val product: LiveData<Product?> = _product

    val databaseCount = productDao.getLiveCount()

    private val httpClient = OkHttpClient.Builder()
        .readTimeout(Duration.ofMinutes(1))
        .build()
    private val service = OnlineService(httpClient)

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
            return service.lookupByBarcode(ean)
        } catch (e: Exception) {
            val context = getApplication<Application>()
            Toast.makeText(context, R.string.online_lookup_error, Toast.LENGTH_SHORT).show()

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
    }
}