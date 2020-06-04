package com.videodac.rinkebytv

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.os.Handler
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import com.google.zxing.EncodeHintType

import com.videodac.rinkebytv.helpers.Utils
import com.videodac.rinkebytv.helpers.Utils.WALLET_CREATED
import com.videodac.rinkebytv.helpers.Utils.WALLET_PATH
import com.videodac.rinkebytv.helpers.Utils.closeActivity
import com.videodac.rinkebytv.helpers.Utils.streamingFeeInEth
import com.videodac.rinkebytv.helpers.Utils.walletPassword
import com.videodac.rinkebytv.helpers.Utils.walletPublicKey
import com.videodac.rinkebytv.services.Connext

import kotlinx.android.synthetic.main.wallet_layout.*

import net.glxn.qrgen.android.QRCode

import org.web3j.crypto.WalletUtils
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.Web3ClientVersion
import org.web3j.utils.Convert
import org.web3j.utils.Convert.Unit

import java.io.File
import java.math.BigDecimal

class WalletActivity : AppCompatActivity() {
    // shared preferences
    private var PRIVATE_MODE = 0
    private val PREF_NAME = "video-dac-wallet"
    private lateinit var sharedPref: SharedPreferences

    // polling vars
    private var handler: Handler? = Handler()
    private var runnable: Runnable? = null
    private var delay = 5 * 1000 //Delay for 5 seconds.  One second = 1000 milliseconds.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.wallet_layout)
        sharedPref = getSharedPreferences(PREF_NAME, PRIVATE_MODE)

        Utils.goFullScreen(this)
        initializeWallet()



        with(root_layout, {
            setOnClickListener {
                closeActivity(this@WalletActivity, walletPublicKey)
            }
        })
    }

    private fun initializeWallet() {

        val walletCreated = sharedPref.getBoolean(WALLET_CREATED, false)

        if (!walletCreated) {
            showLoadingUi()
            // create the wallet first
            createWallet()
            // then get it
            getWalletBalance()

        } else{
            // otherwise just get it
            showLoadingUi()
            getWalletBalance()
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

    private fun getWalletBalance() {
        var clientVersion: Web3ClientVersion
        val web3 = Utils.getWeb3(this)

        val walletPath = sharedPref.getString(WALLET_PATH,"")
        val credentials = WalletUtils.loadCredentials(walletPassword, walletPath)
        walletPublicKey = credentials.address

        // initialize the connext channel here


        Thread {
            //get client version
            clientVersion = web3.web3ClientVersion().send()
            val balanceWei = web3.ethGetBalance(walletPublicKey, DefaultBlockParameterName.LATEST).send()

            runOnUiThread {

                if (!clientVersion.hasError()) { //Connected
                    // get the address balance
                    val balanceInEther = Convert.fromWei(balanceWei.balance.toString(), Unit.ETHER)
                    // finally start playing the video if the balance is greater than
                    if(balanceInEther > BigDecimal.valueOf(streamingFeeInEth)) {
                        startActivity(Intent(this@WalletActivity, VideoActivity::class.java))
                        closeActivity(this, null)
                    } else {
                        hideLoadingUi()
                        wallet_balance.text = getString(R.string.livestream_credits)
                        wallet_balance_unit.text = balanceInEther.toString() + " " + getString(R.string.go_eth_unit)
                        wallet_address.text = walletPublicKey
                        creator_fee.text = getString(R.string.creator_fee)
                        creator_fee_unit.text = String.format("%.4f ", streamingFeeInEth) + getString(R.string.go_eth_unit) + " per minute + gas"
                        // set the qr code for the address too
                        qr_code.setImageBitmap(QRCode.from(walletPublicKey).withHint(EncodeHintType.MARGIN, 1).bitmap())
                    }
                }
                else { //Show Error
                    Toast.makeText(this, "Unable to instantiate burner wallet", Toast.LENGTH_LONG).show()
                }
            }
        }.start()

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

    override fun onResume() {
        // update the wallet balance every 5 seconds
        handler!!.postDelayed(Runnable {
            //do something
            getWalletBalance()
            handler!!.postDelayed(runnable!!, delay.toLong())
        }.also {
            runnable = it
        }, delay.toLong())

        super.onResume();
    }


    override fun onPause() {
        handler!!.removeCallbacks(runnable!!) //stop handler when activity not visible
        super.onPause()
    }

}