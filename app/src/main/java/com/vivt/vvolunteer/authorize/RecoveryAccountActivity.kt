package com.vivt.vvolunteer.authorize

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import com.vivt.vvolunteer.MainActivity
import com.vivt.vvolunteer.R

class RecoveryAccountActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recovery_account)

        val recTlb: androidx.appcompat.widget.Toolbar = findViewById(R.id.recTlb)
        setSupportActionBar(recTlb)
        getSupportActionBar()?.setTitle("Восстановление аккаунта")
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true)
    }
}