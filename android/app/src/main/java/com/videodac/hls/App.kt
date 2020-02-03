package com.videodac.hls

import android.content.Context
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        setupBouncyCastle()
    }

    private fun setupBouncyCastle() {
        val provider =
            Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)
                ?: // Web3j will set up the provider lazily when it's first used.
                return
        if (provider.javaClass == BouncyCastleProvider::class.java) { // BC with same package name, shouldn't happen in real life.
            return
        }
        // Android registers its own BC provider. As it might be outdated and might not include
        // all needed ciphers, we substitute it with a known BC bundled in the app.
        // Android's BC has its package rewritten to "com.android.org.bouncycastle" and because
        // of that it's possible to have another BC implementation loaded in VM.
        // Android registers its own BC provider. As it might be outdated and might not include
        // all needed ciphers, we substitute it with a known BC bundled in the app.
           // Android's BC has its package rewritten to "com.android.org.bouncycastle" and because
         // of that it's possible to have another BC implementation loaded in VM.
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.insertProviderAt(BouncyCastleProvider(), 1)
    }

    override fun attachBaseContext(base: Context?) {
        MultiDex.install(this)
        super.attachBaseContext(base)
    }

}