package com.vivt.vvolunteer.event

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class EventFinishActivity : AppCompatActivity(){


    val uries = mutableListOf<Uri>()
    var selectedButton = 0
    var changeList = Uri.parse("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_finish)

        val eventid = intent.getStringExtra("eventid")



        val Tlb: androidx.appcompat.widget.Toolbar = findViewById(R.id.evReportTlb)
        setSupportActionBar(Tlb)
        getSupportActionBar()?.setTitle("Отчёт")
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true)



        val image1: ImageView = findViewById(R.id.evReportIm1)
        val image2: ImageView = findViewById(R.id.evReportIm2)
        val image3: ImageView = findViewById(R.id.evReportIm3)
        val image4: ImageView = findViewById(R.id.evReportIm4)
        val image5: ImageView = findViewById(R.id.evReportIm5)
        val image6: ImageView = findViewById(R.id.evReportIm6)
        val image7: ImageView = findViewById(R.id.evReportIm7)
        val image8: ImageView = findViewById(R.id.evReportIm8)
        val image9: ImageView = findViewById(R.id.evReportIm9)


        // Проверка на null для избежания NullPointerException
        val images = arrayOf(image1, image2, image3, image4, image5, image6, image7, image8, image9)
        val imagesURL = arrayListOf<String?>()
        var nextImage = image1
        for (i in images.indices) {
            if (i < images.size - 1) {
                images[i + 1].visibility = ImageView.INVISIBLE
            }
            images[i].setOnClickListener {
                selectedButton = i
                UploadImage()
                lifecycleScope.launch(Dispatchers.IO) {
                    if (i != images.size - 1) {
                        nextImage = images[i + 1]
                    }
                    if (uries.size == 0) {
                        while (uries.size == 0) {
                            continue
                        }
                        runOnUiThread {
                            Picasso.get().load(uries[i]).into(images[i])
                        // Проверка, что текущий элемент не последний в массиве
                            if (nextImage.visibility != ImageView.VISIBLE) {
                            // Обращение к следующему элементу массива
                                nextImage.visibility = ImageView.VISIBLE
                            }
                        }

                    } else if ((nextImage.visibility == ImageView.VISIBLE && i != 8) ||
                        (nextImage.visibility == ImageView.VISIBLE && i == 8 && uries.size == 9)) {
                        changeList = uries[i]
                        while (changeList == uries[i]) {
                            continue
                        }
                        runOnUiThread {
                            Picasso.get().load(uries[i]).into(images[i])
                        }

                    } else if (nextImage.visibility != ImageView.VISIBLE || i == 8) {
                        while (uries.size == selectedButton) {
                            continue
                        }
                        runOnUiThread {
                            Picasso.get().load(uries[i]).into(images[i])
                            nextImage.visibility = ImageView.VISIBLE
                        }
            }

                }




            }
        }

/*
        report = findViewById(R.id.evReportImageGrid)
        report.layoutManager = GridLayoutManager(this, 4)
        report.adapter = ReportAdapter(reports, this)
*/


        val applyBtn: Button = findViewById(R.id.evReportApply)
        applyBtn.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                for (i in uries.indices) {
                    imagesURL.add(FileToFTP(uries[i]))
                }
                Apply(eventid,imagesURL)
                runOnUiThread{
                    val intent = Intent(this@EventFinishActivity, MainActivity::class.java)
                    startActivity(intent)
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

    private suspend fun Apply(eventid: String?, URLs: ArrayList<String?>) {
        var connect: Connection? = null
        try {
            val connector = DBConnector()
            connect = connector.getConnection()
            if (connect != null) {
                val updatesql: PreparedStatement = connect.prepareStatement("UPDATE Пользователи " +
                        "SET Рейтинг = Рейтинг + 50 " +
                        "WHERE \"User_ID\" IN " +
                        "(SELECT Пользователи.\"User_ID\" " +
                        "FROM Пользователи, Запросы " +
                        "WHERE Запросы.\"Event_ID\" = ? " +
                        "AND Пользователи.\"User_ID\" = Запросы.\"User_ID\")")
                if (eventid != null) {
                    updatesql.setInt(1, eventid.toInt())
                    updatesql.executeUpdate()
                }
                updatesql.close()

                val imageSQL: PreparedStatement = connect.prepareStatement("INSERT INTO Отчёты (\"Event_ID\") VALUES (?);")
                imageSQL.setInt(1, eventid!!.toInt())
                imageSQL.executeUpdate()
                imageSQL.close()

                for (i in URLs.indices) {
                    val updateimageSQL: PreparedStatement = connect.prepareStatement("UPDATE Отчёты " +
                            "SET \"Отчёт_Фото\" = array_append(\"Отчёт_Фото\", ?) " +
                            "WHERE \"Event_ID\" = ?")
                    updateimageSQL.setString(1, URLs[i])
                    updateimageSQL.setInt(2, eventid.toInt())
                    updateimageSQL.executeUpdate()
                    updateimageSQL.close()
                }

                val statusSQL:  PreparedStatement = connect.prepareStatement("UPDATE События " +
                        "SET Завершено = 1" +
                        "WHERE \"Event_ID\" = ?")
                statusSQL.setInt(1, eventid.toInt())
                statusSQL.executeUpdate()
                statusSQL.close()

            } else {
                runOnUiThread {
                    Toast.makeText(
                        this@EventFinishActivity,
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

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // Обработка выбранного изображения
        uri?.let {

           /* lifecycleScope.launch(Dispatchers.IO) {
                imageURL = FileToFTP(it)
            }*/
            if (selectedButton < uries.size) {
                uries[selectedButton] = it
            } else {
                uries.add(it)
            }
        }
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
                val tempFile = java.io.File.createTempFile("image", ".$extension", cacheDir)
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