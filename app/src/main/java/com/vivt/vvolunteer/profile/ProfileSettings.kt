package com.vivt.vvolunteer.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.vivt.vvolunteer.DBFactory.FTPUploader
import com.vivt.vvolunteer.MainActivity
import com.vivt.vvolunteer.R
import com.vivt.vvolunteer.authorize.AuthActivity
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.vivt.vvolunteer.DBFactory.DBConnector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.sql.Connection
import java.sql.PreparedStatement


class ProfileSettings : AppCompatActivity() {

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // Обработка выбранного изображения
        uri?.let { FileToFTP(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_settings)

        val pId: Button = findViewById(R.id.idButton)
        val pSecurity: Button = findViewById(R.id.securityButton)
        val pExit: Button = findViewById(R.id.exitButton)
        val image: Button = findViewById(R.id.changeImage)

        val Tlb: androidx.appcompat.widget.Toolbar = findViewById(R.id.settingsTlb)
        setSupportActionBar(Tlb)
        getSupportActionBar()?.setTitle("Настройки")
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true)

        pId.setOnClickListener{
            val intent = Intent(this, ProfilePersonalInfo::class.java)
            startActivity(intent)
        }

        pSecurity.setOnClickListener{
            val intent = Intent(this, ProfileSecurity::class.java)
            startActivity(intent)
        }

        pExit.setOnClickListener{
            val logined = getSharedPreferences("logined", MODE_PRIVATE).edit()
            logined.clear()
            logined.apply()
            val intent = Intent(this, AuthActivity::class.java)
            startActivity(intent)
        }

        image.setOnClickListener{
            UploadImage()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("navigate", "menuProfile")
                startActivity(intent)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun UploadImage() {
        // Получаем доступ к файловому менеджеру для выбора изображения
        pickImageLauncher.launch("image/*")
    }

    private fun FileToFTP(uri: Uri) {
        val inputStream = contentResolver.openInputStream(uri)
        inputStream?.let { stream ->
            lifecycleScope.launch(Dispatchers.IO) {
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

                        // Если успешно загружено - подгрузить в БД

                        val userid =  getSharedPreferences("logined", Context.MODE_PRIVATE).getInt("User_ID",0)
                        val connect: Connection?
                        val connector = DBConnector()
                        connect = connector.getConnection()
                        if (connect != null) {
                            val imageSQL: PreparedStatement = connect.prepareStatement("UPDATE Пользователи " +
                                    "SET \"Фото профиля\" = ? " +
                                    "WHERE \"User_ID\" = ?;")
                            if (userid != 0) {
                                imageSQL.setString(1, "http://vivt-volunteer.online.swtest.ru/$uploadedFileName")
                                imageSQL.setInt(2, userid)
                            }
                            imageSQL.executeUpdate()
                            imageSQL.close()
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@ProfileSettings, "Сервер не отвечает", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {

                        // Если не загружено - ошибка

                        runOnUiThread {
                            Toast.makeText(this@ProfileSettings,"Ошибка при загрузке файла", Toast.LENGTH_SHORT).show()
                        }
                    }

                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    inputStream.close()
                }
            }
        }
    }
}