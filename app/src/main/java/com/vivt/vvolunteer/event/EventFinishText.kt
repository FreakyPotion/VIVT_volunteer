package com.vivt.vvolunteer.event

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vivt.vvolunteer.MainActivity
import com.vivt.vvolunteer.R

class EventFinishText : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_finish_text)

        val eventid = intent.getStringExtra("eventid")

        val Tlb: androidx.appcompat.widget.Toolbar = findViewById(R.id.textReportTlb)
        setSupportActionBar(Tlb)
        getSupportActionBar()?.setTitle("Отчёт")
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true)

        val text: EditText = findViewById(R.id.evReportText)
        val charCount: TextView = findViewById(R.id.textSymbols)

        // Добавляем TextWatcher для EditText
        text.addTextChangedListener(object : TextWatcher {
            @SuppressLint("SetTextI18n")
            override fun afterTextChanged(s: Editable?) {
                // Обновляем количество символов
                charCount.text = s?.length.toString() + "/500"
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Не нужно ничего делать здесь
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Не нужно ничего делать здесь
            }
        })

        val nextStepBtn: Button = findViewById(R.id.evNextBtn)
        nextStepBtn.setOnClickListener {
            if (text.text.isEmpty()) {
                Toast.makeText(this, "Введите текст отчёта!", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, EventFinishActivity::class.java)
                intent.putExtra("textReport", text.text.toString())
                intent.putExtra("eventid", eventid)
                startActivity(intent)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {

                val Title: String?
                val Adress: String?
                val Date: String?
                val MaxParticipants: String?
                val Desc: String?
                val Image: String?

                //Берём данные из события
                val eventid = intent.getStringExtra("eventid")
                Title = intent.getStringExtra("prevTitle").toString()
                Adress = intent.getStringExtra("prevAdress").toString()
                Date = intent.getStringExtra("prevDate").toString()
                MaxParticipants= intent.getStringExtra("prevMaxP").toString()
                Desc = intent.getStringExtra("prevDesc").toString()
                Image = intent.getStringExtra("prevImage").toString()

                //Возвращаем данные в событие
                val intent = Intent()
                intent.putExtra("eventid", eventid)
                intent.putExtra("prevTitle", Title)
                intent.putExtra("prevDate", Date)
                intent.putExtra("prevAdress",Adress)
                intent.putExtra("prevMaxP",MaxParticipants)
                intent.putExtra("prevDesc", Desc)
                intent.putExtra("prevImage",Image)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}