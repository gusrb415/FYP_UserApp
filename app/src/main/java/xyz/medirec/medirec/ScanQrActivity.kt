package xyz.medirec.medirec

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import android.content.Intent
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.activity_scan_qr.*
import java.lang.IllegalArgumentException
import java.security.interfaces.ECPrivateKey


class ScanQrActivity : AppCompatActivity() {
    private var privateKey: ECPrivateKey? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_qr)
        startQRScanner()
        privateKey = intent.getSerializableExtra("privateKey") as ECPrivateKey
        backToMain_scan.setOnClickListener {
            goToMenu()
        }
        scanButton.setOnClickListener {
            startQRScanner()
        }
    }

    private fun startQRScanner() {
        val integrator = IntentIntegrator(this)
        integrator.setBeepEnabled(false)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Scan any MediRec QR Code")
        integrator.captureActivity = CapturePortrait::class.java
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
                goToMenu()
            } else {
                updateText(result.contents, privateKey!!)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    /**
     * Check
     * If the input is the hash of the record -> encrypt it with private key and return signature + AES key + timestamp
     * else raise pop up and re-scan
     *
     * @param scannedStr string format of QR code
     */
    private fun updateText(scannedStr: String, privateKey: ECPrivateKey) {
        try {
            //If it is hash ( check if it is hash or throw exception)
            if(scannedStr.length * 4 != 256 || scannedStr.contains(Regex("[^a-f0-9]+",RegexOption.IGNORE_CASE)))
                throw IllegalArgumentException("INVALID QR Code. Please re-scan")
            val signature = Helper.createECDSASignatureWithContent(privateKey, Helper.hexStringToByteArray(scannedStr))

            //GENERATE QR CODE
            Helper.drawQRCode(imageView, signature, windowManager)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            startQRScanner()
        }
    }

    override fun onBackPressed() {
        goToMenu()
    }

    private fun goToMenu() {
        startActivity(Intent(this, MenuActivity::class.java))
    }
}
