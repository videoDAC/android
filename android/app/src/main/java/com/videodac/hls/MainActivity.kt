package com.videodac.hls

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.RelativeLayout

import androidx.appcompat.app.AppCompatActivity

import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoListener

import kotlinx.android.synthetic.main.video_layout.*

class MainActivity : AppCompatActivity() {

    private lateinit var player: SimpleExoPlayer
    private lateinit var mediaDataSourceFactory: DataSource.Factory
    private var fullscreen = false

    private var PRIVATE_MODE = 0
    private val PREF_NAME = "video-dac-wallet"
    private lateinit var sharedPref: SharedPreferences

    companion object {
        const val STREAM_URL = "http://159.100.251.158:8935/stream/0xdac817294c0c87ca4fa1895ef4b972eade99f2fd.m3u8"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.video_layout)
        sharedPref = getSharedPreferences(PREF_NAME, PRIVATE_MODE)
    }

    private fun initializePlayer() {
        player = ExoPlayerFactory.newSimpleInstance(this)
        mediaDataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, "video_dac"))

        val mediaSource = HlsMediaSource.Factory(mediaDataSourceFactory).createMediaSource(Uri.parse(STREAM_URL))

        with(player, {
            prepare(mediaSource, false, false)
            playWhenReady = true
            addVideoListener(object : VideoListener {
                override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                    initView.visibility = View.GONE
                    Log.d("VIDEO_DAC", String.format("play video listener video size changed, WIDTH IS %1\$2s , HEIGHT IS %2\$2s", width.toString(), height.toString()))

                    when {
                        width > height -> goFullScreen("landscape")
                        width <= height -> goFullScreen("portrait")
                    }
                }

                override fun onRenderedFirstFrame() {
                    Log.d("VIDEO_DAC", String.format("First frame rendered"))
                }
            })

        })

        playerView.setShutterBackgroundColor(Color.TRANSPARENT)
        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        playerView.player = player
        playerView.requestFocus()

        with(playerView, {
            setOnClickListener {
                finish()
            }
        })

    }

    @SuppressLint("DefaultLocale", "InlinedApi")
    private fun goFullScreen(orientation:String) {

        if ( orientation.toLowerCase() == "portrait" ) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        if ( orientation.toLowerCase() == "landscape" ) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        val params = playerView.layoutParams as RelativeLayout.LayoutParams
        params.width = ViewGroup.LayoutParams.MATCH_PARENT
        params.height = ViewGroup.LayoutParams.MATCH_PARENT
        playerView.layoutParams = params
        fullscreen = true
    }

    private fun releasePlayer() {
        player.release()
    }

    public override fun onResume() {
        super.onResume()
        val walletCreated = sharedPref.getBoolean("walletCreated", false)
        if (walletCreated) {
            initializePlayer()
        } else {
            val walletIntent = Intent(this, WalletActivity::class.java)
            startActivity(walletIntent)
            finish()
        }
    }

    public override fun onPause() {
        super.onPause()
        if (::player.isInitialized) {
            releasePlayer()
        }
    }

    public override fun onStop() {
        super.onStop()
        if (::player.isInitialized) {
            releasePlayer()
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        if (::player.isInitialized) {
            releasePlayer()
        }
    }
}