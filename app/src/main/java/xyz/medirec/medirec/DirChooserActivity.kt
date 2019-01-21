package xyz.medirec.medirec

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.graphics.Point
import android.support.v4.provider.DocumentFile
import android.widget.Toast
import xyz.medirec.medirec.pojo.MediRec
import java.security.KeyPair
import java.util.*
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.util.DisplayMetrics
import android.widget.EditText
import java.lang.Exception


class DirChooserActivity : AppCompatActivity() {
    private var mode = ""
    private val code = 2019
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dir_chooser)
        mode = if(intent.getStringExtra("TYPE") == "SAVE") "SAVE" else "LOAD"
        loadChooser()
    }

    private fun loadChooser() {
        val i = if(mode == "SAVE") Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
        } else Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        startActivityForResult(Intent.createChooser(i, "Select" + if(mode == "SAVE") " Directory" else " MediRec File"), code)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(data != null) {
            when (requestCode) {
                code -> {
                    val builderText = EditText(this)
                    builderText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    builderText.setPaddingRelative(50, 30, 50, 30)

                    val builder = AlertDialog.Builder(this)
                    builder.setView(builderText)
                    builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

                    if (mode == "SAVE") {
                        builder.setTitle("Input PIN (It will be used for encryption)")
                        builder.setPositiveButton("OK") { _, _ ->
                            val directory = DocumentFile.fromTreeUri(this, data.data!!)!!
                            val set = getSharedPreferences("UserData", Context.MODE_PRIVATE).getStringSet("TimeSet", mutableSetOf())!!
                            val keyPair = intent.getSerializableExtra("keyPair") as KeyPair
                            val randomString = intent.getStringExtra("randomString")
                            val salt = getSharedPreferences("UserData", Context.MODE_PRIVATE).getString("salt", "")!!
                            val timestamp = (GregorianCalendar.getInstance().timeInMillis / 1000).toString()

                            val password = Helper.generateSecretKey(builderText.text.toString().toCharArray(), timestamp.toByteArray())
                            val fileContents = Helper.encrypt(Helper.serialize(MediRec(set, keyPair, randomString, salt)), password)
                            val newFile = directory.createFile("*/*", "$timestamp.medirec")
                            val out = contentResolver.openOutputStream(newFile!!.uri)!!
                            out.write(fileContents)
                            out.close()
                            Toast.makeText(this, "File successfully exported as $timestamp.medirec", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MenuActivity::class.java))
                        }
                    } else {
                        val temp = data.data!!.path!!.split("/")
                        val filename = temp[temp.size - 1].split(".")
                        if(filename[1] == "medirec") {
                            builder.setTitle("Input PIN (It will be used for decryption)")
                            builder.setPositiveButton("OK") { _, _ ->
                                val input = contentResolver.openInputStream(data.data!!)!!
                                val password = Helper.generateSecretKey(
                                    builderText.text.toString().toCharArray(),
                                    filename[0].toByteArray()
                                )
                                try {
                                    val mediRec =
                                        Helper.deserialize(Helper.decrypt(input.readBytes(), password)) as MediRec
                                    input.close()
//                                    val pinNumber = intent.getStringExtra("pinNumber")
//                                    val newPassword = Helper.generateSecretKey(pinNumber.toCharArray(), mediRec.salt.toByteArray())
                                    val editor = getSharedPreferences("UserData", Context.MODE_PRIVATE).edit()
//                                    editor.putString(
//                                        "keyPair",
//                                        Helper.encodeToString(
//                                            Helper.encrypt(
//                                                Helper.serialize(mediRec.keyPair),
//                                                newPassword
//                                            )
//                                        )
//                                    )
                                    editor.putStringSet("TimeSet", mediRec.timeSet)
                                    editor.putString("MediRec", Helper.encodeToString(Helper.serialize(mediRec)))
//                                    editor.putString(
//                                        "randomString",
//                                        Helper.encodeToString(
//                                            Helper.encrypt(
//                                                mediRec.randomString.toByteArray(),
//                                                newPassword
//                                            )
//                                        )
//                                    )
                                    editor.putString("salt", mediRec.salt)
                                    editor.apply()
                                    Toast.makeText(
                                        this,
                                        "File successfully imported and overwritten the current identity",
                                        Toast.LENGTH_SHORT
                                    ).show()
//                                    val intent = Intent(this, MenuActivity::class.java)
//                                    intent.putExtra("FROM_ACTIVITY", "LOAD")
                                    startActivity(Intent(this, MenuActivity::class.java))
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        this,
                                        "The file is corrupted or the password is wrong",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    startActivity(Intent(this, MenuActivity::class.java))
                                }
                            }
                        } else {
                            Toast.makeText(this, "Please choose .medirec file", Toast.LENGTH_SHORT).show()
                        }
                    }
                    builder.show()
                }
            }
        } else {
            Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MenuActivity::class.java))
        }
    }
}
