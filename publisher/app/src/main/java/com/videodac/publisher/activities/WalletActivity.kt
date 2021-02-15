package com.videodac.publisher.activities

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity

import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.security.ProviderInstaller

import com.videodac.publisher.R
import com.videodac.publisher.helpers.Constants
import com.videodac.publisher.helpers.Constants.WALLET_CREATED
import com.videodac.publisher.helpers.Constants.WALLET_FIRST_PAYMENT_RECEIVED
import com.videodac.publisher.helpers.Constants.WALLET_PASSWORD
import com.videodac.publisher.helpers.Constants.WALLET_PATH
import com.videodac.publisher.helpers.Utils.walletFirstPaymentReceived


import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.web3j.crypto.WalletUtils

import java.io.File
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

        val walletCreated = sharedPref.getBoolean(WALLET_CREATED, false)
        walletFirstPaymentReceived  = sharedPref.getBoolean(WALLET_FIRST_PAYMENT_RECEIVED, false)

        if (!walletCreated) {
            // create the burner first
            createWallet()
        }
        // otherwise just  go to the main screen
        startActivity(Intent(this@WalletActivity, StreamingActivity::class.java))
        finish()
    }

    // create a new burner wallet
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

    // Check if this device has a camera
    private fun checkCameraHardware(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
    }

}