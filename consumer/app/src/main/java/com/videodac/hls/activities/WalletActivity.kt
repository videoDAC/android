package com.videodac.hls.activities

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.security.ProviderInstaller
import com.google.zxing.EncodeHintType
import com.videodac.hls.R
import com.videodac.hls.databinding.VideoDacWalletBinding
import com.videodac.hls.helpers.GasOracleHelper.gasOracle
import com.videodac.hls.helpers.StatusHelper.channels
import com.videodac.hls.helpers.StatusHelper.status
import com.videodac.hls.helpers.Utils
import com.videodac.hls.helpers.Utils.WALLET_CREATED
import com.videodac.hls.helpers.Utils.WALLET_PATH
import com.videodac.hls.helpers.Utils.closeActivity
import com.videodac.hls.helpers.Utils.gasPrice
import com.videodac.hls.helpers.Utils.streamingFeeInEth
import com.videodac.hls.helpers.Utils.walletBalanceLeft
import com.videodac.hls.helpers.Utils.walletPassword
import com.videodac.hls.helpers.Utils.walletPublicKey
import com.videodac.hls.helpers.WebThreeHelper.web3

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import net.glxn.qrgen.android.QRCode
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.json.JSONObject
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.utils.Convert
import org.web3j.utils.Convert.Unit
import java.io.File
import java.math.BigDecimal
import java.math.BigInteger
import java.security.Security


class WalletActivity : AppCompatActivity() {

    // shared preferences
    private var PRIVATE_MODE = 0
    private val PREF_NAME = "video-dac-video_dac_wallet"
    private val TAG = "WALLET ACTIVITY"
    private lateinit var sharedPref: SharedPreferences

    // the balance check delay
    private val loopDelay = 2000L

    // view binding
    private lateinit var binding: VideoDacWalletBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // inflate binding
        binding = VideoDacWalletBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // check if
        installTls12()

        // setup bouncy castle for the wallet
        setupBouncyCastle()

        // get the prefs
        sharedPref = getSharedPreferences(PREF_NAME, PRIVATE_MODE)

        // go to full screen
        Utils.goFullScreen(this)

        // then init the wallet
        initializeWallet()

        with(binding.rootLayout, {
            setOnClickListener {
                closeActivity(this@WalletActivity, walletPublicKey)
            }
        })
    }

    private fun Context.installTls12() {
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


        binding.walletBalance.text = getString(R.string.livestream_credits)
        binding.walletBalanceUnit.text = "0 " + getString(R.string.go_eth_unit) // default balance will always be zero
        binding.walletAddress.text = walletPublicKey
        binding.creatorFee.text = getString(R.string.creator_fee)
        binding.creatorFeeUnit.text = String.format("%.4f ", streamingFeeInEth) + getString(R.string.go_eth_unit) + " per minute + gas"

        // set the qr code for the address too
        binding.qrCode.setImageBitmap(
            QRCode.from(walletPublicKey).withHint(EncodeHintType.MARGIN, 1).bitmap()
        )

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
                    binding.creatorFeeUnit.text = String.format(
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

                    if(Utils.isValidETHAddress(key)){
                        channels.add(key)
                    }

                }
            }

        }

    }


    @Suppress("BlockingMethodInNonBlockingContext")
    private fun checkWalletBalance() {

        lifecycleScope.launch(Dispatchers.IO) {


            withContext(Dispatchers.Main) {
                hideLoadingUi()
            }

            // finally start a loop to check if the user balance is sufficient to show the channel list
            var checkingBal = true

            while (checkingBal) {

                delay(loopDelay)

                Log.d("starting get balance", "no balance yet")

                // get the web3 client version
                val clientVersion = web3!!.web3ClientVersion().send()

                // if client has no error, proceed to check the wallet balance
                if(!clientVersion.hasError()) {


                    val balanceInWei = web3!!.ethGetBalance(
                        walletPublicKey,
                        DefaultBlockParameterName.LATEST
                    ).send().balance.toString()
                    val balanceInEther = Convert.fromWei(balanceInWei, Unit.ETHER)

                    Log.d("Getting balance", "has gotten balance")

                    if (balanceInEther >= BigDecimal.valueOf(streamingFeeInEth)) {
                        walletBalanceLeft  = balanceInEther
                        startActivity(Intent(this@WalletActivity, ChannelActivity::class.java))
                        closeActivity(this@WalletActivity, null)
                    }
                    else {
                        withContext(Dispatchers.Main) {
                            binding.walletBalanceUnit.text = """$balanceInEther ${getString(R.string.go_eth_unit)}"""
                        }
                    }
                } else{
                    checkingBal = false
                    Toast.makeText(
                        this@WalletActivity,
                        "Unable to instantiate burner wallet, please check you internet connection",
                        Toast.LENGTH_LONG
                    ).show()
                }

            }
        }

    }


    private fun hideLoadingUi() {
        binding.loadingText.visibility = View.GONE
        binding.loader.visibility = View.GONE

        binding.welcomeTitle.visibility = View.VISIBLE
        binding.tvName.visibility = View.VISIBLE
        binding.walletAddress.visibility = View.VISIBLE
        binding.walletBalanceUnit.visibility = View.VISIBLE
        binding.creatorFee.visibility = View.VISIBLE
        binding.creatorFeeUnit.visibility = View.VISIBLE
        binding.walletBalance.visibility = View.VISIBLE
        binding.tapInstructions.visibility = View.VISIBLE
        binding.qrCode.visibility = View.VISIBLE

    }

    private fun showLoadingUi() {

        binding.loadingText.visibility = View.VISIBLE
        binding.loader.visibility = View.VISIBLE

        binding.welcomeTitle.visibility = View.GONE
        binding.tvName.visibility = View.GONE
        binding.walletAddress.visibility = View.GONE
        binding.walletBalanceUnit.visibility = View.GONE
        binding.creatorFee.visibility = View.GONE
        binding.creatorFeeUnit.visibility = View.GONE
        binding.walletBalance.visibility = View.GONE
        binding.tapInstructions.visibility = View.GONE
        binding.qrCode.visibility = View.GONE
    }

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


}