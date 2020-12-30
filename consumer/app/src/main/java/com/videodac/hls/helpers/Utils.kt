package com.videodac.hls.helpers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import com.videodac.hls.R
import kotlinx.coroutines.delay

import org.web3j.crypto.Hash
import org.web3j.ens.EnsResolutionException
import org.web3j.ens.EnsResolver
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Numeric
import java.math.BigDecimal

import java.math.BigInteger
import java.util.*
import java.util.regex.Pattern

object Utils {

    internal const val WALLET_CREATED = "WALLET_CREATED"
    internal const val WALLET_PATH = "WALLET_PATH"
    internal const val CHANNEL_ADDRESS = "CHANNEL_ADDRESS"
    internal var streamingFeeInEth = 0.0
    internal var walletBalanceLeft = BigDecimal(0)
    internal const val walletPassword = "password"

    // we set a default gas price of 40 gwei
    internal var gasPrice = BigInteger.valueOf(40_000_000_000L)
    internal val gasLimit = BigInteger.valueOf(12_500_000L)

    internal var walletPublicKey: String = ""

    // regex patterns
    private val ignoreCaseAddrPattern: Pattern = Pattern.compile("(?i)^(0x)?[0-9a-f]{40}$")
    private val lowerCaseAddrPattern: Pattern = Pattern.compile("^(0x)?[0-9a-f]{40}$")
    private val upperCaseAddrPattern: Pattern = Pattern.compile("^(0x)?[0-9A-F]{40}$")

    @JvmStatic
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

    @JvmStatic
    internal fun getWeb3(context: Context) = Web3j.build(HttpService(context.getString(R.string.infura_url)))

    @JvmStatic
    internal fun closeActivity(activity: AppCompatActivity, userAddress: String?) {
        if (!userAddress.isNullOrEmpty()) {
            val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("User Address", userAddress)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(activity, "Address copied to clipboard" , Toast.LENGTH_LONG).show()
        }
        // finish activity
        activity.finish()
    }

    @JvmStatic
    internal suspend fun resolveChannelENSName(address: String, web3j: Web3j) :String {

        val ens = EnsResolver(web3j)
        var name = ""

        try {
            name = ens.reverseResolve(address)
            // Check to be sure the reverse record is correct.
            val reverseAddress = ens.resolve(name)
            when {
                address.trim().toLowerCase(Locale.ROOT) != reverseAddress.trim().toLowerCase(Locale.ROOT) -> {
                    name = ""
                }
            }

        }
        catch(e: EnsResolutionException){
            println(e.message)
            Log.e("ENS RESOLVER","Node does not Provide an access to a valid public ENS resolver")
        }
        catch(re: RuntimeException) {
            println(re.message)
            Log.e("ENS RESOLVER","Unable to execute Ethereum request")
        }

        return name
    }

    @JvmStatic
    /**
     * Verify that a hex account string is a valid Ethereum address.
     *
     * @param address given address in HEX
     * @return is this a valid address
     */
    internal fun isValidETHAddress(address: String): Boolean {
        /*
         * check basic address requirements, i.e. is not empty and contains
         * the valid number and type of characters
         */
        return if (address.isEmpty() || !ignoreCaseAddrPattern.matcher(address).find()) {
            false
        } else if (lowerCaseAddrPattern.matcher(address).find() || upperCaseAddrPattern.matcher(address).find()) {
            // if it's all small caps or caps return true
            true
        } else {
            // if it is mixed caps it is a checksum address and needs to be validated
            validateChecksumAddress(address)
        }
    }

    private fun validateChecksumAddress(ethAddress: String): Boolean {
        val address = ethAddress.replace("0x", "")
        val hash: String = Numeric.toHexStringNoPrefix(Hash.sha3(address.toLowerCase(Locale.ROOT).toByteArray()))
        for (i in 0..39) {
            if (Character.isLetter(address[i])) {
                // each uppercase letter should correlate with a first bit of 1 in the hash
                // char with the same index, and each lowercase letter with a 0 bit
                val charInt = hash[i].toString().toInt(16)
                when {
                    Character.isUpperCase(address[i]) && charInt <= 7
                            || Character.isLowerCase(address[i]) && charInt > 7
                    -> {
                        return false
                    }
                }
            }
        }
        return true
    }

}
