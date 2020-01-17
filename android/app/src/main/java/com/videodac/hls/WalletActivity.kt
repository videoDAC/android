package com.videodac.hls

import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.wallet_layout.*
import org.web3j.crypto.*
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.Web3ClientVersion
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Convert
import org.web3j.utils.Convert.Unit
import java.util.*


class WalletActivity : AppCompatActivity() {

    private var PRIVATE_MODE = 0
    private val PREF_NAME = "video-dac-wallet"
    private lateinit var sharedPref: SharedPreferences
    private val TAG = "VIDEO_DAC_WALLET"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.wallet_layout)
        this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        supportActionBar!!.hide()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        sharedPref = getSharedPreferences(PREF_NAME, PRIVATE_MODE)

        initializeWallet()
    }

    private fun initializeWallet() {

        val walletCreated = sharedPref.getBoolean("walletCreated", false)

        if (!walletCreated) {
            // create the wallet first
            createWallet()
            // then get it
            getWalletBalance()

        } else{
            // otherwise just get it
            getWalletBalance()
        }

    }

    private fun createWallet() {

        try {
            val ecKeyPair = Keys.createEcKeyPair()
            val creds = Credentials.create(ecKeyPair)
            val privateKeyInDec = ecKeyPair.privateKey
            val sPrivatekeyInHex = privateKeyInDec.toString(16)
            val aWallet = Wallet.createLight(UUID.randomUUID().toString(), ecKeyPair);
            val sAddress = aWallet.getAddress()
        }
        catch (e: Exception){
            Log.e(TAG, e.message!!)
        }

    }

    private fun getWalletBalance() {
        var web3: Web3j
        var clientVersion: Web3ClientVersion

        initWalletView.visibility = View.GONE
        hideWalletUi()
        showLoadingUi()
        Thread {
            //Do some Network Request
            web3 = Web3j.build(HttpService(getString(R.string.rinkeby_infura_url)))
            clientVersion = web3.web3ClientVersion().send()
            val balanceWei = web3.ethGetBalance("0xF0f15Cedc719B5A55470877B0710d5c7816916b1", DefaultBlockParameterName.LATEST).send()

            runOnUiThread {
                showWalletUi()
                hideLoadingUi()
                if (!clientVersion.hasError()) { //Connected
                    // get the address balance
                    val balanceInEther = Convert.fromWei(balanceWei.balance.toString(), Unit.ETHER)
                    wallet_balance.text = "Balance: $balanceInEther"

                }
                else { //Show Error
                    Toast.makeText(this, "Unable to instantiate burner wallet", Toast.LENGTH_LONG).show()
                }

            }
        }.start()

    }

    private fun hideWalletUi() {
        wallet_address.visibility = View.GONE
        wallet_balance.visibility = View.GONE
        qr_code.visibility = View.GONE
    }

    private fun showWalletUi() {
        wallet_address.visibility = View.VISIBLE
        wallet_balance.visibility = View.VISIBLE
        qr_code.visibility = View.VISIBLE
    }

    private fun hideLoadingUi() {
        loading_text.visibility = View.GONE
        loader.visibility = View.GONE
    }

    private fun showLoadingUi() {
        loading_text.visibility = View.VISIBLE
        loader.visibility = View.VISIBLE
    }

}