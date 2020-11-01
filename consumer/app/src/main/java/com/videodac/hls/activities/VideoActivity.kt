package com.videodac.hls.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoListener
import com.videodac.hls.R
import com.videodac.hls.helpers.Utils.CHANNEL_ADDRESS
import com.videodac.hls.helpers.Utils.WALLET_PATH
import com.videodac.hls.helpers.Utils.closeActivity
import com.videodac.hls.helpers.Utils.goFullScreen
import com.videodac.hls.helpers.Utils.streamingFeeInEth
import com.videodac.hls.helpers.Utils.walletBalanceLeft
import com.videodac.hls.helpers.Utils.walletPassword
import com.videodac.hls.helpers.Utils.walletPublicKey
import com.videodac.hls.helpers.WebThreeHelper.web3
import kotlinx.android.synthetic.main.video.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.tx.Transfer
import org.web3j.utils.Convert
import org.web3j.utils.Convert.Unit
import java.io.IOException
import java.math.BigDecimal


class VideoActivity : AppCompatActivity() {

    private lateinit var player: SimpleExoPlayer

    // shared preferences
    private var PRIVATE_MODE = 0
    private val PREF_NAME = "video-dac-video_dac_wallet"
    private lateinit var sharedPref: SharedPreferences
    private val TAG = "VIDEO_DAC_WALLET"

    // recipientAddress
    lateinit var recipientAddress: String

    // stream funds every second
    private val loopDelay = 60000L


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.video)
        goFullScreen(this)
        sharedPref = getSharedPreferences(PREF_NAME, PRIVATE_MODE)
    }

    private fun initializePlayer() {
        player = ExoPlayerFactory.newSimpleInstance(this)
        val factory = DefaultDataSourceFactory(this, Util.getUserAgent(this, "Exo Player"))

        recipientAddress = intent.extras!!.getString(CHANNEL_ADDRESS)!!
        val channelUrl = getString(R.string.streaming_url, recipientAddress)


        val mediaSource = HlsMediaSource.Factory(factory).createMediaSource(Uri.parse(channelUrl))

        with(player, {
            prepare(mediaSource, false, false)
            playWhenReady = true
            addVideoListener(object : VideoListener {
                override fun onVideoSizeChanged(
                    width: Int,
                    height: Int,
                    unappliedRotationDegrees: Int,
                    pixelWidthHeightRatio: Float
                ) {
                    Log.d(
                        "VIDEO_DAC", String.format(
                            "play video listener video size changed, WIDTH IS %1\$2s , HEIGHT IS %2\$2s",
                            width.toString(),
                            height.toString()
                        )
                    )
                }

                override fun onRenderedFirstFrame() {
                    showPlayer()
                    Log.d("VIDEO_DAC", String.format("First frame rendered"))
                }
            })

        })

        playerView.setShutterBackgroundColor(Color.TRANSPARENT)
        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        playerView.player = player
        playerView.requestFocus()

        with(rootVideoView, {
            setOnClickListener {
                closeActivity(this@VideoActivity, walletPublicKey)
            }
        })

    }

    private fun showPlayer() {
        loading_text.visibility = View.GONE
        loader.visibility = View.GONE
        loader_view.visibility = View.GONE
        playerView.visibility = View.VISIBLE
    }

    @SuppressLint("SetTextI18n")
    private fun payStreamingFee() {

        // set the initial balance
        wallet_balance_left.text = String.format("%.4f ", walletBalanceLeft) + "ETH"

        lifecycleScope.launch(Dispatchers.IO) {

            var streamingFunds = true
            var count = 0

            while(streamingFunds) {

                if(count > 0){
                    delay(loopDelay)
                }

                val clientVersion = web3!!.web3ClientVersion().send()

                try {
                    // open the video_dac_wallet into a Credential object
                    val walletPath = sharedPref.getString(WALLET_PATH, "")
                    val credentials = WalletUtils.loadCredentials(walletPassword, walletPath)

                    val transferReceipt = Transfer.sendFunds(
                        web3, credentials, recipientAddress, BigDecimal.valueOf(
                            streamingFeeInEth
                        ), Unit.ETHER
                    ).send()

                    if(transferReceipt.isStatusOK) {
                        Log.d(TAG, "Streamed $streamingFeeInEth to $recipientAddress")

                        val balanceWei = web3!!.ethGetBalance(
                            credentials.address,
                            DefaultBlockParameterName.LATEST
                        ).send()
                        walletBalanceLeft = Convert.fromWei(
                            balanceWei.balance.toString(),
                            Unit.ETHER
                        )

                        withContext(Dispatchers.Main) {
                            wallet_balance_left.text = String.format("%.4f ", walletBalanceLeft) + " ETH"
                        }

                        // finally stop playing the video if the balance is lesser than the streaming fee
                        if (walletBalanceLeft < BigDecimal.valueOf(streamingFeeInEth)) {
                            streamingFunds = false
                            startActivity(Intent(this@VideoActivity, WalletActivity::class.java))
                            closeActivity(this@VideoActivity, null)
                        }

                    }
                } catch (io: IOException) {
                    Log.e(TAG, io.message!!)
                } catch (ex: InterruptedException) {
                    Log.e(TAG, ex.message!!)
                }
                catch (re: RuntimeException){
                    if (streamingFeeInEth != 0.0) {
                        streamingFunds = false
                        startActivity(Intent(this@VideoActivity, WalletActivity::class.java))
                        closeActivity(this@VideoActivity, null)
                    }

                }


                withContext(Dispatchers.Main) {

                    if (clientVersion.hasError()) {
                        Toast.makeText(
                            this@VideoActivity,
                            "Unable to stream funds to video_dac_wallet",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

              count++

            }

        }

    }

    private fun releasePlayer() {
        player.release()
    }

    public override fun onResume() {
        super.onResume()
        initializePlayer()
        payStreamingFee()
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