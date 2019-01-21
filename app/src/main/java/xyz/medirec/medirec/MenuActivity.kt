package xyz.medirec.medirec

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_menu.*
import org.apache.commons.lang3.RandomStringUtils
import xyz.medirec.medirec.pojo.MediRec
import xyz.medirec.medirec.pojo.SecretTime
import java.security.KeyPair
import java.security.SecureRandom
import java.util.*

class MenuActivity : AppCompatActivity() {
    companion object {
        private var keyPair: KeyPair? = null
        private var randomString: String? = null
        private var pinNumber: String? = null
        private var salt: String? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        askForPermission(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.NFC
        )

        if (intent.getStringExtra("FROM_ACTIVITY") == "MAIN") {
            Snackbar.make(logInSuccess, getString(R.string.logInSuccess), Snackbar.LENGTH_SHORT).show()
            pinNumber = intent.getStringExtra("pinNumber")
        }

        val mediRecSerialized = getSharedPreferences("UserData", MODE_PRIVATE).getString("MediRec", "")!!
        if(mediRecSerialized != "") {
            val mediRec = Helper.deserialize(mediRecSerialized) as MediRec
            keyPair = mediRec.keyPair
            randomString = mediRec.randomString
            salt = mediRec.salt
            updateKeyAndString()
        }

        val keyPairEncrypted = getSharedPreferences("UserData", MODE_PRIVATE).getString("keyPair", "")!!
        val randomStringEncrypted = getSharedPreferences("UserData", MODE_PRIVATE).getString("randomString", "")!!
        if(salt == null)
            salt = if(getSharedPreferences("UserData", MODE_PRIVATE).getString("salt", "")!! == "")
                generateAndSaveSalt() else getSharedPreferences("UserData", MODE_PRIVATE).getString("salt", "")!!

        val password = Helper.generateSecretKey(pinNumber!!.toCharArray(), salt!!.toByteArray())

        if(keyPair == null)
            keyPair = if(keyPairEncrypted == "") generateAndSaveKeyPair() else {
                Helper.deserialize(Helper.decrypt(Helper.decodeFromString(keyPairEncrypted), password)) as KeyPair
            }

        if(randomString == null)
            randomString = if (randomStringEncrypted == "") generateAndSaveRandomString() else {
                String(Helper.decrypt(Helper.decodeFromString(randomStringEncrypted), password))
            }

        val buttons = listOf(
            myQRcode, generateKey, exportID, importID, changePin, exit
        )

        buttons[0].setOnClickListener { goToQrCodeActivity() }
        buttons[1].setOnClickListener { generateKey() }
        buttons[2].setOnClickListener { exportID() }
        buttons[3].setOnClickListener { importID() }
        buttons[4].setOnClickListener { resetPIN() }
        buttons[5].setOnClickListener { exitApp() }
    }

    private fun askForPermission(vararg permissions: String) {
        val tempList = mutableListOf<String>()
        permissions.forEach {
            if (ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED)
                tempList.add(it)
        }
        if(tempList.isNotEmpty())
            ActivityCompat.requestPermissions(this, tempList.toTypedArray(), 1)
    }

    private fun goToQrCodeActivity() {
        val intent = Intent(this, SelectDatesActivity::class.java)
        intent.putExtra("keyPair", keyPair)
        intent.putExtra("randomString", randomString)
        startActivity(intent)
    }

    private fun generateKey() {
        val currentTimestamp = GregorianCalendar.getInstance().timeInMillis
        val secret = Helper.getAESKey(keyPair!!.private, currentTimestamp.toString(), randomString!!)
        val secretTime = SecretTime(currentTimestamp, secret.encoded)
        val intent = Intent(this, GenerateKeyActivity::class.java)
        intent.putExtra("secretTime", secretTime)
        intent.putExtra("privateKey", keyPair!!.private)
        startActivity(intent)
    }

    private fun importID() {
        val intent = Intent(this, DirChooserActivity::class.java)
        intent.putExtra("TYPE", "LOAD")
        startActivity(intent)
    }

    private fun exportID() {
        val intent = Intent(this, DirChooserActivity::class.java)
        intent.putExtra("keyPair", keyPair)
        intent.putExtra("randomString", randomString)
        intent.putExtra("TYPE", "SAVE")
        startActivity(intent)
    }

    private fun resetPIN() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder
            .setMessage(R.string.reallyChangePin)
            .setCancelable(false)
            .setPositiveButton("Yes") { _, _ ->
                val editor = getSharedPreferences("UserData", MODE_PRIVATE).edit()
                val timeSet = getSharedPreferences("UserData", MODE_PRIVATE).getStringSet("TimeSet", mutableSetOf())!!
                val mediRec = MediRec(timeSet, keyPair!!, randomString!!, salt!!)
                editor.putString("MediRec", Helper.encodeToString(Helper.serialize(mediRec)))
                editor.putString("loginPIN", null)
                editor.apply()
                startActivity(Intent(this, MainActivity::class.java))
            }
            .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.show()

        val myMsg = dialog.findViewById<TextView>(android.R.id.message)
        myMsg.gravity = Gravity.CENTER
        myMsg.textSize = 20f
    }

    private fun exitApp() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder
            .setMessage(R.string.reallyExit)
            .setCancelable(false)
            .setPositiveButton("Exit") { _, _ ->
                moveTaskToBack(true)
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(0)
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.show()

        val myMsg = dialog.findViewById<TextView>(android.R.id.message)
        myMsg.gravity = Gravity.CENTER
        myMsg.textSize = 20f
    }

    private fun updateKeyAndString() {
        val password = Helper.generateSecretKey(pinNumber!!.toCharArray(), salt!!.toByteArray())
        val stringEncrypted = Helper.encrypt(randomString!!.toByteArray(), password)
        val keyPairEncrypted = Helper.encrypt(Helper.serialize(keyPair!!), password)
        val editor = getSharedPreferences("UserData", Context.MODE_PRIVATE).edit()
        editor.putString("MediRec", "")
        editor.putString("randomString", Helper.encodeToString(stringEncrypted))
        editor.putString("keyPair", Helper.encodeToString(keyPairEncrypted))
        editor.apply()
    }

    private fun generateAndSaveSalt(): String {
        val salt = Helper.encodeToString(SecureRandom().generateSeed(64))
        val editor = getSharedPreferences("UserData", Context.MODE_PRIVATE).edit()
        editor.putString("salt", salt)
        editor.apply()
        return salt
    }

    private fun generateAndSaveRandomString(): String {
        val randomString = RandomStringUtils.random(128, true, true)
        val password = Helper.generateSecretKey(pinNumber!!.toCharArray(), salt!!.toByteArray())
        val stringEncrypted = Helper.encrypt(randomString.toByteArray(), password)
        val editor = getSharedPreferences("UserData", Context.MODE_PRIVATE).edit()
        editor.putString("randomString", Helper.encodeToString(stringEncrypted))
        editor.apply()
        return randomString
    }

    private fun generateAndSaveKeyPair(): KeyPair {
        val keyPair = Helper.generateKeyPair("EC")
        val password = Helper.generateSecretKey(pinNumber!!.toCharArray(), salt!!.toByteArray())
        val keyPairEncrypted = Helper.encrypt(Helper.serialize(keyPair), password)
        val editor = getSharedPreferences("UserData", Context.MODE_PRIVATE).edit()
        editor.putString("keyPair", Helper.encodeToString(keyPairEncrypted))
        editor.apply()
        return keyPair
    }
}
