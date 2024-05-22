package com.vivt.vvolunteer.profile

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.vivt.vvolunteer.DBFactory.DBConnector
import com.vivt.vvolunteer.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException

class ProfileSecurity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_security)

        val Tlb: androidx.appcompat.widget.Toolbar = findViewById(R.id.securTlb)
        setSupportActionBar(Tlb)
        getSupportActionBar()?.setTitle("Безопасность")
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true)

        var user = getSharedPreferences("logined", Context.MODE_PRIVATE)
        val userid = user.getInt("User_ID",0)

        val OldPass: EditText = findViewById(R.id.profileOldPass)
        val NewPass: EditText = findViewById(R.id.profileNewPass)
        val NewPassConfirm: EditText = findViewById(R.id.profileNewPassConfirm)
        val Apply: Button = findViewById(R.id.profileSecurityApply)

        Apply.setOnClickListener {
            var FieldsFull = false
            var PassConfirm = false

            // Все ли поля заполнены
            if (OldPass.text.isEmpty() || NewPass.text.isEmpty() || NewPassConfirm.text.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            } else {
                FieldsFull = true
            }

            // Совпадает ли новый пароль с проверкой
            if (NewPass.text.toString() != NewPassConfirm.text.toString()) {
                Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show()
            } else {
                PassConfirm = true
            }

            if (FieldsFull && PassConfirm) {
                var forCheck = ""
                lifecycleScope.launch(Dispatchers.IO) {
                    var connect: Connection? = null
                    try {
                        val connector = DBConnector()
                        connect = connector.getConnection()
                        if (connect != null) {
                            val checkOldPassSQL: PreparedStatement = connect.prepareStatement("SELECT Пароль FROM Пользователи WHERE \"User_ID\" = ?;")
                            checkOldPassSQL.setInt(1, userid)
                            val resultSet = checkOldPassSQL.executeQuery()

                            while (resultSet.next()) {
                                forCheck = resultSet.getString(1)
                            }

                            if (forCheck == OldPass.text.toString()) {
                                val ChangeSQL: PreparedStatement = connect.prepareStatement("UPDATE Пользователи SET Пароль = ? WHERE \"User_ID\" = ?;")
                                ChangeSQL.setString(1, NewPass.text.toString())
                                ChangeSQL.setInt(2,userid)
                                ChangeSQL.executeUpdate()
                                runOnUiThread {
                                    Toast.makeText(this@ProfileSecurity, "Успешно", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this@ProfileSecurity, ProfileSettings::class.java)
                                    startActivity(intent)
                                }
                            } else {
                                runOnUiThread {
                                    Toast.makeText(this@ProfileSecurity, "Старый пароль введен неверно", Toast.LENGTH_SHORT).show()
                                }
                            }

                        }
                        else {
                            runOnUiThread {
                                Toast.makeText(this@ProfileSecurity, "Сервер не отвечает", Toast.LENGTH_SHORT).show()
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
}