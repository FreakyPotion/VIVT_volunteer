package com.vivt.vvolunteer.profile

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vivt.vvolunteer.DBFactory.DBConnector
import com.vivt.vvolunteer.R
import com.vivt.vvolunteer.adapters.EventsAdapter
import com.vivt.vvolunteer.tables.EventsTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException

class ProfileVolunteerRequestActivity : AppCompatActivity() {

    private lateinit var eventsList: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_volunteer_request)

        val reqTlb: androidx.appcompat.widget.Toolbar = findViewById(R.id.myrequestTlb)
        setSupportActionBar(reqTlb)
        getSupportActionBar()?.setTitle("Мои заявки")
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true)

        var user = getSharedPreferences("logined", Context.MODE_PRIVATE)

        eventsList = findViewById(R.id.myrequestList)
        val events = arrayListOf<EventsTable>()

        lifecycleScope.launch(Dispatchers.IO) {
            var connect: Connection? = null
            try {
                val connector = DBConnector()
                connect = connector.getConnection()
                if (connect != null) {
                    if (user.getInt("User_ID", 0) != 0) {
                        val EventsSQL: PreparedStatement = connect!!.prepareStatement(
                            "SELECT События.*, Запросы.\"User_ID\", Запросы.\"Status_ID\", Пользователи.Имя, Пользователи.Фамилия, Пользователи.Отчество " +
                                    "FROM События " +
                                    "JOIN Запросы ON События.\"Event_ID\" = Запросы.\"Event_ID\" " +
                                    "JOIN Пользователи ON События.Организатор = Пользователи.\"User_ID\" " +
                                    "WHERE События.\"Event_ID\" = Запросы.\"Event_ID\" " +
                                    "AND Запросы.\"User_ID\" = ? " +
                                    "AND События.Дата > CURRENT_DATE;"
                        )
                        val id = user.getInt("User_ID", 0)
                        EventsSQL.setInt(1, id)

                        val resultSet = EventsSQL.executeQuery()
                        while (resultSet.next()) {
                            val EvID = resultSet.getInt(1)
                            val EvTit = resultSet.getString(2)
                            val EvDesc = resultSet.getString(3)
                            val EvDate = resultSet.getDate(4).toString()
                            val EvOrg = resultSet.getString(13) + " " + resultSet.getString(12) + " " + resultSet.getString(14)
                            val EvMaxPart = resultSet.getInt(6).toString()
                            val EvAddr = resultSet.getString(7)
                            val EvImageURL = resultSet.getString(8)
                            runOnUiThread {
                                events.add(
                                    EventsTable(
                                        EvID,
                                        EvTit,
                                        EvDesc,
                                        EvDate,
                                        EvAddr,
                                        EvOrg,
                                        EvImageURL,
                                        EvMaxPart
                                    )
                                )
                            }
                        }
                        runOnUiThread {
                            eventsList.layoutManager = LinearLayoutManager(this@ProfileVolunteerRequestActivity)
                            val SortedEvents = events.sortedByDescending { it.date }
                            eventsList.adapter = EventsAdapter(SortedEvents, this@ProfileVolunteerRequestActivity, false)
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@ProfileVolunteerRequestActivity, "Ошибка", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                else {
                    runOnUiThread {
                        Toast.makeText(this@ProfileVolunteerRequestActivity, "Сервер не отвечает", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: SQLException) {
                e.printStackTrace()
            } finally {
                // Закрытие соединения с базой данных
                connect?.close()
            }
        }

    }
}