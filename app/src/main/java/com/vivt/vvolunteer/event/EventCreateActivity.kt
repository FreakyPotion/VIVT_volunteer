package com.vivt.vvolunteer.event

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.squareup.picasso.Picasso
import com.vivt.vvolunteer.DBFactory.DBConnector
import com.vivt.vvolunteer.DBFactory.FTPUploader
import com.vivt.vvolunteer.MainActivity
import com.vivt.vvolunteer.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class EventCreateActivity : AppCompatActivity() {

    private lateinit var FDate: EditText

    var imageURL: String? = "https://plat-forma.ru/upload/services/images/24/vivt(d).gif"

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // Обработка выбранного изображения
        uri?.let {
            lifecycleScope.launch(Dispatchers.IO) {
                imageURL = FileToFTP(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_create)

        var user = getSharedPreferences("logined", Context.MODE_PRIVATE)

        val FTitle: EditText = findViewById(R.id.evCTitIText)
        val FMaxPart: EditText = findViewById(R.id.evCMPIText)
        val FDesc: EditText = findViewById(R.id.evCDesIText)
        val FAddr: EditText = findViewById(R.id.evCAdrIText)
        val FImage: ImageView = findViewById(R.id.evCreAddImage)


        FDate = findViewById(R.id.evCDateIText)

        FDate.setOnClickListener {
            showDatePickerDialog()
        }

        FImage.setOnClickListener {
            UploadImage()
        }

        val create: Button = findViewById(R.id.createEventButton)
        create.setOnClickListener {

            val title: String = FTitle.text.toString().trim()
            val description: String = FDesc.text.toString().trim()
            val address: String = FAddr.text.toString().trim()
            val maxpart: String = FMaxPart.text.toString().trim()

            var date: Date? =
                SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse("01.01.1910")

            val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val selectedDate = FDate.text.toString()
            try {
                date = formatter.parse(selectedDate)
            } catch (e: ParseException) {
                e.printStackTrace()
            }

            var FieldsFull = false
            var AddrValid = false
            var id = 0

            // Полнота полей
            if ((title.isEmpty() || description.isEmpty() || address.isEmpty() || maxpart.isEmpty() || selectedDate.isEmpty())) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            } else {
                FieldsFull = true
            }

            // Валидность адреса
            if (isValidAddress(address)) {
                AddrValid = true
            } else {
                Toast.makeText(this, "Адрес введен неверно. Формат \"г. ..., ул. ..., д. ...\"", Toast.LENGTH_SHORT).show()
            }

            // Cоздание события
            if (FieldsFull && AddrValid) {
                lifecycleScope.launch(Dispatchers.IO) {
                    var connect: Connection? = null
                    try {
                        val connector = DBConnector()
                        connect = connector.getConnection()
                        if (connect != null) {
                            //SQL - запрос для получения ID  последнего события
                            val createEventSQL: PreparedStatement = connect.prepareStatement(
                                "INSERT INTO События " +
                                        "(Название, Описание, Дата, Организатор, \"Максимум участников\", Адрес, Изображение)" +
                                        "VALUES (?, ?, ?, ?, ?, ?, ?);"
                            )
                            createEventSQL.setString(1, title)
                            createEventSQL.setString(2, description)
                            createEventSQL.setDate(3, java.sql.Date(date!!.time))
                            createEventSQL.setInt(4, user.getInt("User_ID", 0))
                            createEventSQL.setInt(5, maxpart.toInt())
                            createEventSQL.setString(6, address)
                            createEventSQL.setString(7, imageURL)

                            val rowsAffected = createEventSQL.executeUpdate()

                            if (rowsAffected > 0) {
                                runOnUiThread {
                                    Toast.makeText(
                                        this@EventCreateActivity,
                                        "Событие создано",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                val intent =
                                    Intent(this@EventCreateActivity, MainActivity::class.java)
                                startActivity(intent)
                            } else {
                                // В случае неудачного выполнения запроса
                                runOnUiThread {
                                    Toast.makeText(
                                        this@EventCreateActivity,
                                        "Ошибка при создании события",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }


                            createEventSQL.close()

                        } else {
                            runOnUiThread {
                                Toast.makeText(
                                    this@EventCreateActivity,
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

        val evCreTlb: androidx.appcompat.widget.Toolbar = findViewById(R.id.evCreTlb)
        setSupportActionBar(evCreTlb)
        getSupportActionBar()?.setTitle("Новое событие")
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true)
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

    // Диалоговое окно ввода даты
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
        minCalendar.add(Calendar.DAY_OF_MONTH, 1)
        datePickerDialog.datePicker.minDate = minCalendar.timeInMillis

        // Установка максимальной даты через год от текущей
        val maxCalendar = Calendar.getInstance()
        maxCalendar.add(Calendar.YEAR, 1)
        datePickerDialog.datePicker.maxDate = maxCalendar.timeInMillis

        datePickerDialog.show()
    }


    // Ф-ция проверки почты
    fun isValidAddress(address: String): Boolean {
        val regex = Regex("^г\\. [а-яА-Я0-9,\\s]+, ул\\. [а-яА-Я0-9,\\s]+, д\\. [а-яА-Я0-9,\\s]+\$")
        return regex.matches(address)
    }

    private fun UploadImage() {
        // Получаем доступ к файловому менеджеру для выбора изображения
        pickImageLauncher.launch("image/*")
    }

    private suspend fun FileToFTP(uri: Uri): String = suspendCoroutine { continuation ->
        val inputStream = contentResolver.openInputStream(uri)
        var url = "https://plat-forma.ru/upload/services/images/24/vivt(d).gif"
        inputStream?.let { stream ->
                try {
                    val extension = getContentResolver().getType(uri)?.let { type ->
                        MimeTypeMap.getSingleton().getExtensionFromMimeType(type)
                    } ?: ""
                    val tempFile = createTempFile("image", ".$extension", cacheDir)
                    tempFile.outputStream().use { output ->
                        stream.copyTo(output)
                    }
                    val ftpUploader = FTPUploader()
                    val uploadedFileName = ftpUploader.uploadFile(tempFile, "/public_html")
                    if (uploadedFileName != null) {
                        url = "http://vivt-volunteer.online.swtest.ru/$uploadedFileName"
                    }
                    continuation.resume(url)
                } catch (e: IOException) {
                    e.printStackTrace()
                    continuation.resume(url)
                } finally {
                    inputStream.close()
                }
            }
        }


}