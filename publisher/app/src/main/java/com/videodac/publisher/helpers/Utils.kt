package com.videodac.publisher.helpers

import android.content.Context
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService

import com.videodac.publisher.R

object  Utils {

    @JvmStatic
    internal fun getWeb3(context: Context) = Web3j.build(HttpService(context.getString(R.string.infura_url)))
    
}