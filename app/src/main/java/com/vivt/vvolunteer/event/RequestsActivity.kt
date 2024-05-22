package com.vivt.vvolunteer.event

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.content.Context
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isEmpty
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vivt.vvolunteer.DBFactory.DBConnector
import com.vivt.vvolunteer.R
import com.vivt.vvolunteer.adapters.UsersAdapter
import com.vivt.vvolunteer.tables.UsersTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import java.text.SimpleDateFormat

class RequestsActivity : AppCompatActivity() {

    private lateinit var usersList: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_requests)

        val reqTlb: androidx.appcompat.widget.Toolbar = findViewById(R.id.requestsToolbar)
        setSupportActionBar(reqTlb)
        getSupportActionBar()?.setTitle("Список заявок")
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true)


        //Берём данные из события
        val eventid = intent.getStringExtra("eventid")


        usersList = findViewById(R.id.requestsList)
        val users = arrayListOf<UsersTable>()

        lifecycleScope.launch(Dispatchers.IO) {
            var connect: Connection? = null
            try {
                val connector = DBConnector()
                connect = connector.getConnection()
                if (connect != null) {
                    val RequestSQL: PreparedStatement = connect.prepareStatement(
                        "SELECT * FROM Запросы JOIN Пользователи ON Запросы.\"Event_ID\" = ? WHERE Запросы.\"User_ID\" = Пользователи.\"User_ID\" AND Запросы.\"Status_ID\" = 1;"
                    )
                    RequestSQL.setInt(1, eventid!!.toInt())

                    val resultSet = RequestSQL.executeQuery()
                    while (resultSet.next()) {
                        val UID = resultSet.getInt(5)
                        val UName = resultSet.getString(6)
                        val USurname = resultSet.getString(7)
                        val UPatronymic = resultSet.getString(8)
                        val UMail = resultSet.getString(9)
                        val UPhone = "+7" + resultSet.getString(10 )

                        val date = resultSet.getDate(14)
                        val dateFormat = SimpleDateFormat("dd.MM.yyyy")
                        val UDate = dateFormat.format(date)

                        val URate = resultSet.getInt(12)
                        val UImage = resultSet.getString(13)
                        val UCity = resultSet.getInt(16)

                        var city = ""
                        //SQL - запрос для сопоставления ID города
                        val CitySQL = connect.prepareStatement("SELECT * FROM Города WHERE \"City_ID\" = ?;")
                        CitySQL.setInt(1, UCity)
                        val resultSet_city = CitySQL.executeQuery()
                        while (resultSet_city.next()) {
                            city = resultSet_city.getString(2)
                        }
                        CitySQL.close()

                        runOnUiThread {
                            users.add(
                                UsersTable(
                                    UID,
                                    UName,
                                    USurname,
                                    UPatronymic,
                                    UMail,
                                    UPhone,
                                    UImage,
                                    URate,
                                    UDate,
                                    city
                                )
                            )
                        }
                    }
                    RequestSQL.close()
                    runOnUiThread {
                        usersList.layoutManager = LinearLayoutManager(this@RequestsActivity)
                        val SortedUsers = users.sortedByDescending { it.name}
                        usersList.adapter = UsersAdapter(SortedUsers, this@RequestsActivity, eventid, true)
                        if (users.isEmpty()) {
                            val empty: TextView = findViewById(R.id.reqListEmpty)
                            empty.visibility = TextView.VISIBLE
                        }
                    }
                }
                else {
                    runOnUiThread {
                        Toast.makeText(this@RequestsActivity, "Сервер не отвечает", Toast.LENGTH_SHORT).show()
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
}