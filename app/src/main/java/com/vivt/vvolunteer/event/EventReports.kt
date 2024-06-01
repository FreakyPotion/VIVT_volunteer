package com.vivt.vvolunteer.event

import ReportAdapter
import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vivt.vvolunteer.DBFactory.DBConnector
import com.vivt.vvolunteer.R
import com.vivt.vvolunteer.tables.EventsTable
import com.vivt.vvolunteer.tables.ReportTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException

class EventReports : AppCompatActivity() {

    private lateinit var imagesList: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_reports)

        val evDetTlb: androidx.appcompat.widget.Toolbar = findViewById(R.id.repTlb)
        setSupportActionBar(evDetTlb)
        getSupportActionBar()?.setTitle("Отчёт")
        evDetTlb.navigationIcon?.colorFilter = PorterDuffColorFilter(ContextCompat.getColor(this, R.color.black), PorterDuff.Mode.SRC_ATOP)
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true)

        val eventid = intent.getStringExtra("eventid")

        imagesList = findViewById(R.id.repList)
        val images = arrayListOf<ReportTable>()
        var getsim: ArrayList<String>
        imagesList.layoutManager = GridLayoutManager(this, 3)

        lifecycleScope.launch(Dispatchers.IO) {
            getsim = getImages(eventid)
            for (i in getsim.indices) {
                runOnUiThread {
                    images.add(ReportTable(getsim[i]))
                    imagesList.adapter = ReportAdapter(images, this@EventReports)
                }
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
                intent.putExtra("prevDate", Adress)
                intent.putExtra("prevAdress",Date)
                intent.putExtra("prevMaxP",MaxParticipants)
                intent.putExtra("prevDesc", Desc)
                intent.putExtra("prevImage",Image)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private suspend fun getImages(eventid: String?): ArrayList<String> {
        val arrayImageURL = ArrayList<String>()
        var connect: Connection? = null
        try {
            val connector = DBConnector()
            connect = connector.getConnection()
            if (connect != null) {
                val getCreatorSQL: PreparedStatement = connect.prepareStatement("SELECT \"Отчёт_Фото\" FROM Отчёты WHERE \"Event_ID\" = ?")
                if (eventid != null) {
                    getCreatorSQL.setInt(1, eventid.toInt())
                }
                val resultSet = getCreatorSQL.executeQuery()
                while (resultSet.next()) {
                    val array = resultSet.getArray("Отчёт_Фото").array as Array<*>
                    array.forEach { element ->
                        if (element != null) {
                            arrayImageURL.add(element.toString())
                        }
                    }
                    break
                }
                getCreatorSQL.close()
            }
            else {
                runOnUiThread {
                    Toast.makeText(this@EventReports, "Сервер не отвечает", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            // Закрытие соединения с базой данных
            connect?.close()
        }
        return arrayImageURL
    }
}