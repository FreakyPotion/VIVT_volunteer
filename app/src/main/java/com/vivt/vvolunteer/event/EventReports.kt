package com.vivt.vvolunteer.event

import ReportAdapter
import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.text.Editable
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
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
        imagesList.layoutManager = GridLayoutManager(this, 4)

        val organizerName: TextView = findViewById(R.id.reportOrg)
        val organizerImage: ImageView = findViewById(R.id.report_image)
        val reportText: EditText = findViewById(R.id.reportText)
        val reportImageTitle: TextView = findViewById(R.id.reportImageTitle)

        lifecycleScope.launch(Dispatchers.IO) {
            getsim = getImages(eventid)
            val organizer = getOrganizer(eventid)
            val imageURL = getOrganizerImage(eventid)
            val textReport = getOrganizerText(eventid)
            for (i in getsim.indices) {
                runOnUiThread {
                    images.add(ReportTable(getsim[i]))
                    imagesList.adapter = ReportAdapter(images, this@EventReports)
                }
            }
            runOnUiThread {
                if (images.isEmpty()) {
                    reportImageTitle.visibility = TextView.GONE
                }
                organizerName.text = organizer
                Picasso.get().load(imageURL).into(organizerImage)
                reportText.text = Editable.Factory.getInstance().newEditable(textReport)
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
                    val sqlArray = resultSet.getArray("Отчёт_Фото")
                    if (sqlArray != null) {
                        val array = sqlArray.array as Array<*>
                        array.forEach { element ->
                            if (element != null) {
                                arrayImageURL.add(element.toString())
                            }
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

    private suspend fun getOrganizer(eventid: String?): String {
        var organizer = ""
        var connect: Connection? = null
        try {
            val connector = DBConnector()
            connect = connector.getConnection()
            if (connect != null) {
                val getCreatorSQL: PreparedStatement = connect.prepareStatement("SELECT Имя, Фамилия FROM Пользователи WHERE \"User_ID\" IN " +
                        "(SELECT Организатор FROM События WHERE \"Event_ID\" = ?)")
                if (eventid != null) {
                    getCreatorSQL.setInt(1, eventid.toInt())
                }
                val resultSet = getCreatorSQL.executeQuery()
                while (resultSet.next()) {
                    organizer = resultSet.getString(1) + " " + resultSet.getString(2)
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
        return organizer
    }

    private suspend fun getOrganizerImage(eventid: String?): String {
        var url = ""
        var connect: Connection? = null
        try {
            val connector = DBConnector()
            connect = connector.getConnection()
            if (connect != null) {
                val getCreatorSQL: PreparedStatement = connect.prepareStatement("SELECT \"Фото профиля\" FROM Пользователи WHERE \"User_ID\" IN " +
                        "(SELECT Организатор FROM События WHERE \"Event_ID\" = ?)")
                if (eventid != null) {
                    getCreatorSQL.setInt(1, eventid.toInt())
                }
                val resultSet = getCreatorSQL.executeQuery()
                while (resultSet.next()) {
                    url = resultSet.getString(1)
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
        return url
    }

    private suspend fun getOrganizerText(eventid: String?): String {
        var text = ""
        var connect: Connection? = null
        try {
            val connector = DBConnector()
            connect = connector.getConnection()
            if (connect != null) {
                val getTextSQL: PreparedStatement = connect.prepareStatement("SELECT text FROM Отчёты WHERE \"Event_ID\" = ?")
                if (eventid != null) {
                    getTextSQL.setInt(1, eventid.toInt())
                }
                val resultSet = getTextSQL.executeQuery()
                while (resultSet.next()) {
                    text = resultSet.getString(1)
                }
                getTextSQL.close()
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
        return text
    }

}