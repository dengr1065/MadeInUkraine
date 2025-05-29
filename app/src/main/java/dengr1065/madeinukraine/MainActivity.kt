package dengr1065.madeinukraine

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallClient
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import dengr1065.madeinukraine.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val viewModel by viewModels<MainViewModel>()

    private lateinit var binding: ActivityMainBinding
    private lateinit var moduleInstallClient: ModuleInstallClient
    private lateinit var scanner: GmsBarcodeScanner

    private val scannerLauncher: ActivityResultLauncher<ScanOptions> =
        registerForActivityResult(ScanContract()) { result ->
            result.contents?.toLongOrNull()?.let {
                viewModel.checkEan(it)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        moduleInstallClient = ModuleInstall.getClient(this)
        scanner = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_EAN_13)
            .build()
            .let { GmsBarcodeScanning.getClient(this@MainActivity, it) }

        binding.buttonScan.setOnClickListener {
            checkAndOpenGmsScanner()
        }

        binding.buttonScanOld.setOnClickListener {
            openScanner()
        }

        viewModel.product.observe(this) {
            showProductData(it)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuUpdateDatabase -> {
                val result = viewModel.downloadDatabase()
                val message = if (result < 0) {
                    getString(R.string.message_update_enqueued)
                } else {
                    getString(R.string.message_update_in_progress, result)
                }

                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }

            R.id.menuOpenCalculator -> openCalculator()
            else -> return false
        }

        return true
    }

    private fun checkAndOpenGmsScanner() {
        val moduleInstallRequest = ModuleInstallRequest.newBuilder()
            .addApi(scanner)
            .setListener { update ->
                update.progressInfo?.let {
                    val progress = (it.bytesDownloaded * 100 / it.totalBytesToDownload).toInt()
                    binding.textName.text = progress.toString()
                }
            }
            .build()

        moduleInstallClient
            .installModules(moduleInstallRequest)
            .addOnSuccessListener { openGmsScanner() }
            .addOnFailureListener { openScanner() }
    }

    private fun openScanner() {
        ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.EAN_13)
            setBeepEnabled(true)
            setBarcodeImageEnabled(false)
        }.let { scannerLauncher.launch(it) }
    }

    private fun openGmsScanner() {
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val ean = barcode.rawValue!!.toLong(10)
                viewModel.checkEan(ean)
            }
            .addOnFailureListener {
                // Task failed with an exception
                Toast.makeText(this, R.string.scan_error, Toast.LENGTH_SHORT).show()
                Toast.makeText(this, it.localizedMessage, Toast.LENGTH_LONG).show()
            }
    }

    private fun showProductData(product: Product?) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        vibrator.vibrate(VibrationEffect.createOneShot(500, 200))

        binding.productStatus.text = getString(Product.getStatusText(product))
        binding.productStatus.setBackgroundColor(getColor(Product.getStatusColor(product)))

        if (product == null) {
            binding.textName.text = ""
            binding.textShortName.text = ""
            binding.textBrand.text = ""
            binding.textDateAdded.text = ""
            return
        }

        binding.textName.text = getString(R.string.prop_name, product.name)
        binding.textShortName.text = getString(R.string.prop_short_name, product.shortName)
        binding.textBrand.text = getString(R.string.prop_brand, product.brand)
        binding.textDateAdded.text = getString(R.string.prop_date_added, product.dateAdded)
    }

    private fun openCalculator() {
        val genericIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR)
        if (tryStartActivity(genericIntent)) return

        val samsungIntent = packageManager.getLaunchIntentForPackage("com.sec.android.app.popupcalculator")
        if (samsungIntent?.let { tryStartActivity(it) } == true) return

        Toast.makeText(this, R.string.open_calculator_error, Toast.LENGTH_SHORT).show()
    }

    private fun tryStartActivity(intent: Intent): Boolean {
        return try {
            startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}