package com.videodac.publisher.helpers

import android.content.Context
import android.util.Log

import com.videodac.publisher.BuildConfig
import com.videodac.publisher.R
import com.videodac.publisher.services.GasOracle

import okhttp3.OkHttpClient.*

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import java.util.concurrent.TimeUnit

class RetrofitHelper(private val context: Context) {
    private val TAG  = Retrofit::class.simpleName


    // init the gas price oracle
    internal fun getGasOracleService(): GasOracle {
        return Retrofit.Builder()
            .baseUrl(context.getString(R.string.gas_price_base_url))
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient())
            .build()
            .create(GasOracle::class.java)
    }


    private fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        val interceptor = HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
            override fun log(message: String) {
                Log.d(TAG, message)
            }
        })
        if (BuildConfig.DEBUG) {
            interceptor.level = HttpLoggingInterceptor.Level.BODY
        } else {
            // TODO - change to HEADERS on production...
            interceptor.level = HttpLoggingInterceptor.Level.BODY
        }

        return interceptor
    }


    private fun okHttpClient() =
        Builder()
            .addInterceptor(provideHttpLoggingInterceptor())
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
}