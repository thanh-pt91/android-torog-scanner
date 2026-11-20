package com.kootoro.torogscanner.data.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.kootoro.torogscanner.data.api.RetrofitClient
import com.kootoro.torogscanner.data.model.VerifyUserRequest
import com.kootoro.torogscanner.data.model.VerifyUserResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GoogleUserState(
    val isSignedIn: Boolean = false,
    val idToken: String? = null,
    val email: String? = null,
    val name: String? = null,
    val picture: String? = null,
    val isAllowed: Boolean = false,
    val errorMessage: String? = null
)

class GoogleAuthManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("torog_auth_prefs", Context.MODE_PRIVATE)

    private val _userState = MutableStateFlow(GoogleUserState())
    val userState: StateFlow<GoogleUserState> = _userState.asStateFlow()

    init {
        // Load saved token if any
        val savedToken = prefs.getString("saved_id_token", null)
        val savedEmail = prefs.getString("saved_email", null)
        val savedName = prefs.getString("saved_name", null)
        val savedPic = prefs.getString("saved_pic", null)
        val isAllowed = prefs.getBoolean("saved_is_allowed", false)

        if (!savedToken.isNullOrEmpty() && !savedEmail.isNullOrEmpty()) {
            _userState.value = GoogleUserState(
                isSignedIn = true,
                idToken = savedToken,
                email = savedEmail,
                name = savedName,
                picture = savedPic,
                isAllowed = isAllowed
            )
        }
    }

    suspend fun signIn(activity: Activity, serverClientId: String): Result<VerifyUserResponse> {
        val targetClientId = if (serverClientId.isBlank()) {
            "748872611293-hlqat4fvhtvmjt7bbq1hodd2b16174af.apps.googleusercontent.com"
        } else serverClientId

        return try {
            val credentialManager = CredentialManager.create(activity)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(targetClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result: GetCredentialResponse = credentialManager.getCredential(
                request = request,
                context = activity
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                verifyAndSaveUser(idToken, googleIdTokenCredential.displayName, googleIdTokenCredential.profilePictureUri?.toString())
            } else {
                Result.failure(Exception("Không nhận dạng được thông tin Google Credential"))
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("Người dùng đã hủy chọn tài khoản Google"))
        } catch (e: NoCredentialException) {
            Result.failure(Exception("Không tìm thấy tài khoản Google nào trên máy"))
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi Google Sign-In (${e.javaClass.simpleName}): ${e.message}"))
        }
    }

    suspend fun verifyAndSaveUser(idToken: String, defaultName: String? = null, defaultPic: String? = null): Result<VerifyUserResponse> {
        return try {
            val api = RetrofitClient.getService()
            val response = api.verifyUser(
                authHeader = "Bearer $idToken",
                request = VerifyUserRequest(idToken = idToken)
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    _userState.value = GoogleUserState(
                        isSignedIn = true,
                        idToken = idToken,
                        email = body.email,
                        name = body.name ?: defaultName,
                        picture = body.picture ?: defaultPic,
                        isAllowed = body.isAllowed
                    )
                    // Save in prefs
                    prefs.edit()
                        .putString("saved_id_token", idToken)
                        .putString("saved_email", body.email)
                        .putString("saved_name", body.name ?: defaultName)
                        .putString("saved_pic", body.picture ?: defaultPic)
                        .putBoolean("saved_is_allowed", body.isAllowed)
                        .apply()

                    Result.success(body)
                } else {
                    Result.failure(Exception(body.error ?: "Xác thực tài khoản thất bại"))
                }
            } else {
                Result.failure(Exception("Lỗi máy chủ HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        prefs.edit().clear().apply()
        _userState.value = GoogleUserState()
    }
}
