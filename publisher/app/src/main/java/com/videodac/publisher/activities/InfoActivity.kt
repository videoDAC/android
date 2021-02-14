package com.videodac.publisher.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import com.videodac.publisher.databinding.InfoHeaderBinding
import com.videodac.publisher.databinding.InfoScreenBinding

class InfoActivity: AppCompatActivity() {

    private lateinit var infoScreenBinding: InfoScreenBinding
    private lateinit var infoHeaderBinding: InfoHeaderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // inflate layout
        infoScreenBinding = InfoScreenBinding.inflate(layoutInflater)
        infoHeaderBinding = InfoHeaderBinding.bind(infoScreenBinding.root)

        setContentView(infoScreenBinding.root)

        // setup the toolbar icon
        setupActionBar()
    }

    // setup top toolbar icons
    // setup custom toolbar with icons
    private fun setupActionBar() {

        infoHeaderBinding.homeBtn.setOnClickListener {
            startActivity(Intent(this@InfoActivity, StreamingActivity::class.java))
            finish()
        }

    }
    private fun exportPublicKey(){

    }

    private fun exportPrivateKey(){

    }

}