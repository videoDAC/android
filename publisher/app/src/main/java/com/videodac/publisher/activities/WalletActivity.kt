package com.videodac.publisher.activities

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope

import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.security.ProviderInstaller

import com.videodac.publisher.R
import com.videodac.publisher.helpers.Constants
import com.videodac.publisher.helpers.Constants.WALLET_TAG
import com.videodac.publisher.helpers.Utils.walletBalance
import com.videodac.publisher.helpers.Utils.walletPublicKey
import com.videodac.publisher.helpers.WebThreeHelper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.utils.Convert
import java.io.File
import java.io.IOException
import java.security.Security

class WalletActivity  : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.wallet_screen)

        if( checkCameraHardware(this)) {
            // check if the user has the latest google play apk
            updateGooglePlay()

            // get the prefs
            sharedPref = getSharedPreferences(Constants.PREF_NAME, Constants.PRIVATE_MODE)

            // setup bouncy castle for the wallet
            setupBouncyCastle()

            // init the wallet
            initializeWallet()
        } else {

            AlertDialog.Builder(this@WalletActivity)
                .setTitle("Camera Error!")
                .setMessage("This app requires a working camera to function well")
                .setCancelable(false)
                .setPositiveButton("ok") { _, _ ->
                    finish()
                }.show()

        }



    }

    // check if user has the latest google play apk installed
    private fun Context.updateGooglePlay() {
        try {
            ProviderInstaller.installIfNeeded(this)
        } catch (e: GooglePlayServicesRepairableException) {
            // Prompt the user to install/update/enable Google Play services.
            AlertDialog.Builder(this@WalletActivity)
                .setTitle(getString(R.string.google_play_error_title))
                .setMessage(getString(R.string.google_play_error))
                .setCancelable(false)
                .setPositiveButton("ok") { _, _ ->
                    finish()
                }.show()
        } catch (ge: GooglePlayServicesNotAvailableException) {
            // Indicates a non-recoverable error: let the user know.
            AlertDialog.Builder(this@WalletActivity)
                .setTitle(getString(R.string.google_play_error_title))
                .setMessage(getString(R.string.google_play_services_not_available_error))
                .setCancelable(false)
                .setPositiveButton("ok") { _, _ ->
                    finish()
                }.show()
        }
    }

    // Initialize a burner wallet to be used for streaming funds to
    private fun initializeWallet() {

        val walletCreated = sharedPref.getBoolean(Constants.WALLET_CREATED, false)

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

    // create a new burner wallet
    private fun createWallet() {
        val path = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.path
        val fileName = WalletUtils.generateLightNewWalletFile(Constants.WALLET_PASSWORD, File(path))
        val filePath = "$path/$fileName"

        sharedPref.edit().apply {
            putString(Constants.WALLET_PATH, filePath)
            putBoolean(Constants.WALLET_CREATED, true)
            apply()
        }
    }

    // load an existing wallet
    private fun loadWallet() {

        val walletPath = sharedPref.getString(Constants.WALLET_PATH, "")
        val credentials = WalletUtils.loadCredentials(Constants.WALLET_PASSWORD, walletPath)

        // set the wallet public key global var
        walletPublicKey = credentials.address

        // the check the wallet balance
        checkWalletBalance()

    }

    // check wallet balance from RPC Endpoint
    private fun checkWalletBalance() {

        // the balance check delay
        val loopDelay = 2000L

        lifecycleScope.launch(Dispatchers.IO) {

            // finally start a loop to get the user's balance as per the RPC Endpoint
            var checkingBal = true

            while (checkingBal) {

                delay(loopDelay)

                try {
                    // get the web3 client version
                    val clientVersion = WebThreeHelper.web3!!.web3ClientVersion().send()

                    // if client has no error, proceed to check the wallet balance
                    if(!clientVersion.hasError()) {

                        val balanceInWei = WebThreeHelper.web3!!.ethGetBalance(
                            walletPublicKey,
                            DefaultBlockParameterName.LATEST
                        ).send().balance.toString()
                        walletBalance = Convert.fromWei(balanceInWei, Convert.Unit.ETHER)


                        Log.d(WALLET_TAG, "wallet balance in WEI is $balanceInWei")
                        Log.d(WALLET_TAG, "wallet balance in MATIC is $walletBalance")
                        Log.d(WALLET_TAG, "wallet public key  is $walletPublicKey")

                        checkingBal = false


                        startActivity(Intent(this@WalletActivity, StreamingActivity::class.java))
                        finish()

                    } else{
                        checkingBal = handleError("Client Error!!!")
                    }


                } catch (io: IOException) {
                    checkingBal = handleError("IOException: " + io.message!!)
                } catch (ex: InterruptedException) {
                    checkingBal = handleError("InterruptedException: " + ex.message!!)
                }
                catch (re: RuntimeException){
                    checkingBal = handleError("RuntimeException: " +re.message!!)
                }

            }
        }

    }

    private suspend fun handleError(errorMsg: String?): Boolean {

        Log.e(WALLET_TAG, errorMsg!!)

        withContext(Dispatchers.Main) {

            AlertDialog.Builder(this@WalletActivity)
                .setTitle("RPC Error!")
                .setMessage("Unable to connect to the blockchain, please make sure your internet is working and try again!")
                .setCancelable(false)
                .setPositiveButton("ok") { _, _ ->
                    finish()
                }.show()
        }

        return false
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

    /** Check if this device has a camera */
    private fun checkCameraHardware(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
    }


}