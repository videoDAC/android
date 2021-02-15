package com.videodac.publisher.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.videodac.publisher.R
import com.videodac.publisher.databinding.InfoHeaderBinding
import com.videodac.publisher.databinding.InfoScreenBinding
import com.videodac.publisher.helpers.Utils.walletPrivateKey
import com.videodac.publisher.helpers.Utils.walletAddress


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

        // setup the instructions
        setupInstructions()
    }

    // setup top toolbar icons
    // setup custom toolbar with icons
    private fun setupActionBar() {

        infoHeaderBinding.homeBtn.setOnClickListener {
            startActivity(Intent(this@InfoActivity, StreamingActivity::class.java))
            finish()
        }

    }


    // setup the instructions
    private fun setupInstructions(){

        val str = SpannableStringBuilder(getString(R.string.info_instruction_copy))
        str.setSpan(
            StyleSpan(Typeface.ITALIC),
            30,
            40,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        infoScreenBinding.instruction1.text = str

        // private key
        infoScreenBinding.copyPrivateKey.paintFlags = infoScreenBinding.copyPrivateKey.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        infoScreenBinding.copyPrivateKey.setOnClickListener {
            copyToClipboard("Private Key", walletPrivateKey)
        }

        // public key
        infoScreenBinding.copyAddress.paintFlags = infoScreenBinding.copyAddress.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        infoScreenBinding.copyAddress.setOnClickListener {
            copyToClipboard("Wallet Address", walletAddress)
        }
    }


    private fun copyToClipboard(label: String, key: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, key)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(this, "$label copied to clipboard!",Toast.LENGTH_LONG).show()
    }

}