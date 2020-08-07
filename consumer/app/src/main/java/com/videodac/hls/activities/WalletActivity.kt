package com.videodac.hls.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope

import com.google.zxing.EncodeHintType

import com.videodac.hls.R
import com.videodac.hls.helpers.GasOracleHelper.gasOracle
import com.videodac.hls.helpers.StatusHelper.channels
import com.videodac.hls.helpers.StatusHelper.status
import com.videodac.hls.helpers.Utils
import com.videodac.hls.helpers.Utils.WALLET_CREATED
import com.videodac.hls.helpers.Utils.WALLET_PATH
import com.videodac.hls.helpers.Utils.closeActivity
import com.videodac.hls.helpers.Utils.gasPrice
import com.videodac.hls.helpers.Utils.streamingFeeInEth
import com.videodac.hls.helpers.Utils.walletPassword
import com.videodac.hls.helpers.Utils.walletPublicKey
import com.videodac.hls.helpers.WebThreeHelper.web3

import kotlinx.android.synthetic.main.video_dac_wallet.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import net.glxn.qrgen.android.QRCode
import org.json.JSONObject

import org.web3j.crypto.WalletUtils
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.utils.Convert
import org.web3j.utils.Convert.Unit

import java.io.File
import java.math.BigDecimal
import java.math.BigInteger

class WalletActivity : AppCompatActivity() {

    // shared preferences
    private var PRIVATE_MODE = 0
    private val PREF_NAME = "video-dac-video_dac_wallet"
    private val TAG = "WALLET ACTIVITY"
    private lateinit var sharedPref: SharedPreferences

