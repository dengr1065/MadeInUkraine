package dengr1065.madeinukraine

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject

class OnlineService(private val httpClient: OkHttpClient) {

    fun lookupByBarcode(barcode: Long): Product? {
        val response = getResponseString(BARCODE_API_URL + barcode)
        val json = JSONObject(response)

        if (json.getString("status") != "success") {
            throw IllegalStateException("API reported an error status: $response")
        }

        json.getJSONObject("data").let {
            return if (it.getInt("total") == 0) {
                null
            } else {
                jsonToProduct(it.getJSONArray("data").getJSONObject(0))
            }
        }
    }

    private fun getResponseString(url: String): String {
        val response = Request.Builder()
            .url(url)
            .build()
            .let { httpClient.newCall(it).execute() }

        if (!response.isSuccessful) {
            val code = response.code
            throw RuntimeException("Error response code for HTTP request of $url: $code")
        }

        return requireNotNull(response.body).string()
    }

    private fun jsonToProduct(data: JSONObject): Product {
        val ean = data.getLong("barCode")
        val name = data.getString("name")
        val brand = data.getJSONObject("brand").getString("name")

        return Product(ean, name, name, brand, "API")
    }

    companion object {
        const val BARCODE_API_URL = "https://api.madeinukraine.gov.ua/api/products?barcode="
    }

}