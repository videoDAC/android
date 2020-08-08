package com.videodac.hls.services

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ThreeBox {

    @GET("/profile")
    suspend fun getProfile(@Query("address")userAddress: String): Response<JsonObject>


    @GET("/list-spaces?name=livepeer")
    suspend fun getProfileFromLivepeerSpace(@Query("address")userAddress: String): Response<JsonArray>


    @GET("/space?name=livepeer")
    suspend fun getSpaceDetails(@Query("address")userAddress: String): Response<JsonObject>

}