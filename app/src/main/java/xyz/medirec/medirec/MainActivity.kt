package xyz.medirec.medirec

import android.app.AlertDialog
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.content.Context
import android.content.Intent
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private var logInHash : String? = null
    private val pinNumber = mutableListOf<Int>()
    private var tempString : String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logInHash = getSharedPreferences("UserData", MODE_PRIVATE).getString("loginPIN", "")!!
        setContentView(R.layout.activity_main)
        initButtons()
        if(logInHash != "") {
            register_header.text = getString(R.string.PIN)
        }
    }

    private fun initButtons() {
        val randomIndexList = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).shuffled()

        val buttonList = listOf(
            button0, button1, button2, button3, button4,
            button5, button6, button7, button8, button9
        )

        repeat(buttonList.size) {
            buttonList[it].text = randomIndexList[it].toString()
            buttonList[it].textSize = 28f
            buttonList[it].setOnClickListener { e ->
                pinNumber.add((e as Button).text.toString().toInt())
                updateStar()
                if(pinNumber.size == 6) {
                    if(logInHash == "") {
                        registerListener()
                    } else {
                        logInListener()
                    }
                    pinNumber.clear()
                }
            }
        }
        
        blankButton1.setOnClickListener {
            pinNumber.clear()
            updateStar()
        }

        blankButton2.setOnClickListener {
            if(pinNumber.isNotEmpty())
                pinNumber.removeAt(pinNumber.size - 1)
            updateStar()
        }
    }

    private fun updateStar() {
        when {
            pinNumber.isEmpty() -> {
                register_header.text = if(logInHash != "")getString(R.string.PIN)
                else if(tempString != "") getString(R.string.Retype_PIN)
                else getString(R.string.main_header)
            }
            else -> {
                val text = StringBuilder()
                for (count in 0 until pinNumber.size)
                    text.append("* ")
                register_header.text = text.toString()
            }
        }
    }

    private fun listToString(list: List<Int>): String {
        val sb = StringBuilder()
        for(value in list) sb.append(value)
        return sb.toString()
    }

    private fun logInListener() {
        if (tryLogIn(pinNumber)) {
            register_header.text = getString(R.string.PIN)
            logIn()
        } else {
            alertLogInFail(getString(R.string.Wrong_PIN))
            register_header.text = getString(R.string.PIN)
            initButtons()
        }
    }

    private fun registerListener() {
        if(tempString == ""){
            tempString = listToString(pinNumber)
            register_header.text = getString(R.string.Retype_PIN)
            initButtons()
        } else {
            register_header.text = if(tempString != listToString(pinNumber)) {
                alertLogInFail("Mismatch PIN, Re-register PIN")
                tempString = ""
                getString(R.string.main_header)
            } else {
                logInHash = setLogInHash(tempString)
                getString(R.string.PIN)
            }
        }
    }

    private fun setLogInHash (pin : String): String {
        val hash = Helper.getHash(pin)
        val prefs = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString("loginPIN", hash)
        editor.apply()
        initButtons()
        return hash
    }

    private fun logIn() {
        val intent = Intent(this, MenuActivity::class.java)
        intent.putExtra("FROM_ACTIVITY", "MAIN")
        intent.putExtra("pinNumber", listToString(pinNumber))
        startActivity(intent)
    }

    private fun alertLogInFail(message: String) {
        val myMsg = TextView(this)
        myMsg.text = message
        myMsg.top = 3
        myMsg.textSize = 24f
        myMsg.setPadding(0, 40, 0, 0)
        myMsg.gravity = Gravity.CENTER

        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder
            .setView(myMsg)
            .setCancelable(false)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()
        dialog.show()
        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        val positiveButtonLL = positiveButton.layoutParams as LinearLayout.LayoutParams
        positiveButtonLL.weight = 1000f
        positiveButton.layoutParams = positiveButtonLL
    }

    private fun tryLogIn(list: List<Int>): Boolean {
        return logInHash == Helper.getHash(listToString(list))
    }
}
