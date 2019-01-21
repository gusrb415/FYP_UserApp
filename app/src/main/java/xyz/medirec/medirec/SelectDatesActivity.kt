package xyz.medirec.medirec

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.widget.CheckBox
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_select_dates.*
import java.security.KeyPair
import java.text.SimpleDateFormat
import java.util.*

class SelectDatesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_dates)
        val timeSet = getSharedPreferences("UserData", Context.MODE_PRIVATE).getStringSet("TimeSet", mutableSetOf())!!
        timeSet.sorted()

        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss Z", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getDefault()
        for(timestamp in timeSet) {
            val box = CheckBox(this)
            val date = Date(timestamp.toLong())
            box.text = dateFormat.format(date)
            box.textSize = 20f
            dateList.addView(box)
        }

        generateKey.setOnClickListener {
            val list = mutableListOf<Long>()
            var i = 0
            for(timestamp in timeSet)
                if((dateList.getChildAt(i++) as CheckBox).isChecked)
                    list.add(timestamp.toLong())

            val intent = Intent(this, ViewQrActivity::class.java)
            intent.putExtra("keyPair", this.intent.getSerializableExtra("keyPair") as KeyPair)
            intent.putExtra("randomString", this.intent.getStringExtra("randomString"))
            intent.putExtra("timeList", list.toLongArray())
            startActivity(intent)
        }

        backMain.setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }

        selectAll.setOnCheckedChangeListener { _, isChecked ->
            for(i in 0 until timeSet.size)
                (dateList.getChildAt(i) as CheckBox).isChecked = isChecked
        }

        discard.setOnClickListener {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder
                .setMessage("Are you sure to discard selected dates?\n(Discarded dates are irreparable)")
                .setCancelable(false)
                .setPositiveButton("Yes") { _, _ ->
                    var i = 0
                    while(i < dateList.childCount) {
                        if((dateList.getChildAt(i) as CheckBox).isChecked) {
                            val date = (dateList.getChildAt(i) as CheckBox).text
                            for(time in timeSet) {
                                val dateString = dateFormat.format(Date(time.toLong()))
                                if(dateString == date) {
                                    timeSet.remove(time)
                                    break
                                }
                            }
                            dateList.removeViewAt(i)
                        } else ++i
                    }
                    if(timeSet.isEmpty()) generateKey.performClick()
                }
                .setNegativeButton("No") { dialog, _ -> dialog.dismiss()}

            val dialog = builder.show()

            val myMsg = dialog.findViewById<TextView>(android.R.id.message)
            myMsg.gravity = Gravity.CENTER
            myMsg.textSize = 20f
        }

        //INITIALLY SELECT ALL
        selectAll.performClick()

        // IF THERE IS NO TIMESET -> CREATE KEY
        if(timeSet.isEmpty()) generateKey.performClick()
    }
}
