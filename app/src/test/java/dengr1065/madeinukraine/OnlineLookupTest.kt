package dengr1065.madeinukraine

import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Test

class OnlineLookupTest {

    private val httpClient = OkHttpClient.Builder().build()
    private val service = OnlineService(httpClient)

    @Test
    fun onlineLookupWorks() {
        val waterEan = 4820051240035L
        val result: Product? = service.lookupByBarcode(waterEan)

        assertNotNull("Lookup did not return anything", result!!)
        assertEquals("Result EAN didn't match input", waterEan, result.ean)
        assertTrue(
            "Returned product name was not correct",
            result.name.contains("карпатська джерельна", ignoreCase = true)
        )
    }
}