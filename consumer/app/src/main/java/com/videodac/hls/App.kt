package com.videodac.hls

import androidx.multidex.MultiDexApplication


import com.videodac.hls.helpers.GasOracleHelper.gasOracle
import com.videodac.hls.helpers.RetrofitHelper
import com.videodac.hls.helpers.StatusHelper.status
import com.videodac.hls.helpers.ThreeBoxHelper.threeBox
import com.videodac.hls.helpers.Utils
import com.videodac.hls.helpers.WebThreeHelper.web3

class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()

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






}