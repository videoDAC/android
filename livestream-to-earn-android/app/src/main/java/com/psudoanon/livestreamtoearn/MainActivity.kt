package com.psudoanon.livestreamtoearn

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager

class MainActivity : AppCompatActivity() {

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


//        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        val intent = Intent(this, VideoBroadcastActivity::class.java)
        startActivity(intent)
    }
}
