package com.videodac.hls

import androidx.multidex.MultiDexApplication

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

import com.videodac.hls.helpers.GasOracleHelper.gasOracle
import com.videodac.hls.helpers.RetrofitHelper
import com.videodac.hls.helpers.StatusHelper.status
import com.videodac.hls.helpers.ThreeBoxHelper.threeBox
import com.videodac.hls.helpers.Utils
import com.videodac.hls.helpers.WebThreeHelper.web3

class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()

        // setup the correct crypto lib
        setupBouncyCastle()

        // init web3
        web3 = Utils.getWeb3(this)

        // INIT THE SERVICES
        val retrofitHelper = RetrofitHelper(this)

        // init the gas oracle
        gasOracle = retrofitHelper.getGasOracleService()

        // init the 3box service
        threeBox = retrofitHelper.getThreeBoxService()

        // init the status endpoint
        status = retrofitHelper.getStatusService()
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