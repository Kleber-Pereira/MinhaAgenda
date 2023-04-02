package com.teste.minhaagenda

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    var customCalendarView: CustomCalendarView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        customCalendarView = findViewById<View>(R.id.custom_calendar_view) as CustomCalendarView
    }
}