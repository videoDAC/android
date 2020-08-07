package com.videodac.hls.services

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.GET

interface Status {

    @GET("/status")
    suspend fun getManifest(): Response<JsonObject>

}