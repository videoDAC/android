package com.videodac.publisher.services

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.GET

interface GasOracle {

    @GET("/")
    suspend fun getGasPrice(): Response<JsonObject>
}