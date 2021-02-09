package com.videodac.publisher

import androidx.multidex.MultiDexApplication
import com.videodac.publisher.helpers.GasOracleHelper.gasOracle
import com.videodac.publisher.helpers.RetrofitHelper
import com.videodac.publisher.helpers.Utils
import com.videodac.publisher.helpers.WebThreeHelper.web3

class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()

        // init web3
        web3 = Utils.getWeb3(this)


        // INIT THE SERVICES

        // init retrofit
        val retrofitHelper = RetrofitHelper(this)

        // init the gas oracle
        gasOracle = retrofitHelper.getGasOracleService()

    }
}