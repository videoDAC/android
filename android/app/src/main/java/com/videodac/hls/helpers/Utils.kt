package com.videodac.hls.helpers

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import com.videodac.hls.R
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import java.math.BigDecimal

object Utils {


    internal const val WALLET_CREATED = "WALLET_CREATED"
    internal const val WALLET_PATH = "WALLET_PATH"
    internal const val streamingFeeInEth = 0.0059
    internal const val recipientAddress = "0xdac817294c0c87ca4fa1895ef4b972eade99f2fd"

    internal fun goFullScreen(activity: AppCompatActivity) {
        activity.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        if (Build.VERSION.SDK_INT in 12..18) { // lower api
            val v = activity.window.decorView
            v.systemUiVisibility = View.GONE
        } else if (Build.VERSION.SDK_INT >= 19) { //for new api versions.
            val decorView = activity.window.decorView
            val uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            decorView.systemUiVisibility = uiOptions
        }
    }

    internal fun getWeb3(activity: AppCompatActivity) = Web3j.build(HttpService(activity.getString(R.string.rinkeby_infura_url)))

}