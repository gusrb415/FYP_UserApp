package xyz.medirec.medirec

import android.content.Intent
import android.nfc.NdefMessage
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_view_qr.*
import xyz.medirec.medirec.pojo.KeyTime
import java.security.KeyPair
import android.widget.Toast
import android.nfc.NfcAdapter
import android.nfc.NfcEvent
import android.nfc.NdefRecord




class ViewQrActivity : AppCompatActivity(), NfcAdapter.CreateNdefMessageCallback {
    var message = ""

    override fun createNdefMessage(event: NfcEvent?): NdefMessage {
        val ndefRecord = NdefRecord.createMime("text/vnd.medirec.keytime", message.toByteArray())
        return NdefMessage(ndefRecord)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_qr)
        val keyPair = intent.getSerializableExtra("keyPair") as KeyPair
        val timeList = intent.getLongArrayExtra("timeList")
        val randomStr = intent.getStringExtra("randomString")
        createQRcode(keyPair, timeList, randomStr)

        val mAdapter = NfcAdapter.getDefaultAdapter(this)

        var toastText = "Please scan QR code or tap NFC reader."
        if (mAdapter == null) {
            toastText = "This device does not have NFC function.\nPlease use QR code."
        } else if (!mAdapter.isEnabled) {
            toastText = "Please enable NFC or use QR code."
        } else {
            mAdapter.setNdefPushMessageCallback(null, this)
        }
        Toast.makeText(this, toastText, Toast.LENGTH_LONG).show()

        GoToMenuButton.setOnClickListener {
            goToMenu()
        }
    }


    override fun onBackPressed() {
        goToMenu()
    }

    private fun createQRcode(keyPair: KeyPair, timeList: LongArray, randomString: String) {
        val secretKeyList = mutableListOf<ByteArray>()
        timeList.forEach { secretKeyList.add(Helper.getAESKey(keyPair.private, it.toString(), randomString).encoded) }
        val serialized = Helper.serialize(KeyTime(keyPair.public.encoded, secretKeyList.toTypedArray(), timeList))
        message = Helper.encodeToString(serialized)
        Helper.drawQRCode(qrCodeView, message, windowManager)
    }

    private fun goToMenu() {
        startActivity(Intent(this, MenuActivity::class.java))
    }
}
