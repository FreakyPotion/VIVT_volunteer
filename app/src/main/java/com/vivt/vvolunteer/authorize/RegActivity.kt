package com.vivt.vvolunteer.authorize

import android.content.Intent
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vivt.vvolunteer.DBFactory.DBConnector
import com.vivt.vvolunteer.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Statement
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.app.DatePickerDialog
import java.text.SimpleDateFormat
import java.text.ParseException
import java.util.*


class RegActivity : AppCompatActivity() {

    private lateinit var FDate: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reg)

        // Выпадающий список
        val cities_list: Spinner = findViewById(R.id.regCityList)
        val list: Array<String> = resources.getStringArray(R.array.cities)

        //Адаптер для списка
        val adapter: ArrayAdapter<String> = ArrayAdapter(this, android.R.layout.simple_spinner_item, list)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        cities_list.adapter = adapter
        // Адаптер для списка End
        // Выпадающий список End

        // ROLE
        val VRButton: RadioButton = findViewById(R.id.regVRButton)
        val ORButton: RadioButton = findViewById(R.id.regORButton)
        var role: Int = 1


        VRButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                ORButton.isChecked = false
                role = 1
            }
        }

        ORButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                VRButton.isChecked = false
                role = 0
            }
        }
        // Radio Buttons End

        // ЗНАЧЕНИЯ ПОЛЕЙ И ПОЛЯ

        val Fname: EditText = findViewById(R.id.regNameTextView)
        val Fsurname: EditText = findViewById(R.id.regSurnameTextView)
        val Fpatronymic: EditText = findViewById(R.id.regPatronymicTextView)
        val Fphone: EditText = findViewById(R.id.regPhoneTextView)
        val Femail: EditText = findViewById(R.id.regEmailTextView)
        val Fpw: EditText = findViewById(R.id.regPass)
        FDate = findViewById(R.id.regDate)

        FDate.setOnClickListener {
            showDatePickerDialog()
        }



        // Кнопка регистрации

        val reg: Button = findViewById(R.id.regDone)
        reg.setOnClickListener {

            // Стартовые поля и их определения
            val name: String = Fname.text.toString().trim()
            val surname: String = Fsurname.text.toString().trim()
            val patronymic: String = Fpatronymic.text.toString().trim()
            val phone: String = Fphone.text.toString().trim()
            val email: String = Femail.text.toString().trim()
            val pw: String = Fpw.text.toString().trim()

            var date: Date? = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse("01.01.1910")

            val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val selectedDate = FDate.text.toString()
            try {
                date = formatter.parse(selectedDate)
            } catch (e: ParseException) {
                e.printStackTrace()
            }

            var city = 0
            var FieldsFull = false
            var EmailValid = false
            var PhoneValid = false
            var id = 0

            // Все ли поля заполнены
            if ((name.isEmpty() || surname.isEmpty()
                        || patronymic.isEmpty() || phone.isEmpty()
                        || email.isEmpty() || pw.isEmpty())
                        || selectedDate.isEmpty()
                        || cities_list.selectedItemPosition == AdapterView.INVALID_POSITION
                        || cities_list.selectedItemPosition == 0
                        || (!VRButton.isChecked && !ORButton.isChecked))
            {
                Toast.makeText(this,"Заполните все поля и выберите роль", Toast.LENGTH_SHORT).show()
            }
            else {
                FieldsFull = true
            }


            // Правильно ли введён E-Mail
            if (EmailValidator.isValidEmail(email)){
                EmailValid = true
            } else {
                Toast.makeText(this,"Почта введена неверно", Toast.LENGTH_SHORT).show()
            }



            // Правильно ли введён телефон
            if (phone.isNotEmpty()) {
                if  (phone.length == 10) {
                    PhoneValid = true
                } else {
                    Toast.makeText(this,"Номер телефона введен неверно", Toast.LENGTH_SHORT).show()
                }
            }

            // Если всё заполнено верно - переход к работе с SQL

            // Внесение данных в БД и обработка исключений
            if (FieldsFull && EmailValid && PhoneValid)
            {
                var connect: Connection? = null
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val connector = DBConnector()
                        connect = connector.getConnection()

                        if (connect != null) {
                            val statement: Statement = connect!!.createStatement()
                            // Есть ли такая почта в базе данных
                            val getSQL_mail = statement.executeQuery("SELECT \"E-Mail\" FROM Пользователи")

                            while (getSQL_mail.next()) {
                                val mail = getSQL_mail.getString(1)
                                if (mail == email) {
                                    runOnUiThread {
                                        Toast.makeText(this@RegActivity, "Пользователь с таким почтовым адресом уже существует", Toast.LENGTH_SHORT).show()
                                    }
                                    return@launch
                                }
                            }
                            //Закрытие запроса
                            getSQL_mail.close()


                            //SQL - запрос для сопоставления ID города
                            val getSQL_city = statement.executeQuery("SELECT \"City_ID\" FROM Города")

                            while (getSQL_city.next()) {
                                city = getSQL_city.getInt(1)
                                if (city == cities_list.selectedItemPosition) {
                                    break
                                }
                            }

                            //Закрытие запроса
                            getSQL_city.close()

                            //SQL - запрос для получения ID пользователя
                            val getSQL_userid = statement.executeQuery("SELECT * FROM Пользователи ORDER BY \"User_ID\" ASC")

                            while (getSQL_userid.next()) {
                                id += 1
                            }

                            //Закрытие запроса
                            getSQL_userid.close()

                            //SQL - запрос для внесения данных нового пользователя
                            val RegSQL: PreparedStatement = connect!!.prepareStatement(
                                "INSERT INTO Пользователи " +
                                        "(\"User_ID\", Имя, Фамилия, Отчество, Телефон, \"E-Mail\", Пароль, \"Дата рождения\", \"Role_ID\", \"City_ID\") " +
                                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
                            )
                            RegSQL.setInt(1,id+1)
                            RegSQL.setString(2, name)
                            RegSQL.setString(3, surname)
                            RegSQL.setString(4, patronymic)
                            RegSQL.setString(5, phone)
                            RegSQL.setString(6, email)
                            RegSQL.setString(7, pw)
                            RegSQL.setDate(8,java.sql.Date(date!!.time))
                            RegSQL.setInt(9, role)
                            RegSQL.setInt(10, city)

                            val rowsAffected = RegSQL.executeUpdate()

                            if (rowsAffected > 0) {
                                runOnUiThread {
                                    Toast.makeText(this@RegActivity, "Успешная регистрация", Toast.LENGTH_SHORT).show()
                                }
                                val intent = Intent(this@RegActivity, AuthActivity::class.java)
                                startActivity(intent)
                            } else {
                                // В случае неудачного выполнения запроса
                                runOnUiThread {
                                    Toast.makeText(this@RegActivity, "Ошибка при регистрации", Toast.LENGTH_SHORT).show()
                                }
                            }

                            //Закрытие запроса
                            RegSQL.close()

                        } else {
                            runOnUiThread {
                                Toast.makeText(
                                    this@RegActivity,
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
        // Кнопка регистрации END


        val regTlb: androidx.appcompat.widget.Toolbar = findViewById(R.id.regTlb)
        setSupportActionBar(regTlb)
        getSupportActionBar()?.setTitle("Регистрация")
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true)

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