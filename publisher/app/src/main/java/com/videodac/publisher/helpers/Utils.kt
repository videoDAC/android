package com.videodac.publisher.helpers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.videodac.publisher.R

import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import java.math.BigDecimal


object Utils {

    internal const val WALLET_CREATED = "WALLET_CREATED"
    internal const val WALLET_PATH = "WALLET_PATH"
    internal const val CHANNEL_ADDRESS = "CHANNEL_ADDRESS"
    internal var streamingFeeInEth = 0.0
    internal var walletBalanceLeft = BigDecimal(0)
    internal const val walletPassword = "password"

    internal var walletPublicKey: String = ""



    @JvmStatic
    internal fun getWeb3(context: Context) = Web3j.build(HttpService(context.getString(R.string.infura_url)))

    @JvmStatic
    internal fun copyToClipboard(activity: AppCompatActivity, walletInfo: String, walletInfoType: String,) {
        if (walletInfo.isNotEmpty()) {
            val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(walletInfoType, walletInfo)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(activity, "$walletInfoType copied to clipboard", Toast.LENGTH_LONG).show()
        }
    }


}