    // the balance check delay
    private val loopDelay = 2000L


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.video_dac_wallet)
        sharedPref = getSharedPreferences(PREF_NAME, PRIVATE_MODE)

        // go to full screen
        Utils.goFullScreen(this)

        // then init the wallet
        initializeWallet()

        with(root_layout, {
            setOnClickListener {
                closeActivity(this@WalletActivity, walletPublicKey)
            }
        })
    }

    private fun initializeWallet() {

        showLoadingUi()

        val walletCreated = sharedPref.getBoolean(WALLET_CREATED, false)

        if (!walletCreated) {
            // create the video_dac_wallet first
            createWallet()
            // then load it
            loadWallet()

        } else {
            // otherwise just get it
            loadWallet()
        }

    }

    private fun createWallet() {
        val path = getExternalFilesDir(DIRECTORY_DOWNLOADS)!!.path
        val fileName = WalletUtils.generateLightNewWalletFile(walletPassword, File(path))
        val filePath = "$path/$fileName"

        sharedPref.edit().apply {
            putString(WALLET_PATH, filePath)
            putBoolean(WALLET_CREATED, true)
            apply()
        }
    }

    private fun loadWallet() {

        val walletPath = sharedPref.getString(WALLET_PATH, "")
        val credentials = WalletUtils.loadCredentials(walletPassword, walletPath)

        // set the wallet public key
        walletPublicKey = credentials.address


        wallet_balance.text = getString(R.string.livestream_credits)
        wallet_balance_unit.text = "0 " + getString(R.string.go_eth_unit) // default balance will always be zero
        wallet_address.text = walletPublicKey
        creator_fee.text = getString(R.string.creator_fee)
        creator_fee_unit.text = String.format("%.4f ", streamingFeeInEth) + getString(R.string.go_eth_unit) + " per minute + gas"

        // set the qr code for the address too
        qr_code.setImageBitmap(QRCode.from(walletPublicKey).withHint(EncodeHintType.MARGIN, 1).bitmap())

        // load the wallet details
        getGasPrice()
        getManifestChannels()
        checkWalletBalance()



    }

    private fun getGasPrice() {

        // first of all get the fastest gas price possible
        lifecycleScope.launch(Dispatchers.IO) {

            val gasPriceResp = gasOracle!!.getGasPrice()

            if (gasPriceResp.isSuccessful) {

                val gasObj = JSONObject(gasPriceResp.body().toString())

                // convert the gas price to gwei
                val fastGasPriceInGwei = gasObj.getInt("fast") / 10

                // then to wei
                val fastGasPriceInWei = Convert.toWei(BigDecimal(fastGasPriceInGwei), Unit.GWEI)

                // then set it globally
                gasPrice = BigInteger.valueOf(fastGasPriceInWei.toLong())

                // then add it to the streaming fee
                val streamingFeeInWei = Convert.toWei(BigDecimal(streamingFeeInEth), Unit.ETHER)

                val totalFeeInWei = fastGasPriceInWei + streamingFeeInWei

                // then finally get the total price to ETH
                val totalFeeInEth = Convert.fromWei(totalFeeInWei, Unit.ETHER)

                withContext(Dispatchers.Main) {
                    Log.d(TAG, totalFeeInEth.toString())
                    creator_fee_unit.text = String.format(
                        "%.4f ",
                        totalFeeInEth
                    ) + getString(R.string.go_eth_unit) + " per minute + gas"
                }
            }
        }

    }

    private fun getManifestChannels() {

        // then the channel manifest
        lifecycleScope.launch(Dispatchers.IO) {

            val channelManifestRes = status!!.getManifest()

            if (channelManifestRes.isSuccessful) {

                val manifestObj = JSONObject(channelManifestRes.body().toString())
                val channelsObj = manifestObj.getString(getString(R.string.manifest_key))
                val jsonObject = JSONObject(channelsObj)
                val keys: Iterator<String> = jsonObject.keys()

                channels = mutableListOf()

                while (keys.hasNext()) {
                    val key = keys.next()
                    channels.add(key)
                }
            }

        }

    }


    @Suppress("BlockingMethodInNonBlockingContext")
    private fun checkWalletBalance() {

        lifecycleScope.launch(Dispatchers.IO) {

            // finally start a loop to check if the user balance is sufficient to show the channel list
            var checkingBal = true

            while (checkingBal) {

                delay(loopDelay)

                // get the web3 client version
                val clientVersion = web3!!.web3ClientVersion().send()

                // if client has no error, proceed to check the wallet balance
                if(!clientVersion.hasError()) {

                    withContext(Dispatchers.Main) {
                        hideLoadingUi()
                    }

                    val balanceInWei = web3!!.ethGetBalance(walletPublicKey, DefaultBlockParameterName.LATEST).send().balance.toString()
                    val balanceInEther = Convert.fromWei(balanceInWei, Unit.ETHER)

                    if (balanceInEther > BigDecimal.valueOf(streamingFeeInEth)) {
                        startActivity(Intent(this@WalletActivity, ChannelActivity::class.java))
                        closeActivity(this@WalletActivity, null)
                    }
                    else {
                        withContext(Dispatchers.Main) {
                            wallet_balance_unit.text = """$balanceInEther ${getString(R.string.go_eth_unit)}"""
                        }
                    }
                } else{
                    checkingBal = false
                    Toast.makeText(this@WalletActivity, "Unable to instantiate burner wallet, please check you internet connection", Toast.LENGTH_LONG).show()
                }

            }
        }

    }


    private fun hideLoadingUi() {
        loading_text.visibility = View.GONE
        loader.visibility = View.GONE

        welcome_title.visibility = View.VISIBLE
        tv_name.visibility = View.VISIBLE
        wallet_address.visibility = View.VISIBLE
        wallet_balance_unit.visibility = View.VISIBLE
        creator_fee.visibility = View.VISIBLE
        creator_fee_unit.visibility = View.VISIBLE
        wallet_balance.visibility = View.VISIBLE
        tap_instructions.visibility = View.VISIBLE
        qr_code.visibility = View.VISIBLE

    }

    private fun showLoadingUi() {
        loading_text.visibility = View.VISIBLE
        loader.visibility = View.VISIBLE

        welcome_title.visibility = View.GONE
        tv_name.visibility = View.GONE
        wallet_address.visibility = View.GONE
        wallet_balance_unit.visibility = View.GONE
        creator_fee.visibility = View.GONE
        creator_fee_unit.visibility = View.GONE
        wallet_balance.visibility = View.GONE
        tap_instructions.visibility = View.GONE
        qr_code.visibility = View.GONE
    }

}