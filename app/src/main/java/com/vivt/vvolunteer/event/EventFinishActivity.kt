package com.vivt.vvolunteer.event

import ReportAdapter
import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.media.Image
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.GridView
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.vivt.vvolunteer.DBFactory.DBConnector
import com.vivt.vvolunteer.DBFactory.FTPUploader
import com.vivt.vvolunteer.MainActivity
import com.vivt.vvolunteer.R
import com.vivt.vvolunteer.tables.EventsTable
import com.vivt.vvolunteer.tables.ReportTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.io.IOException
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class EventFinishActivity : AppCompatActivity() {

    var imageURL: String? = ""

    private lateinit var report: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_finish)

        val eventid = intent.getStringExtra("eventid")

        val uries: MutableList<Pair<String, Uri?>> = mutableListOf()
        val reports = arrayListOf<ReportTable>()

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
        for (i in images.indices) {
            if (i < images.size - 1) {
                images[i + 1].visibility = ImageView.INVISIBLE
            }
            images[i].setOnClickListener {
                lifecycleScope.launch(Dispatchers.IO) {
                    UploadImage()
                    imagesURL.add(imageURL)
                    runOnUiThread {
                        Picasso.get()
                            .load("https://cdn0.iconfinder.com/data/icons/simple-lines-filled/32/22_Done_Circle_Complete_Downloaded_Added-1024.png")
                            .into(images[i])
                        // Проверка, что текущий элемент не последний в массиве
                        if (i < images.size - 1) {
                            // Обращение к следующему элементу массива
                            val nextImage = images[i + 1]
                            nextImage.visibility = ImageView.VISIBLE
                        }
                    }
                }
            }
        }

        report = findViewById(R.id.evReportImageGrid)
        report.layoutManager = GridLayoutManager(this, 4)
        report.adapter = ReportAdapter(reports, this, { UploadImage() })


        val applyBtn: Button = findViewById(R.id.evReportApply)
        applyBtn.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {

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

                for (i in URLs.indices) {
                    val imageSQL: PreparedStatement = connect.prepareStatement("UPDATE События " +
                            "SET \"Отчёт_Фото\" = array_append(\"Отчёт_Фото\", ?)" +
                            "WHERE \"Event_ID\" = ?;")
                    imageSQL.setString(1, URLs[i])
                    imageSQL.setInt(2, eventid!!.toInt())
                    imageSQL.executeUpdate()
                    imageSQL.close()
                }

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
            lifecycleScope.launch(Dispatchers.IO) {
                imageURL = FileToFTP(it)
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