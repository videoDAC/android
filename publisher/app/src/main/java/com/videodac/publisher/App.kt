package com.videodac.publisher

import androidx.multidex.MultiDexApplication
import com.videodac.publisher.helpers.Utils
import com.videodac.publisher.helpers.WebThreeHelper.web3

class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()

        // init web3
        web3 = Utils.getWeb3(this)
    }
}