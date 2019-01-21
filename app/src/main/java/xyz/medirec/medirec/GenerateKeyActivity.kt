package xyz.medirec.medirec

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_generate_key.*
import xyz.medirec.medirec.pojo.SecretTime

class GenerateKeyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_key)

        val secretTime = intent.getSerializableExtra("secretTime") as SecretTime

        createQRcode(secretTime)
        scanButton.setOnClickListener {
            saveAndViewQR(secretTime.timestamp)
        }
        discardButton.setOnClickListener {
            discard()
        }
    }

    private fun createQRcode(secretTime: SecretTime) {
        Helper.drawQRCode(qrCodeView, Helper.serialize(secretTime), windowManager)
    }

    private fun discard() {
        // DO NOT SAVE AND BACK TO MENU
        startActivity(Intent(this, MenuActivity::class.java))
    }

    private fun saveAndViewQR(currentTimestamp: Long) {
        // SAVE AND BACK TO MENU
        // ADD THE TIMESTAMP TO SET
        val set = getSharedPreferences("UserData", Context.MODE_PRIVATE).getStringSet("TimeSet", mutableSetOf())!!
        val newSet = set.toMutableSet()
        newSet.add(currentTimestamp.toString())
        val editor = getSharedPreferences("UserData", MODE_PRIVATE).edit()
        editor.putStringSet("TimeSet", newSet)
        editor.apply()
        val intent = Intent(this, ScanQrActivity::class.java)
        intent.putExtra("privateKey", this.intent.getSerializableExtra("privateKey"))
        startActivity(intent)
    }
}
