package com.vivt.vvolunteer.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.vivt.vvolunteer.DBFactory.DBConnector
import com.vivt.vvolunteer.profile.ProfileSettings
import com.vivt.vvolunteer.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.sql.Connection
import java.sql.SQLException
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.vivt.vvolunteer.adapters.EventsAdapter
import com.vivt.vvolunteer.profile.ProfileVolunteerRequestActivity
import com.vivt.vvolunteer.tables.EventsTable
import java.sql.PreparedStatement
import java.text.SimpleDateFormat


class ProfileFragment : Fragment() {

    private lateinit var eventsList: RecyclerView

    @SuppressLint("SuspiciousIndentation", "SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_profile, container, false)



        val profSettings: ImageButton? = rootView.findViewById(R.id.profSettings)
            profSettings?.setOnClickListener {
                val intent = Intent(requireActivity(), ProfileSettings::class.java)
                startActivity(intent)
            }

        val myRequests: ImageButton = rootView.findViewById(R.id.profileMyRequests)
        val color = ContextCompat.getColor(requireContext(), R.color.mainBlue)
        myRequests.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        myRequests.setOnClickListener {
            val intent = Intent(requireActivity(), ProfileVolunteerRequestActivity::class.java)
            startActivity(intent)
        }

        val Fname: TextView = rootView.findViewById(R.id.profileName)
        val Fbirthday: TextView = rootView.findViewById(R.id.profileDate)
        val Fphone: TextView = rootView.findViewById(R.id.profilePhone)
        val Femail: TextView = rootView.findViewById(R.id.profileEmail)
        val Image: ImageView = rootView.findViewById(R.id.profile_image)
        val Rating: TextView = rootView.findViewById(R.id.profileRateLevel)
        val rateStart: TextView = rootView.findViewById(R.id.profileRateStart)
        val rateEnd: TextView = rootView.findViewById(R.id.profileRateEnd)
        val Fcity: TextView = rootView.findViewById(R.id.profileCity)
        var name = ""
        var birthday = ""
        var phone = ""
        var email = ""
        var find_city = 0
        var city = ""
        var Level = 1
        val progress: ProgressBar = rootView.findViewById(R.id.progressBar)
        val progressValue: TextView = rootView.findViewById(R.id.progressValue)
        var imageURL = ""

        val user = requireContext().getSharedPreferences("logined", Context.MODE_PRIVATE)


        // Данные профиля (БД)
        lifecycleScope.launch(Dispatchers.IO) {
            var connect: Connection? = null
            try {
                val connector = DBConnector()
                connect = connector.getConnection()
                if (connect != null) {
                    if (user.getInt("User_ID", 0) != 0) {
                        val UserSQL = connect.prepareStatement("SELECT * FROM Пользователи WHERE \"User_ID\" = ?;")
                        val id = user.getInt("User_ID", 0)
                        UserSQL.setInt(1, id)

                        val resultSet = UserSQL.executeQuery()
                        while (resultSet.next()) {
                            name = resultSet.getString(2).trim() + " " + resultSet.getString(3).trim()

                            val date = resultSet.getDate(10)
                            val dateFormat = SimpleDateFormat("dd.MM.yyyy")
                            birthday = dateFormat.format(date)

                            phone = "+7" + resultSet.getString(6).trim()
                            email = resultSet.getString(5).trim()
                            Level = resultSet.getInt(8)
                            imageURL = resultSet.getString(9)?: ""
                            find_city = resultSet.getInt(12)
                        }

                        UserSQL.close()

                        //SQL - запрос для сопоставления ID города
                        val CitySQL = connect.prepareStatement("SELECT * FROM Города WHERE \"City_ID\" = ?;")
                        CitySQL.setInt(1, find_city)
                        val resultSet_city = CitySQL.executeQuery()
                        while (resultSet_city.next()) {
                            city = resultSet_city.getString(2)
                        }

                        CitySQL.close()

                        requireActivity().runOnUiThread {
                            Fname.text = name
                            Fbirthday.text = birthday
                            Fphone.text = phone
                            Femail.text = email
                            Fcity.text = city
                        }
                        if (imageURL != "") {
                            requireActivity().runOnUiThread {
                                Picasso.get().setLoggingEnabled(true)
                                Picasso.get().load(imageURL).into(Image)
                            }
                        }
                        requireActivity().runOnUiThread {
                            Rating.text = "Уровень" + " " + (Level.div(1000) + 1).toString()
                            progress.progress = Level.mod(1000)
                            progressValue.text = Level.mod(1000).toString()
                        }
                    } else {
                        requireActivity().runOnUiThread {
                            Toast.makeText(requireActivity(), "Ошибка", Toast.LENGTH_SHORT).show()
                        }
                    }



                }
                else {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireActivity(), "Сервер не отвечает", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: SQLException) {
                e.printStackTrace()
            } finally {
                // Закрытие соединения с базой данных
                connect?.close()
            }
        }

        // Список мероприятий
        eventsList = rootView.findViewById(R.id.profileEventList)
        val events = arrayListOf<EventsTable>()

        // Если пользователь - организатор
        if (user.getInt("Role_ID",0) == 0)
        {
            Rating.visibility = TextView.GONE
            rateStart.visibility = TextView.GONE
            rateEnd.visibility = TextView.GONE
            progress.visibility = ProgressBar.GONE
            progressValue.visibility = TextView.GONE
            myRequests.visibility = ImageView.INVISIBLE
            // Загрузка данных на старте
            lifecycleScope.launch(Dispatchers.IO) {
                var connect: Connection? = null
                try {
                    val connector = DBConnector()
                    connect = connector.getConnection()
                    if (connect != null) {
                        if (user.getInt("User_ID", 0) != 0) {
                            val EventsSQL: PreparedStatement = connect!!.prepareStatement(
                                "SELECT События.*, Пользователи.Имя, Пользователи.Фамилия, Пользователи.Отчество " +
                                        "FROM События JOIN Пользователи ON События.Организатор = Пользователи.\"User_ID\" WHERE События.Организатор = ? " +
                                        "AND Дата >= CURRENT_DATE;"
                            )
                            val id = user.getInt("User_ID", 0)
                            EventsSQL.setInt(1, id)

                            val resultSet = EventsSQL.executeQuery()
                            while (resultSet.next()) {
                                val EvID = resultSet.getInt(1)
                                val EvTit = resultSet.getString(2)
                                val EvDesc = resultSet.getString(3)
                                val EvDate = resultSet.getDate(4).toString()
                                val EvOrg = resultSet.getString(11) + " " + resultSet.getString(10) + " " + resultSet.getString(12)
                                val EvMaxPart = resultSet.getInt(6).toString()
                                val EvAddr = resultSet.getString(7)
                                val EvImageURL = resultSet.getString(8)
                                requireActivity().runOnUiThread {
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
                            requireActivity().runOnUiThread {
                                eventsList.layoutManager = LinearLayoutManager(context)
                                val SortedEvents = events.sortedByDescending { it.date }
                                eventsList.adapter = EventsAdapter(SortedEvents, requireContext(), false)
                                if (events.isEmpty()) {
                                    eventsList.visibility = RecyclerView.INVISIBLE
                                    val emptylist: TextView = rootView.findViewById(R.id.profileIfListEmpty)
                                    emptylist.visibility = TextView.VISIBLE
                                } else {
                                    eventsList.visibility = RecyclerView.VISIBLE
                                    val emptylist: TextView = rootView.findViewById(R.id.profileIfListEmpty)
                                    emptylist.visibility = TextView.INVISIBLE
                                }
                            }
                        } else {
                            requireActivity().runOnUiThread {
                                Toast.makeText(requireActivity(), "Ошибка", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    else {
                        requireActivity().runOnUiThread {
                            Toast.makeText(requireActivity(), "Сервер не отвечает", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: SQLException) {
                    e.printStackTrace()
                } finally {
                    // Закрытие соединения с базой данных
                    connect?.close()
                }
            }

            val selected: BottomNavigationView = rootView.findViewById(R.id.profileEventsDateSortMenu)
            selected.setOnItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    // Если выделн тумблер "Активные мероприятия"
                    R.id.nowEvent -> {
                        events.clear()
                        // Загрузка списка данных
                        lifecycleScope.launch(Dispatchers.IO) {
                            var connect: Connection? = null
                            try {
                                val connector = DBConnector()
                                connect = connector.getConnection()
                                if (connect != null) {
                                    if (user.getInt("User_ID", 0) != 0) {
                                        val EventsSQL: PreparedStatement = connect!!.prepareStatement(
                                            "SELECT События.*, Пользователи.Имя, Пользователи.Фамилия, Пользователи.Отчество " +
                                                    "FROM События JOIN Пользователи ON События.Организатор = Пользователи.\"User_ID\" WHERE События.Организатор = ? " +
                                                    "AND Дата >= CURRENT_DATE;"
                                        )
                                        val id = user.getInt("User_ID", 0)
                                        EventsSQL.setInt(1, id)

                                        val resultSet = EventsSQL.executeQuery()
                                        while (resultSet.next()) {
                                            val EvID = resultSet.getInt(1)
                                            val EvTit = resultSet.getString(2)
                                            val EvDesc = resultSet.getString(3)
                                            val EvDate = resultSet.getDate(4).toString()
                                            val EvOrg = resultSet.getString(11) + " " + resultSet.getString(10) + " " + resultSet.getString(12)
                                            val EvMaxPart = resultSet.getInt(6).toString()
                                            val EvAddr = resultSet.getString(7)
                                            val EvImageURL = resultSet.getString(8)
                                            requireActivity().runOnUiThread {
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
                                        requireActivity().runOnUiThread {
                                            eventsList.layoutManager = LinearLayoutManager(context)
                                            val SortedEvents = events.sortedByDescending { it.date }
                                            eventsList.adapter = EventsAdapter(SortedEvents, requireContext(), false)
                                            if (events.isEmpty()) {
                                                eventsList.visibility = RecyclerView.INVISIBLE
                                                val emptylist: TextView = rootView.findViewById(R.id.profileIfListEmpty)
                                                emptylist.visibility = TextView.VISIBLE
                                            } else {
                                                eventsList.visibility = RecyclerView.VISIBLE
                                                val emptylist: TextView = rootView.findViewById(R.id.profileIfListEmpty)
                                                emptylist.visibility = TextView.INVISIBLE
                                            }
                                        }
                                    } else {
                                        requireActivity().runOnUiThread {
                                            Toast.makeText(requireActivity(), "Ошибка", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                else {
                                    requireActivity().runOnUiThread {
                                        Toast.makeText(requireActivity(), "Сервер не отвечает", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: SQLException) {
                                e.printStackTrace()
                            } finally {
                                // Закрытие соединения с базой данных
                                connect?.close()
                            }
                        }
                        true
                    }
                    // Если выделен тумблер "Прошедшие мероприятия"
                    R.id.prevEvent -> {
                        events.clear()
                        // Загрузка списка данных
                        lifecycleScope.launch(Dispatchers.IO) {
                            var connect: Connection? = null
                            try {
                                val connector = DBConnector()
                                connect = connector.getConnection()
                                if (connect != null) {
                                    if (user.getInt("User_ID", 0) != 0) {
                                        val EventsSQL: PreparedStatement = connect!!.prepareStatement(
                                            "SELECT События.*, Пользователи.Имя, Пользователи.Фамилия, Пользователи.Отчество " +
                                                    "FROM События JOIN Пользователи ON События.Организатор = Пользователи.\"User_ID\" WHERE События.Организатор = ? " +
                                                    "AND Дата < CURRENT_DATE;"
                                        )
                                        val id = user.getInt("User_ID", 0)
                                        EventsSQL.setInt(1, id)

                                        val resultSet = EventsSQL.executeQuery()
                                        while (resultSet.next()) {
                                            val EvID = resultSet.getInt(1)
                                            val EvTit = resultSet.getString(2)
                                            val EvDesc = resultSet.getString(3)
                                            val EvDate = resultSet.getDate(4).toString()
                                            val EvOrg = resultSet.getString(11) + " " + resultSet.getString(10) + " " + resultSet.getString(12)
                                            val EvMaxPart = resultSet.getInt(6).toString()
                                            val EvAddr = resultSet.getString(7)
                                            val EvImageURL = resultSet.getString(8)
                                            requireActivity().runOnUiThread {
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
                                        requireActivity().runOnUiThread {
                                            eventsList.layoutManager = LinearLayoutManager(context)
                                            val SortedEvents = events.sortedByDescending { it.date }
                                            eventsList.adapter = EventsAdapter(SortedEvents, requireContext(), false)
                                            if (events.isEmpty()) {
                                                eventsList.visibility = RecyclerView.INVISIBLE
                                                val emptylist: TextView =
                                                    rootView.findViewById(R.id.profileIfListEmpty)
                                                emptylist.visibility = TextView.VISIBLE
                                            } else {
                                                eventsList.visibility = RecyclerView.VISIBLE
                                                val emptylist: TextView = rootView.findViewById(R.id.profileIfListEmpty)
                                                emptylist.visibility = TextView.INVISIBLE
                                            }
                                        }
                                    } else {
                                        requireActivity().runOnUiThread {
                                            Toast.makeText(requireActivity(), "Ошибка", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                else {
                                    requireActivity().runOnUiThread {
                                        Toast.makeText(requireActivity(), "Сервер не отвечает", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: SQLException) {
                                e.printStackTrace()
                            } finally {
                                // Закрытие соединения с базой данных
                                connect?.close()
                            }
                        }
                        true
                    }
                    else -> false
                }
            }
        }
        // Если пользователь - волонтёр
        else if (user.getInt("Role_ID",0) == 1) {
            // Загрузка данных на старте
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
                                        "AND Запросы.\"Status_ID\" = 0 " +
                                        "AND События.Дата >= CURRENT_DATE;"
                            )
                            val id = user.getInt("User_ID", 0)
                            EventsSQL.setInt(1, id)

                            val resultSet = EventsSQL.executeQuery()
                            while (resultSet.next()) {
                                val EvID = resultSet.getInt(1)
                                val EvTit = resultSet.getString(2)
                                val EvDesc = resultSet.getString(3)
                                val EvDate = resultSet.getDate(4).toString()
                                val EvOrg = resultSet.getString(11) + " " + resultSet.getString(10) + " " + resultSet.getString(12)
                                val EvMaxPart = resultSet.getInt(6).toString()
                                val EvAddr = resultSet.getString(7)
                                val EvImageURL = resultSet.getString(8)
                                requireActivity().runOnUiThread {
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
                            requireActivity().runOnUiThread {
                                eventsList.layoutManager = LinearLayoutManager(context)
                                val SortedEvents = events.sortedByDescending { it.date }
                                eventsList.adapter = EventsAdapter(SortedEvents, requireContext(), false)
                                if (events.isEmpty()) {
                                    eventsList.visibility = RecyclerView.INVISIBLE
                                    val emptylist: TextView = rootView.findViewById(R.id.profileIfListEmpty)
                                    emptylist.visibility = TextView.VISIBLE
                                } else {
                                    eventsList.visibility = RecyclerView.VISIBLE
                                    val emptylist: TextView = rootView.findViewById(R.id.profileIfListEmpty)
                                    emptylist.visibility = TextView.INVISIBLE
                                }
                            }
                        } else {
                            requireActivity().runOnUiThread {
                                Toast.makeText(requireActivity(), "Ошибка", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    else {
                        requireActivity().runOnUiThread {
                            Toast.makeText(requireActivity(), "Сервер не отвечает", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: SQLException) {
                    e.printStackTrace()
                } finally {
                    // Закрытие соединения с базой данных
                    connect?.close()
                }
            }

            val selected: BottomNavigationView = rootView.findViewById(R.id.profileEventsDateSortMenu)
            selected.setOnItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    // Если выделн тумблер "Активные мероприятия"
                    R.id.nowEvent -> {
                        events.clear()
                        // Загрузка списка данных
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
                                                    "AND Запросы.\"Status_ID\" = 0 " +
                                                    "AND События.Дата >= CURRENT_DATE;"
                                        )
                                        val id = user.getInt("User_ID", 0)
                                        EventsSQL.setInt(1, id)

                                        val resultSet = EventsSQL.executeQuery()
                                        while (resultSet.next()) {
                                            val EvID = resultSet.getInt(1)
                                            val EvTit = resultSet.getString(2)
                                            val EvDesc = resultSet.getString(3)
                                            val EvDate = resultSet.getDate(4).toString()
                                            val EvOrg = resultSet.getString(11) + " " + resultSet.getString(10) + " " + resultSet.getString(12)
                                            val EvMaxPart = resultSet.getInt(6).toString()
                                            val EvAddr = resultSet.getString(7)
                                            val EvImageURL = resultSet.getString(8)
                                            requireActivity().runOnUiThread {
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
                                        requireActivity().runOnUiThread {
                                            eventsList.layoutManager = LinearLayoutManager(context)
                                            val SortedEvents = events.sortedByDescending { it.date }
                                            eventsList.adapter = EventsAdapter(SortedEvents, requireContext(), false)
                                            if (events.isEmpty()) {
                                                eventsList.visibility = RecyclerView.INVISIBLE
                                                val emptylist: TextView = rootView.findViewById(R.id.profileIfListEmpty)
                                                emptylist.visibility = TextView.VISIBLE
                                            } else {
                                                eventsList.visibility = RecyclerView.VISIBLE
                                                val emptylist: TextView = rootView.findViewById(R.id.profileIfListEmpty)
                                                emptylist.visibility = TextView.INVISIBLE
                                            }
                                        }
                                    } else {
                                        requireActivity().runOnUiThread {
                                            Toast.makeText(requireActivity(), "Ошибка", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                else {
                                    requireActivity().runOnUiThread {
                                        Toast.makeText(requireActivity(), "Сервер не отвечает", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: SQLException) {
                                e.printStackTrace()
                            } finally {
                                // Закрытие соединения с базой данных
                                connect?.close()
                            }
                        }
                        true
                    }
                    // Если выделен тумблер "Прошедшие мероприятия"
                    R.id.prevEvent -> {
                        events.clear()
                        // Загрузка списка данных
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
                                                    "AND Запросы.\"Status_ID\" = 0 " +
                                                    "AND События.Дата < CURRENT_DATE;"
                                        )
                                        val id = user.getInt("User_ID", 0)
                                        EventsSQL.setInt(1, id)

                                        val resultSet = EventsSQL.executeQuery()
                                        while (resultSet.next()) {
                                            val EvID = resultSet.getInt(1)
                                            val EvTit = resultSet.getString(2)
                                            val EvDesc = resultSet.getString(3)
                                            val EvDate = resultSet.getDate(4).toString()
                                            val EvOrg = resultSet.getString(11) + " " + resultSet.getString(10) + " " + resultSet.getString(12)
                                            val EvMaxPart = resultSet.getInt(6).toString()
                                            val EvAddr = resultSet.getString(7)
                                            val EvImageURL = resultSet.getString(8)
                                            requireActivity().runOnUiThread {
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
                                        requireActivity().runOnUiThread {
                                            eventsList.layoutManager = LinearLayoutManager(context)
                                            val SortedEvents = events.sortedByDescending { it.date }
                                            eventsList.adapter = EventsAdapter(SortedEvents, requireContext(), false)
                                            if (events.isEmpty()) {
                                                eventsList.visibility = RecyclerView.INVISIBLE
                                                val emptylist: TextView =
                                                    rootView.findViewById(R.id.profileIfListEmpty)
                                                emptylist.visibility = TextView.VISIBLE
                                            } else {
                                                eventsList.visibility = RecyclerView.VISIBLE
                                                val emptylist: TextView = rootView.findViewById(R.id.profileIfListEmpty)
                                                emptylist.visibility = TextView.INVISIBLE
                                            }
                                        }
                                    } else {
                                        requireActivity().runOnUiThread {
                                            Toast.makeText(requireActivity(), "Ошибка", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                else {
                                    requireActivity().runOnUiThread {
                                        Toast.makeText(requireActivity(), "Сервер не отвечает", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: SQLException) {
                                e.printStackTrace()
                            } finally {
                                // Закрытие соединения с базой данных
                                connect?.close()
                            }
                        }
                        true
                    }
                    else -> false
                }
            }
        }



        return rootView
    }
}