package com.vivt.vvolunteer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.vivt.vvolunteer.authorize.AuthActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val auth = getSharedPreferences("logined", Context.MODE_PRIVATE)

        //Проверка авторизации
        if (auth.getString("Allow", "0") != "1") {
            val intent = Intent(this, AuthActivity::class.java)
            startActivity(intent)
        } else {
            val btmNavView = findViewById<BottomNavigationView>(R.id.btmNavView)
            val controller = findNavController(R.id.fragContView)
            btmNavView.setupWithNavController(controller)
        }



    }
}