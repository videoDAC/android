package com.videodac.hls.services

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.GET

interface GasOracle {

    @GET("/api/ethgasAPI.json")
    suspend fun getGasPrice(): Response<JsonObject>
}