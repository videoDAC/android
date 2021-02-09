package com.videodac.publisher.activities

import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.Spannable
import android.text.SpannableString
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.widget.TextView

import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope

import com.videodac.publisher.R
import com.videodac.publisher.helpers.Constants.PREF_NAME
import com.videodac.publisher.helpers.Constants.PRIVATE_MODE
import com.videodac.publisher.helpers.Constants.WALLET_CREATED
import com.videodac.publisher.helpers.Constants.WALLET_PASSWORD
import com.videodac.publisher.helpers.Constants.WALLET_PATH
import com.videodac.publisher.helpers.Constants.WALLET_TAG
import com.videodac.publisher.helpers.TypefaceSpan
import com.videodac.publisher.helpers.Utils.walletPublicKey
import com.videodac.publisher.helpers.WebThreeHelper.web3

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.tx.Transfer
import org.web3j.utils.Convert

import java.io.File
import java.io.IOException
import java.math.BigDecimal
import java.security.Security


class MainActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences


    // the balance check delay
    private val loopDelay = 2000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.launch_screen)

        // get the prefs
        sharedPref = getSharedPreferences(PREF_NAME, PRIVATE_MODE)

        // setup bouncy castle for the wallet
        setupBouncyCastle()

        // setup the toolbar icons
        setupToolbarIcons()
        centerActionBarTitle()

        // init the wallet
        initializeWallet()

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

            titleTextView.gravity = Gravity.CENTER_HORIZONTAL
            titleTextView.width = resources.displayMetrics.widthPixels
            titleTextView.textSize = 24f

            supportActionBar!!.title  = SpannableString(getString(R.string.launch_screen_title)).apply {
                setSpan(
                    TypefaceSpan(this@MainActivity, getString(R.string.font_name)),
                    0,
                    length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }


        }
    }

    // Initialize a burner wallet to be used for streaming funds to
    private fun initializeWallet() {

        val walletCreated = sharedPref.getBoolean(WALLET_CREATED, false)

        if (!walletCreated) {
            // create the burner first
            createWallet()
            // then load it
            loadWallet()

        } else {
            // otherwise just get it
            loadWallet()
        }

    }

    private fun createWallet() {
        val path = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.path
        val fileName = WalletUtils.generateLightNewWalletFile(WALLET_PASSWORD, File(path))
        val filePath = "$path/$fileName"

        sharedPref.edit().apply {
            putString(WALLET_PATH, filePath)
            putBoolean(WALLET_CREATED, true)
            apply()
        }
    }

    private fun loadWallet() {

        val walletPath = sharedPref.getString(WALLET_PATH, "")
        val credentials = WalletUtils.loadCredentials(WALLET_PASSWORD, walletPath)

        // set the wallet public key global var
        walletPublicKey = credentials.address

        // the check the wallet balance
        checkWalletBalance()

    }

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

                    val balanceInWei = web3!!.ethGetBalance(walletPublicKey, DefaultBlockParameterName.LATEST).send().balance.toString()
                    val balanceInEther = Convert.fromWei(balanceInWei, Convert.Unit.ETHER)

                    Log.d(WALLET_TAG, "wallet balance in WEI is $balanceInWei")
                    Log.d(WALLET_TAG, "wallet balance in MATIC is $balanceInEther")
                    Log.d(WALLET_TAG, "wallet public key  is $walletPublicKey")

                    checkingBal = false

                    // test pay streaming
                  // testPayStreamingFee() 0.090282 MATIC


                } else{
                    checkingBal = false
                }

            }
        }

    }

    // setup the correct algo for the wallet
    private fun setupBouncyCastle() {
        val provider =
            Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)
                ?: // Web3j will set up the provider lazily when it's first used.
                return
        when (provider.javaClass) {
            BouncyCastleProvider::class.java -> { // BC with same package name, shouldn't happen in real life.
                return
            }
            // Android registers its own BC provider. As it might be outdated and might not include
            // all needed ciphers, we substitute it with a known BC bundled in the app.
            // Android's BC has its package rewritten to "com.android.org.bouncycastle" and because
            // of that it's possible to have another BC implementation loaded in VM.
            else -> {
                Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
                Security.insertProviderAt(BouncyCastleProvider(), 1)
            }
        }
    }


    private fun testPayStreamingFee() {

        lifecycleScope.launch(Dispatchers.IO) {

            var streamingFunds = true
            var count = 0

            while(streamingFunds) {

                if(count > 0){
                    delay(loopDelay)
                }



                try {
                    // open the burner wallet into a credential object
                    val walletPath = sharedPref.getString(WALLET_PATH, "")
                    val credentials = WalletUtils.loadCredentials(WALLET_PASSWORD, walletPath)
                    val recipientAddress = "0x7c88E445fA773275eAdc619D5a6FBe12a4f40a24"
                    val streamingFeeInEth = 0.0001


                    val transferReceipt = Transfer.sendFunds(
                        web3, credentials, recipientAddress, BigDecimal.valueOf(
                            streamingFeeInEth
                        ), Convert.Unit.ETHER
                    ).send()

                    if(transferReceipt.isStatusOK) {
                        Log.d(WALLET_TAG, "Streamed $streamingFeeInEth to $recipientAddress")

                        val balanceWei = web3!!.ethGetBalance(
                            credentials.address,
                            DefaultBlockParameterName.LATEST
                        ).send()

                        val walletBalanceLeft = Convert.fromWei(
                            balanceWei.balance.toString(),
                            Convert.Unit.ETHER
                        )

                        Log.d(WALLET_TAG, "Wallet balance left is $walletBalanceLeft MATIC")

                        if (walletBalanceLeft < BigDecimal.valueOf(streamingFeeInEth)) {
                            streamingFunds = false
                        }


                    }
                } catch (io: IOException) {
                    Log.e(WALLET_TAG, io.message!!)
                } catch (ex: InterruptedException) {
                    Log.e(WALLET_TAG, ex.message!!)
                }
                catch (re: RuntimeException){
                    Log.e(WALLET_TAG, re.message!!)
                }
                count++

            }

        }

    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }
}