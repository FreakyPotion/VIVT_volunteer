package com.vivt.vvolunteer.event

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.squareup.picasso.Picasso
import com.vivt.vvolunteer.DBFactory.DBConnector
import com.vivt.vvolunteer.MainActivity
import com.vivt.vvolunteer.R
import kotlinx.coroutines.Dispatchers
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException

class EventDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_details)

        val Title: TextView = findViewById(R.id.evDetTitle)
        val Adress: TextView = findViewById(R.id.evDetAdressText)
        val Date: TextView = findViewById(R.id.evDetDateText)
        val Org: TextView = findViewById(R.id.evDetOrganizerText)
        val MaxParticipants: TextView = findViewById(R.id.evDetMPText)
        val Desc: TextView = findViewById(R.id.evDetDescriptionText)
        val Image: ImageView = findViewById(R.id.evDetImage)

        val eventid = intent.getStringExtra("eventid")
        Title.text = intent.getStringExtra("prevTitle")
        Adress.text = intent.getStringExtra("prevAdress")
        Date.text = intent.getStringExtra("prevDate")
        Org.text = intent.getStringExtra("prevOrg")
        MaxParticipants.text = intent.getStringExtra("prevMaxP")
        Desc.text = intent.getStringExtra("prevDesc")
        Picasso.get().load(intent.getStringExtra("prevImage")).into(Image)

        val evDetTlb: androidx.appcompat.widget.Toolbar = findViewById(R.id.evDetTlb)
        setSupportActionBar(evDetTlb)
        getSupportActionBar()?.setTitle("Детали события")
        evDetTlb.navigationIcon?.colorFilter = PorterDuffColorFilter(ContextCompat.getColor(this, R.color.black), PorterDuff.Mode.SRC_ATOP)
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true)

        // Кнопка "Заявки"
        val requests: Button = findViewById(R.id.requestButton)
        requests.setOnClickListener {
            val intent = Intent(this, RequestsActivity::class.java)
            intent.putExtra("eventid", eventid)
            intent.putExtra("prevTitle", Title.text).toString()
            intent.putExtra("prevDate", Date.text).toString()
            intent.putExtra("prevAdress", Adress.text).toString()
            intent.putExtra("prevMaxP", MaxParticipants.text).toString()
            intent.putExtra("prevDesc", Desc.text).toString()
            intent.putExtra("prevImage",intent.getStringExtra("prevImage")).toString()
            startActivity(intent)
        }

        // Кнопка "Список участников"
        val listParticipants: Button = findViewById(R.id.listParticipatesButton)
        listParticipants.setOnClickListener {
            val intent = Intent(this, ParticipantsListActivity::class.java)
            intent.putExtra("eventid", eventid)
            intent.putExtra("prevTitle", Title.text).toString()
            intent.putExtra("prevDate", Date.text).toString()
            intent.putExtra("prevAdress", Adress.text).toString()
            intent.putExtra("prevMaxP", MaxParticipants.text).toString()
            intent.putExtra("prevDesc", Desc.text).toString()
            intent.putExtra("prevImage",intent.getStringExtra("prevImage")).toString()
            startActivity(intent)
        }


        // Кнопка "Отправить заявку" - определение
        val pullRequest: Button = findViewById(R.id.pullRequestButton)

        // Проверка существования заявки
        lifecycleScope.launch(Dispatchers.IO) {
            val curStatus = CheckRequestExist(eventid)
            if (curStatus == 0) {
                runOnUiThread {
                    pullRequest.setBackgroundResource(R.drawable.white_green_backgorund_item)
                    pullRequest.setTextColor(ContextCompat.getColor(this@EventDetailsActivity, R.color.Green))
                    pullRequest.setText(R.string.evAcceptedRequest)
                    pullRequest.isClickable = false
                }
            } else if (curStatus == 1) {
                runOnUiThread {
                    pullRequest.setBackgroundResource(R.drawable.white_blue_backgorund_item)
                    pullRequest.setTextColor(ContextCompat.getColor(this@EventDetailsActivity, R.color.mainBlue))
                    pullRequest.setText(R.string.evPuilledRequest)
                    pullRequest.isClickable = false
                }
            }
            else if (curStatus == 2) {
                runOnUiThread {
                    pullRequest.setBackgroundResource(R.drawable.white_red_backgorund_item)
                    pullRequest.setTextColor(ContextCompat.getColor(this@EventDetailsActivity, R.color.Red ))
                    pullRequest.setText(R.string.evDeclinedRequest)
                    pullRequest.isClickable = false
                }
            }

        }

        // Кнопка "Отправить заявку"
        pullRequest.setOnClickListener{
            lifecycleScope.launch(Dispatchers.IO) {
                PullRequestOnEvent(eventid)
                runOnUiThread {
                    pullRequest.setBackgroundResource(R.drawable.white_blue_backgorund_item)
                    pullRequest.setTextColor(ContextCompat.getColor(this@EventDetailsActivity, R.color.mainBlue))
                    pullRequest.setText(R.string.evPuilledRequest)
                    pullRequest.isClickable = false
                }

            }
        }


        // Кнопка "Удалить событие"

        val deleteEvent: Button = findViewById(R.id.deleteButton)
        deleteEvent.setOnClickListener {
            showConfirmationDialog(DialogInterface.OnClickListener { dialog, which ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        lifecycleScope.launch(Dispatchers.IO) {
                            DeleteEvent(eventid)
                            runOnUiThread {
                                val intent = Intent(this@EventDetailsActivity, MainActivity::class.java)
                                startActivity(intent)
                            }
                        }

                    }
                    DialogInterface.BUTTON_NEGATIVE -> {
                    }
                }
            })

        }


        // Кнопка "Завершить событие"
        val finishEvent: Button = findViewById(R.id.finishButton)
        finishEvent.setOnClickListener {
            val intent = Intent(this, EventFinishText::class.java)
            intent.putExtra("eventid", eventid)
            intent.putExtra("prevTitle", Title.text).toString()
            intent.putExtra("prevDate", Date.text).toString()
            intent.putExtra("prevAdress", Adress.text).toString()
            intent.putExtra("prevMaxP", MaxParticipants.text).toString()
            intent.putExtra("prevDesc", Desc.text).toString()
            intent.putExtra("prevImage",intent.getStringExtra("prevImage")).toString()
            startActivity(intent)
        }


        // Кнопка "Отчёт"
        val reportBtn: Button = findViewById(R.id.reportButton)
        reportBtn.setOnClickListener {
            val intent = Intent(this, EventReports::class.java)
            intent.putExtra("eventid", eventid)
            intent.putExtra("prevTitle", Title.text).toString()
            intent.putExtra("prevDate", Date.text).toString()
            intent.putExtra("prevAdress", Adress.text).toString()
            intent.putExtra("prevMaxP", MaxParticipants.text).toString()
            intent.putExtra("prevDesc", Desc.text).toString()
            intent.putExtra("prevImage",intent.getStringExtra("prevImage")).toString()
            startActivity(intent)
        }

        // Настройки отображения в зависимости от пользователя
        val role = getSharedPreferences("logined", Context.MODE_PRIVATE)
        if (role.getInt("Role_ID", 0) != 0) {
            requests.visibility = Button.GONE
            listParticipants.visibility = Button.GONE
        } else {
            pullRequest.visibility = Button.GONE
        }

        var creator: Int
        var finished: Boolean
        var reported: Boolean

        // Отображение кнопки "Удалить событие" или "Завершить событие" в зависимости от прав на событие
        lifecycleScope.launch(Dispatchers.IO) {
            creator = getUserID(eventid)
            finished = getFinished(eventid)
            reported = checkStatus(eventid)
            runOnUiThread {
                if (role.getInt("User_ID", 0) != creator) {
                    deleteEvent.visibility = Button.GONE
                    requests.visibility = Button.GONE
                    finishEvent.visibility = Button.GONE
                    listParticipants.visibility = Button.GONE
                    reportBtn.visibility = Button.GONE
                } else {
                    if (!finished) {
                        finishEvent.visibility = Button.GONE
                        reportBtn.visibility = Button.GONE
                    } else if (!reported) {
                        deleteEvent.visibility = Button.GONE
                        reportBtn.visibility = Button.GONE
                    } else {
                        finishEvent.visibility = Button.GONE
                        deleteEvent.visibility = Button.GONE
                    }
                }
            }
        }
   }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }


    // Получение данных о правах на событие (БД)
    private suspend fun getUserID(eventid: String?): Int {
        var connect: Connection? = null
        var creator = 0
        try {
            val connector = DBConnector()
            connect = connector.getConnection()
            if (connect != null) {
                val getCreatorSQL: PreparedStatement = connect.prepareStatement("SELECT Организатор FROM События WHERE \"Event_ID\" = ?")
                if (eventid != null) {
                    getCreatorSQL.setInt(1, eventid.toInt())
                }
                val resultSet = getCreatorSQL.executeQuery()
                while (resultSet.next()) {
                    creator = resultSet.getInt(1)
                    break
                }
                getCreatorSQL.close()
            }
            else {
                runOnUiThread {
                    Toast.makeText(this@EventDetailsActivity, "Сервер не отвечает", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            // Закрытие соединения с базой данных
            connect?.close()
        }
        return creator
    }

    // Текующая ли дата
    private suspend fun getFinished(eventid: String?): Boolean {
        var connect: Connection? = null
        var toFinish = false
        var date = ""
        try {
            val connector = DBConnector()
            connect = connector.getConnection()
            if (connect != null) {
                val getDateSQL: PreparedStatement = connect.prepareStatement("SELECT Дата FROM События WHERE \"Event_ID\" = ? AND Дата <= CURRENT_DATE")
                if (eventid != null) {
                    getDateSQL.setInt(1, eventid.toInt())
                }
                val resultSet = getDateSQL.executeQuery()
                while (resultSet.next()) {
                    date = resultSet.getDate(1).toString()
                    break
                }
                if (date != "") {
                    toFinish = true
                }

                getDateSQL.close()
            }
            else {
                runOnUiThread {
                    Toast.makeText(this@EventDetailsActivity, "Сервер не отвечает", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            // Закрытие соединения с базой данных
            connect?.close()
        }
        return toFinish
    }


    // Проверка статуса
    private suspend fun checkStatus(eventid: String?): Boolean {
        var connect: Connection? = null
        var toFinish = false
        var status = 0
        try {
            val connector = DBConnector()
            connect = connector.getConnection()
            if (connect != null) {
                val getStatusSQL: PreparedStatement = connect.prepareStatement("SELECT Завершено FROM События WHERE \"Event_ID\" = ?")
                if (eventid != null) {
                    getStatusSQL.setInt(1, eventid.toInt())
                }
                val resultSet = getStatusSQL.executeQuery()
                while (resultSet.next()) {
                    status = resultSet.getInt(1)
                    break
                }
                if (status != 0) {
                    toFinish = true
                }

                getStatusSQL.close()
            }
            else {
                runOnUiThread {
                    Toast.makeText(this@EventDetailsActivity, "Сервер не отвечает", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            // Закрытие соединения с базой данных
            connect?.close()
        }
        return toFinish
    }

    // Удаление события из БД (БД)
    private suspend fun DeleteEvent(eventid: String?) {
        var connect: Connection? = null
        try {
            val connector = DBConnector()
            connect = connector.getConnection()
            if (connect != null) {
                val delSQL: PreparedStatement = connect!!.prepareStatement("DELETE FROM События WHERE \"Event_ID\" = ?")
                if (eventid != null) {
                    delSQL.setInt(1, eventid.toInt())
                    delSQL.executeUpdate()
                }
                delSQL.close()
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@EventDetailsActivity,
                            "Сервер не отвечает",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            // Закрытие соединения с базой данных
            connect?.close()
        }
    }

    // Отправление заявки
    private suspend fun PullRequestOnEvent(eventid: String?) {
        val userid =  getSharedPreferences("logined", Context.MODE_PRIVATE).getInt("User_ID",0)
        var connect: Connection? = null
        try {
            val connector = DBConnector()
            connect = connector.getConnection()
            if (connect != null) {
                var id = 0

                //SQL - запрос для получения ID  последнего запроса
                val getSQL_reqid = connect.createStatement().executeQuery("SELECT * FROM Запросы ORDER BY \"Request_ID\" ASC;")

                while (getSQL_reqid.next()) {
                    id = getSQL_reqid.getInt(4)
                }

                getSQL_reqid.close()

                val requesSQL: PreparedStatement = connect.prepareStatement("INSERT INTO Запросы (\"Event_ID\", \"User_ID\",\"Request_ID\") " +
                        "VALUES (?, ?, ?)")
                if (eventid != null) {
                    requesSQL.setInt(1, eventid.toInt())
                    requesSQL.setInt(2, userid)
                    requesSQL.setInt(3, id + 1)
                    requesSQL.executeUpdate()
                }
                requesSQL.close()
            } else {
                runOnUiThread {
                    Toast.makeText(
                        this@EventDetailsActivity,
                        "Сервер не отвечает",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            // Закрытие соединения с базой данных
            connect?.close()
        }
    }

    // Проверка отправлена ли заявка
    private suspend fun CheckRequestExist(eventid: String?): Int {
        val userid =  getSharedPreferences("logined", Context.MODE_PRIVATE).getInt("User_ID",0)
        var result = 3
        var status = 3
        var connect: Connection? = null
        try {
            val connector = DBConnector()
            connect = connector.getConnection()
            if (connect != null) {
                //SQL - запрос для получения ID  последнего запроса
                val checkSQL: PreparedStatement = connect.prepareStatement("SELECT * FROM Запросы WHERE \"User_ID\" = ? AND \"Event_ID\" = ?;")

                if (eventid != null) {
                    checkSQL.setInt(1, userid)
                    checkSQL.setInt(2, eventid.toInt())
                }
                val resultset = checkSQL.executeQuery()

                while (resultset.next()) {
                    status = resultset.getInt(3)
                }
                if (status != 3) {
                    result = status
                }
                checkSQL.close()

            } else {
                runOnUiThread {
                    Toast.makeText(
                        this@EventDetailsActivity,
                        "Сервер не отвечает",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            // Закрытие соединения с базой данных
            connect?.close()
        }
        return result
    }


    // Диалог подтвеждрения удаления
    private fun showConfirmationDialog(listener: DialogInterface.OnClickListener) {
        val builder = AlertDialog.Builder(this)

        builder.setTitle("Подтверждение")
            .setMessage("Вы уверены, что хотите продолжить?")
            .setPositiveButton("Да", listener)
            .setNegativeButton("Отмена", listener)

        val dialog = builder.create()
        dialog.show()
    }



}