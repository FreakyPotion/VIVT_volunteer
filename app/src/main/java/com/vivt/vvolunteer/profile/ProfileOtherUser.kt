package com.vivt.vvolunteer.profile

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.squareup.picasso.Picasso
import com.vivt.vvolunteer.R


class ProfileOtherUser : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_other_user)

        val Fname: TextView = findViewById(R.id.profileName)
        val Fbirthday: TextView = findViewById(R.id.profileDate)
        val Fphone: TextView = findViewById(R.id.profilePhone)
        val Femail: TextView = findViewById(R.id.profileEmail)
        val Image: ImageView = findViewById(R.id.profile_image)
        val Rating: TextView = findViewById(R.id.profileRateLevel)
        val Fcity: TextView = findViewById(R.id.profileCity)

        val progress: ProgressBar = findViewById(R.id.progressBar)
        val progressValue: TextView = findViewById(R.id.progressValue)

        val userid = intent.getStringExtra("userid")

        val name = intent.getStringExtra("userName") + " " + intent.getStringExtra("userSurname")
        Fname.text = name
        Fbirthday.text = intent.getStringExtra("userDate")
        Fphone.text = intent.getStringExtra("userPhone")
        Femail.text = intent.getStringExtra("userEmail")
        Fcity.text = intent.getStringExtra("userCity")
        val Level = intent.getIntExtra("userRate",0)
        val ratetext = "Уровень" + " " + (Level.div(1000) + 1).toString()
        Rating.text = ratetext
        progress.progress = Level.mod(1000)
        progressValue.text = Level.mod(1000).toString()
        val imageURL = intent.getStringExtra("userImage")
        Picasso.get().load(imageURL).into(Image)


        val othUsTlb: androidx.appcompat.widget.Toolbar = findViewById(R.id.othUsTlb)
        setSupportActionBar(othUsTlb)
        getSupportActionBar()?.setTitle(name)
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {

                val Title: String?
                val Adress: String?
                val Date: String?
                val Dir: String?
                val Org: String?
                val MaxParticipants: String?
                val Desc: String?
                val Image: String?

                //Берём данные из события
                val eventid = intent.getStringExtra("eventid")
                Title = intent.getStringExtra("prevTitle")
                Adress = intent.getStringExtra("prevAdress")
                Date = intent.getStringExtra("prevDate")
                Dir = intent.getStringExtra("prevDir")
                Org= intent.getStringExtra("prevOrg")
                MaxParticipants= intent.getStringExtra("prevMaxP")
                Desc = intent.getStringExtra("prevDesc")
                Image = intent.getStringExtra("prevImage")

                //Возвращаем данные в событие
                val intent = Intent()
                intent.putExtra("eventid", eventid)
                intent.putExtra("prevTitle", Title)
                intent.putExtra("prevDate", Adress)
                intent.putExtra("prevAdress",Date)
                intent.putExtra("prevDir", Dir)
                intent.putExtra("prevOrg", Org)
                intent.putExtra("prevMaxP",MaxParticipants)
                intent.putExtra("prevDesc", Desc)
                intent.putExtra("prevImage",Image)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}