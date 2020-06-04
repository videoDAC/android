package com.videodac.rinkebytv

import androidx.multidex.MultiDexApplication
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P){
            // Do something for android pie and above versions
            setupBouncyCastle()
        }

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