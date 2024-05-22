package com.vivt.vvolunteer.profile

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import com.vivt.vvolunteer.R
import com.vivt.vvolunteer.DBFactory.DBConnector
import androidx.lifecycle.lifecycleScope
import java.sql.SQLException
import java.sql.Statement
import java.sql.Connection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.sql.PreparedStatement
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern

class ProfilePersonalInfo : AppCompatActivity() {

    private lateinit var FDate: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_personal_info)

        val Tlb: androidx.appcompat.widget.Toolbar = findViewById(R.id.profSetTlb)
        setSupportActionBar(Tlb)
        getSupportActionBar()?.setTitle("Личная информация")
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true)

        val Apply: Button = findViewById(R.id.profileApply)

        var user = getSharedPreferences("logined", Context.MODE_PRIVATE)
        val userid = user.getInt("User_ID", 0)

        val Fname: EditText = findViewById(R.id.prEdName)
        val Fsurname: EditText = findViewById(R.id.prEdSurname)
        val Fpatronymic: EditText = findViewById(R.id.prEdPatronymic)
        val Fphone: EditText = findViewById(R.id.prEdPhone)
        val Femail: EditText = findViewById(R.id.prEdEmail)

        val cities_list: Spinner = findViewById(R.id.prEdCityList)
        val list: Array<String> = resources.getStringArray(R.array.cities)

        //Адаптер для списка
        val adapter: ArrayAdapter<String> =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, list)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        cities_list.adapter = adapter
        // Адаптер для списка End

        FDate = findViewById(R.id.prEdDate)

        FDate.setOnClickListener {
            showDatePickerDialog()
        }


        //Подгрузка данных
        lifecycleScope.launch(Dispatchers.IO) {
            var connect: Connection? = null
            try {
                val connector = DBConnector()
                connect = connector.getConnection()
                if (connect != null) {
                    val fillSQL: PreparedStatement =
                        connect.prepareStatement("SELECT * FROM Пользователи WHERE \"User_ID\" = ?;")
                    fillSQL.setInt(1, userid)
                    val resultSet = fillSQL.executeQuery()
                    while (resultSet.next()) {
                        val name = resultSet.getString(2).toString()
                        val surname = resultSet.getString(3).toString()
                        val patronymic = resultSet.getString(4).toString()
                        val phone = resultSet.getString(6).toString()
                        val email = resultSet.getString(5).toString()
                        runOnUiThread {
                            Fname.setText(name)
                            Fsurname.setText(surname)
                            Fpatronymic.setText(patronymic)
                            Fphone.setText(phone)
                            Femail.setText(email)
                        }

                        if (resultSet.getString(10) != null) {
                            val date = resultSet.getDate(10)
                            val dateFormat = SimpleDateFormat("dd.MM.yyyy")
                            runOnUiThread {
                                FDate.setText(dateFormat.format(date))
                            }
                        }

                    }
                    fillSQL.close()
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@ProfilePersonalInfo,
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


        Apply.setOnClickListener {
            var city = 0
            var FieldsFull = false
            var EmailValid = false
            var PhoneValid = false

            var date: Date? =
                SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse("01.01.1910")

            val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val selectedDate = FDate.text.toString()
            try {
                date = formatter.parse(selectedDate)
            } catch (e: ParseException) {
                e.printStackTrace()
            }

            // Все ли поля заполнены
            if ((Fname.text.isEmpty() || Fsurname.text.isEmpty() || Fpatronymic.text.isEmpty() || Fphone.text.isEmpty() || Femail.text.isEmpty())
                || cities_list.selectedItemPosition == AdapterView.INVALID_POSITION || FDate.text.isEmpty() || cities_list.selectedItemPosition == 0
            ) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            } else {
                FieldsFull = true
            }

            // Правильно ли введён E-Mail
            if (EmailValidator.isValidEmail(Femail.text.toString())) {
                EmailValid = true
            } else {
                Toast.makeText(this, "Почта введена неверно", Toast.LENGTH_SHORT).show()
            }

            // Правильно ли введён телефон
            if (Fphone.text.isNotEmpty()) {
                if (Fphone.text.length == 10) {
                    PhoneValid = true
                } else {
                    Toast.makeText(this, "Номер телефона введен неверно", Toast.LENGTH_SHORT).show()
                }
            }

            if (FieldsFull && EmailValid && PhoneValid) {
                lifecycleScope.launch(Dispatchers.IO) {
                    var connect: Connection? = null
                    try {
                        val connector = DBConnector()
                        connect = connector.getConnection()

                        if (connect != null) {
                            val statement: Statement = connect!!.createStatement()
                            // Есть ли такая почта в базе данных
                            val getSQL_mail =
                                statement.executeQuery("SELECT \"E-Mail\", \"User_ID\" FROM Пользователи")

                            while (getSQL_mail.next()) {
                                val mail = getSQL_mail.getString(1)
                                val checkid = getSQL_mail.getInt(2)
                                if (mail == Femail.text.toString() && checkid != userid) {
                                    runOnUiThread {
                                        Toast.makeText(
                                            this@ProfilePersonalInfo,
                                            "Почта занята другим пользователем",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    return@launch
                                }
                            }
                            //Закрытие запроса
                            getSQL_mail.close()


                            //SQL - запрос для сопоставления ID города
                            val getSQL_city =
                                statement.executeQuery("SELECT \"City_ID\" FROM Города")

                            while (getSQL_city.next()) {
                                city = getSQL_city.getInt(1)
                                if (city == cities_list.selectedItemPosition) {
                                    break
                                }
                            }

                            //Закрытие запроса
                            getSQL_city.close()

                            //SQL - запрос для внесения данных нового пользователя
                            val ChangeSQL: PreparedStatement = connect!!.prepareStatement(
                                "UPDATE Пользователи " +
                                        "SET Имя = ?, Фамилия = ?, Отчество = ?, Телефон = ?, \"E-Mail\" = ?, \"Дата рождения\" = ?, \"City_ID\" = ? " +
                                        "WHERE \"User_ID\" = ?;"
                            )
                            ChangeSQL.setString(1, Fname.text.toString())
                            ChangeSQL.setString(2, Fsurname.text.toString())
                            ChangeSQL.setString(3, Fpatronymic.text.toString())
                            ChangeSQL.setString(4, Fphone.text.toString())
                            ChangeSQL.setString(5, Femail.text.toString())
                            ChangeSQL.setDate(6, java.sql.Date(date!!.time))
                            ChangeSQL.setInt(7, city)
                            ChangeSQL.setInt(8, userid)

                            ChangeSQL.executeUpdate()
                            runOnUiThread {
                                Toast.makeText(this@ProfilePersonalInfo, "Успешно", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this@ProfilePersonalInfo, ProfileSettings::class.java)
                                startActivity(intent)
                            }

                            //Закрытие запроса
                            ChangeSQL.close()

                        } else {
                            runOnUiThread {
                                Toast.makeText(
                                    this@ProfilePersonalInfo,
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
            }
        }
    }




    // Проврека валидности почти
    object EmailValidator {
        private const val EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@" +
                "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$"
        private val pattern: Pattern = Pattern.compile(EMAIL_PATTERN)
        fun isValidEmail(email: String?): Boolean {
            val matcher: Matcher = pattern.matcher(email)
            return matcher.matches()
        }
    }


    //Диалоговое окно ввода даты
    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, monthOfYear, dayOfMonth ->
                // При выборе даты обновить текст в EditText
                val selectedDate = "$dayOfMonth.${monthOfYear + 1}.$year" // добавляем 1 к monthOfYear, так как месяцы считаются с 0
                FDate.setText(selectedDate)
            },
            year,
            month,
            dayOfMonth
        )
        // Установка минимальной даты
        val minCalendar = Calendar.getInstance()
        minCalendar.set(1910, Calendar.JANUARY, 1)
        datePickerDialog.datePicker.minDate = minCalendar.timeInMillis

        // Установка максимальной даты
        val maxCalendar = Calendar.getInstance()
        maxCalendar.add(Calendar.YEAR, -16)
        datePickerDialog.datePicker.maxDate = maxCalendar.timeInMillis
        datePickerDialog.show()
    }
}