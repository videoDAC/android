package com.videodac.publisher.activities

import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.videodac.publisher.R
import com.videodac.publisher.helpers.TypefaceSpan


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.launch_screen)

        // setup the toolbar icons
        setupToolbarIcons()
        centerActionBarTitle()

    }

    private fun setupToolbarIcons() {
        val actionBar: ActionBar? = supportActionBar
        if (actionBar != null) {

            actionBar.setDisplayHomeAsUpEnabled(true) // switch on the left hand icon
            actionBar.setHomeAsUpIndicator(R.drawable.ic_baseline_dehaze_24) // replace with your custom icon
        }
    }

    /**
     * This method simply centers the textview without using custom layout * for ActionBar.
     */
    private fun centerActionBarTitle() {
        val titleId: Int = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            resources.getIdentifier("action_bar_title", "id", "android")
        } else {
            // This is the id is from your app's generated R class when ActionBarActivity is used
            // for SupportActionBar
            1
        }

        // Final check for non-zero invalid id
        if (titleId > 0) {
            val titleTextView = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                findViewById<View>(titleId) as TextView
            } else {
                val toolbar = findViewById<View>(R.id.action_bar) as Toolbar
                toolbar.getChildAt(0) as TextView
            }

            titleTextView.gravity = Gravity.CENTER
            titleTextView.width = resources.displayMetrics.widthPixels


            supportActionBar!!.title  = SpannableString(getString(R.string.launch_screen_title)).apply {
                setSpan(
                    TypefaceSpan(this@MainActivity, getString(R.string.font_name)), 0, length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }


        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }
}