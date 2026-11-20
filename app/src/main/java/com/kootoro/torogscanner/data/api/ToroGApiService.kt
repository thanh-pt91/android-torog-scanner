package com.kootoro.torogscanner.data.api

import com.kootoro.torogscanner.data.model.AuthConfigResponse
import com.kootoro.torogscanner.data.model.PushSheetRequest
import com.kootoro.torogscanner.data.model.PushSheetResponse
import com.kootoro.torogscanner.data.model.SerialCheckRequest
import com.kootoro.torogscanner.data.model.SerialCheckResponse
import com.kootoro.torogscanner.data.model.VerifyUserRequest
import com.kootoro.torogscanner.data.model.VerifyUserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ToroGApiService {

    @GET("/api/auth-config")
    suspend fun getAuthConfig(): Response<AuthConfigResponse>

    @POST("/api/verify-user")
    suspend fun verifyUser(
        @Header("Authorization") authHeader: String?,
        @Body request: VerifyUserRequest
    ): Response<VerifyUserResponse>

    @POST("/api/check-serial")
    suspend fun checkSerial(
        @Header("Authorization") authHeader: String?,
        @Body request: SerialCheckRequest
    ): Response<SerialCheckResponse>

    @POST("/api/push-to-sheet")
    suspend fun pushToSheet(
        @Header("Authorization") authHeader: String?,
        @Body request: PushSheetRequest
    ): Response<PushSheetResponse>
}
