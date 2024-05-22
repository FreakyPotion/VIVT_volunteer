package com.vivt.vvolunteer.authorize

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vivt.vvolunteer.DBFactory.DBConnector
import com.vivt.vvolunteer.MainActivity
import com.vivt.vvolunteer.R
import java.sql.SQLException
import java.sql.Statement
import java.sql.Connection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)


        val userLogin : EditText = findViewById(R.id.authLogin)
        val userPassword : EditText = findViewById(R.id.authPass)

        // Нажатие на "Войти"
        val button: Button = findViewById(R.id.enterButton)
        button.setOnClickListener {

            val login = userLogin.text.toString().trim()
            val password = userPassword.text.toString().trim()

            if (login.isEmpty() || password.isEmpty()) {
                Toast.makeText(this,"Заполните все поля", Toast.LENGTH_SHORT).show()
            } else {

                // Авторизация (БД)
                lifecycleScope.launch(Dispatchers.IO) {
                    var connect: Connection? = null
                    try {
                        val connector = DBConnector()
                        connect = connector.getConnection()

                        //Если соединение установлено
                        if (connect != null) {
                            //SQL - запрос
                            val statement: Statement = connect!!.createStatement()
                            val AuthSQL = statement.executeQuery("SELECT \"E-Mail\", Пароль, \"User_ID\", \"Role_ID\" FROM Пользователи;")
                            var userFound = false
                            while (AuthSQL.next()) {
                                val dbLogin = AuthSQL.getString(1)
                                val dbPass = AuthSQL.getString(2)
                                val dbUserID = AuthSQL.getInt(3)
                                val dbRoleID = AuthSQL.getInt(4)

                                //Если найдено
                                if (dbLogin == login && dbPass == password) {
                                    runOnUiThread {
                                        Toast.makeText(this@AuthActivity, "Успешная авторизация", Toast.LENGTH_SHORT).show()
                                    }
                                    userFound = true


                                    //Сохранение данных об успешной авторизации
                                    var logined = getSharedPreferences("logined", MODE_PRIVATE).edit()

                                    logined.putString("Allow", "1")
                                    logined.putInt("User_ID", dbUserID)
                                    logined.putInt("Role_ID",dbRoleID)
                                    logined.commit()

                                    // Переход в MainActivity
                                    val intent = Intent(this@AuthActivity, MainActivity::class.java)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivity(intent)
                                    break
                                }
                            }

                            // ЕСЛИ ПОЛЬЗОВАТЕЛЬ НЕ НАЙДЕН
                            if (!userFound) {
                                runOnUiThread {
                                    Toast.makeText(this@AuthActivity, "Пользователь не найден", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        else {
                            runOnUiThread {
                                Toast.makeText(this@AuthActivity, "Сервер не отвечает", Toast.LENGTH_SHORT).show()
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

        // Нажатие на регистрацию
        val regLabel: TextView = findViewById(R.id.authRegLabel)
        regLabel.setOnClickListener {
            val intent = Intent(this, RegActivity::class.java)
            startActivity(intent)
        }

        // Нажатие на "Забыли пароль?"
        val forgotPass: TextView = findViewById(R.id.authForgotPassLabel)
        forgotPass.setOnClickListener {
            val intent = Intent(this, RecoveryAccountActivity::class.java)
            startActivity(intent)
        }
    }

}