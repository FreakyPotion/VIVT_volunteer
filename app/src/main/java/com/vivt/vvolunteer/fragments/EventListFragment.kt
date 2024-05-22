package com.vivt.vvolunteer.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Toast
import com.vivt.vvolunteer.DBFactory.DBConnector
import com.vivt.vvolunteer.adapters.EventsAdapter
import com.vivt.vvolunteer.R
import com.vivt.vvolunteer.event.EventCreateActivity
import com.vivt.vvolunteer.tables.EventsTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.sql.Connection
import java.sql.SQLException
import java.text.SimpleDateFormat


class EventListFragment : Fragment() {

    private lateinit var eventsList: RecyclerView


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_event_list, container, false)

        val createTlb: androidx.appcompat.widget.Toolbar = view.findViewById(R.id.mainTlb)

        val role = requireContext().getSharedPreferences("logined", Context.MODE_PRIVATE)
        if (role.getInt("Role_ID", 0) != 0) {
            createTlb.visibility = androidx.appcompat.widget.Toolbar.GONE
        }

        createTlb.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_add -> {
                    val intent = Intent(requireContext(), EventCreateActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        // Загрузка списка (БД)
        eventsList = view.findViewById(R.id.eventsListOrg)
        val events = arrayListOf<EventsTable>()

        lifecycleScope.launch(Dispatchers.IO) {
            var connect: Connection? = null
            try {
                val connector = DBConnector()
                connect = connector.getConnection()
                if (connect != null) {
                    val statement = connect.createStatement()
                    val EventsSQL = statement.executeQuery("SELECT События.*, Пользователи.Имя, Пользователи.Фамилия, Пользователи.Отчество " +
                            "FROM События JOIN Пользователи ON События.Организатор = Пользователи.\"User_ID\" WHERE Дата >= CURRENT_DATE;")

                    while (EventsSQL.next()) {
                        val EvID = EventsSQL.getInt(1)
                        val EvTit = EventsSQL.getString(2)
                        val EvDesc = EventsSQL.getString(3)

                        val date = EventsSQL.getDate(4)
                        val dateFormat = SimpleDateFormat("dd.MM.yyyy")
                        val EvDate = dateFormat.format(date)

                        val EvOrg = EventsSQL.getString(11) + " " + EventsSQL.getString(10) + " " + EventsSQL.getString(12)
                        val EvMaxPart = EventsSQL.getInt(6).toString()
                        val EvAddr = EventsSQL.getString(7)
                        val EvImageURL = EventsSQL.getString(8)
                        requireActivity().runOnUiThread {
                            events.add(EventsTable(EvID, EvTit, EvDesc, EvDate, EvAddr, EvOrg, EvImageURL, EvMaxPart))
                            val SortedEvents = events.sortedByDescending { it.id }
                            eventsList.layoutManager = LinearLayoutManager(context)
                            eventsList.adapter = EventsAdapter(SortedEvents, requireContext(), true)
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


        return view
    }

}