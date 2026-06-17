package com.example.flickfind_ltttbdd.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flickfind_ltttbdd.data.MovieRepository
import com.example.flickfind_ltttbdd.data.local.UserEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = FirebaseAuth.getInstance().currentUser?.isEmailVerified ?: false,
    val isVerificationSent: Boolean = false,
    val isEmailVerifiedSuccess: Boolean = false
)

class AuthViewModel(private val repository: MovieRepository) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { fbAuth: FirebaseAuth ->
        val user = fbAuth.currentUser
        _uiState.update { it.copy(isLoggedIn = user != null && user.isEmailVerified) }
    }

    init {
        auth.addAuthStateListener(authListener)
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
    }

    fun login(email: String, password: String) {
        val parts = email.trim().split("@")
        val cleanEmail = if (parts.size == 2) {
            "${parts[0]}@${parts[1].lowercase()}"
        } else {
            email.trim().lowercase()
        }

        Log.d("AuthViewModel", "==> Đang đăng nhập: $cleanEmail")
        if (cleanEmail.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Vui lòng nhập đầy đủ thông tin") }
            return
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            _uiState.update { it.copy(errorMessage = "Email không hợp lệ") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val result = auth.signInWithEmailAndPassword(cleanEmail, password).await()
                val user = result.user
                
                if (user != null) {
                    if (!user.isEmailVerified) {
                        Log.w("AuthViewModel", "Đăng nhập thất bại: Email chưa xác thực")
                        user.sendEmailVerification()
                        auth.signOut()
                        _uiState.update { it.copy(
                            isLoading = false,
                            isLoggedIn = false,
                            errorMessage = "Email chưa được xác thực. Chúng tôi đã gửi lại liên kết xác thực vào email của bạn."
                        ) }
                        return@launch
                    }
                    Log.i("AuthViewModel", "Đăng nhập thành công: ${user.uid}")
                    repository.insertOrUpdateUser(
                        UserEntity(
                            id = user.uid,
                            name = user.displayName ?: "Người dùng",
                            avatarUrl = user.photoUrl?.toString() ?: ""
                        )
                    )
                }

                _uiState.update { it.copy(isLoading = false, isSuccess = true, isLoggedIn = true) }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Lỗi đăng nhập", e)
                val friendlyMessage = when (e) {
                    is FirebaseAuthInvalidCredentialsException -> "Tài khoản hoặc mật khẩu không chính xác"
                    else -> translateError(e.message)
                }
                _uiState.update { it.copy(isLoading = false, errorMessage = friendlyMessage) }
            }
        }
    }

    fun register(name: String, email: String, password: String, confirmPassword: String) {
        val cleanName = name.trim()
        val cleanEmail = email.trim().lowercase()

        if (cleanName.isBlank() || cleanEmail.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Vui lòng nhập đầy đủ thông tin") }
            return
        }

        if (password != confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Mật khẩu xác nhận không khớp") }
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            _uiState.update { it.copy(errorMessage = "Email không hợp lệ") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val result = auth.createUserWithEmailAndPassword(cleanEmail, password).await()
                val user = result.user
                
                if (user != null) {
                    user.updateProfile(
                        UserProfileChangeRequest.Builder()
                            .setDisplayName(cleanName)
                            .build()
                    ).await()

                    user.sendEmailVerification().await()
                    
                    repository.insertOrUpdateUser(
                        UserEntity(
                            id = user.uid,
                            name = cleanName,
                            avatarUrl = ""
                        )
                    )
                    
                    _uiState.update { it.copy(
                        isLoading = false,
                        isSuccess = true,
                        isVerificationSent = true,
                        isLoggedIn = false
                    ) }
                }
            } catch (e: Exception) {
                val friendlyMessage = when (e) {
                    is FirebaseAuthWeakPasswordException -> "Mật khẩu quá yếu"
                    is FirebaseAuthUserCollisionException -> "Email này đã được sử dụng bởi một tài khoản khác"
                    else -> translateError(e.message)
                }
                _uiState.update { it.copy(isLoading = false, errorMessage = friendlyMessage) }
            }
        }
    }

    fun checkEmailVerificationStatus() {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                user.reload().await()
                if (user.isEmailVerified) {
                    repository.insertOrUpdateUser(
                        UserEntity(
                            id = user.uid,
                            name = user.displayName ?: "Người dùng",
                            avatarUrl = ""
                        )
                    )
                    auth.signOut()
                    
                    _uiState.update { it.copy(
                        isEmailVerifiedSuccess = true,
                        isLoggedIn = false
                    ) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun translateError(message: String?): String {
        if (message == null) return "Lỗi không xác định"
        return when {
            message.contains("badly formatted", ignoreCase = true) -> "Email không hợp lệ"
            message.contains("invalid email", ignoreCase = true) -> "Email không hợp lệ"
            message.contains("network", ignoreCase = true) -> "Lỗi kết nối mạng"
            message.contains("too many requests", ignoreCase = true) -> "Quá nhiều yêu cầu. Vui lòng thử lại sau"
            message.contains("user not found", ignoreCase = true) -> "Tài khoản không tồn tại"
            message.contains("wrong password", ignoreCase = true) -> "Mật khẩu không chính xác"
            else -> "Lỗi hệ thống: $message"
        }
    }

    fun logout() {
        Log.d("AuthViewModel", "==> Đăng xuất người dùng")
        auth.signOut()
        _uiState.update { it.copy(isLoggedIn = false, isVerificationSent = false, isEmailVerifiedSuccess = false) }
    }
    
    fun resetSuccess() {
        _uiState.update { it.copy(isSuccess = false, isEmailVerifiedSuccess = false) }
    }
}
