package com.kootoro.torogscanner.data.repository

import com.kootoro.torogscanner.data.api.RetrofitClient
import com.kootoro.torogscanner.data.model.AuthConfigResponse
import com.kootoro.torogscanner.data.model.PushSheetRequest
import com.kootoro.torogscanner.data.model.PushSheetResponse
import com.kootoro.torogscanner.data.model.SerialCheckRequest
import com.kootoro.torogscanner.data.model.SerialCheckResponse
import com.kootoro.torogscanner.data.model.SheetRecordItem

class ToroGRepository {

    suspend fun getAuthConfig(): Result<AuthConfigResponse> {
        return try {
            val response = RetrofitClient.getService().getAuthConfig()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Lỗi tải cấu hình Auth: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkSerial(serial: String, idToken: String?): Result<SerialCheckResponse> {
        return try {
            val authHeader = if (!idToken.isNullOrEmpty()) "Bearer $idToken" else null
            val response = RetrofitClient.getService().checkSerial(
                authHeader = authHeader,
                request = SerialCheckRequest(serial = serial, idToken = idToken)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errBody = response.errorBody()?.string() ?: ""
                Result.failure(Exception(if (errBody.contains("error")) errBody else "Lỗi tra cứu Serial: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pushToSheet(
        records: List<SheetRecordItem>,
        idToken: String?,
        sheetId: String? = null,
        sheetName: String? = null
    ): Result<PushSheetResponse> {
        return try {
            val authHeader = if (!idToken.isNullOrEmpty()) "Bearer $idToken" else null
            val response = RetrofitClient.getService().pushToSheet(
                authHeader = authHeader,
                request = PushSheetRequest(
                    records = records,
                    idToken = idToken,
                    sheetId = sheetId,
                    sheetName = sheetName
                )
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    Result.success(body)
                } else {
                    Result.failure(Exception(body.error ?: "Lỗi đẩy Google Sheet"))
                }
            } else {
                val errBody = response.errorBody()?.string() ?: ""
                Result.failure(Exception(if (errBody.contains("error")) errBody else "Lỗi đẩy dữ liệu: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
