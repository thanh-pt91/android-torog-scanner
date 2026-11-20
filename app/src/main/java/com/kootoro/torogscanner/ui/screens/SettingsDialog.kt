package com.kootoro.torogscanner.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kootoro.torogscanner.ui.theme.AccentCyan
import com.kootoro.torogscanner.ui.theme.DangerRed
import com.kootoro.torogscanner.ui.theme.DarkCard
import com.kootoro.torogscanner.ui.theme.DarkSurface
import com.kootoro.torogscanner.ui.theme.PrimaryBlue
import com.kootoro.torogscanner.ui.theme.SuccessGreen
import com.kootoro.torogscanner.ui.theme.TextMuted
import com.kootoro.torogscanner.ui.theme.TextPrimary
import com.kootoro.torogscanner.ui.viewmodel.ScannerViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsDialog(
    viewModel: ScannerViewModel,
    onDismiss: () -> Unit
) {
    val serverUrl by viewModel.serverUrl.collectAsState()
    val isFilterEnabled by viewModel.isFilterEnabled.collectAsState()
    val allowedPrefixes by viewModel.allowedPrefixes.collectAsState()
    val userState by viewModel.authManager.userState.collectAsState()
    val googleClientId by viewModel.googleClientId.collectAsState()

    var urlInput by remember { mutableStateOf(serverUrl) }
    var prefixInput by remember { mutableStateOf(allowedPrefixes.joinToString(", ")) }
    var isSigningIn by remember { mutableStateOf(false) }

    var showTokenDialog by remember { mutableStateOf(false) }
    var tokenInput by remember { mutableStateOf("") }
    var showNoAccountDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cài đặt ứng dụng",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Server URL Section
                Text("ĐỊA CHỈ MÁY CHỦ (BACKEND)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentCyan)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Server URL") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = DarkCard
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        viewModel.updateServerUrl(urlInput)
                        Toast.makeText(context, "Đã lưu địa chỉ máy chủ!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Lưu địa chỉ máy chủ")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Prefix Filter Section
                Text("BỘ LỌC TIỀN TỐ SERIAL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentCyan)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Bật bộ lọc mã Serial", color = TextPrimary, fontSize = 14.sp)
                    Switch(
                        checked = isFilterEnabled,
                        onCheckedChange = { viewModel.toggleFilter() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextPrimary,
                            checkedTrackColor = PrimaryBlue
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = prefixInput,
                    onValueChange = { prefixInput = it },
                    label = { Text("Tiền tố cho phép (cách nhau dấu phẩy)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = DarkCard
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = {
                        val list = prefixInput.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
                        viewModel.updatePrefixes(list)
                        Toast.makeText(context, "Đã cập nhật bộ lọc tiền tố!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cập nhật danh sách tiền tố")
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 3. Google Account Section
                Text("TÀI KHOẢN GOOGLE & PHÂN QUYỀN", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentCyan)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        if (userState.isSignedIn) {
                            Text(text = userState.name ?: "Người dùng Google", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = userState.email ?: "", fontSize = 13.sp, color = TextMuted)
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (userState.isAllowed) "✅ Đã được cấp quyền tra cứu ToroG & ghi Sheet"
                                else "⛔ Email chưa được cấp quyền tra cứu & ghi Sheet",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (userState.isAllowed) SuccessGreen else DangerRed
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    viewModel.authManager.signOut()
                                    Toast.makeText(context, "Đã đăng xuất Google", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Đăng xuất tài khoản")
                            }
                        } else {
                            Text(
                                text = "Đăng nhập Google để xác thực quyền tra cứu ToroG và đẩy dữ liệu lên Google Sheets.",
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // 1. One-tap sign in (if account exists on device)
                            Button(
                                onClick = {
                                    if (activity == null) {
                                        Toast.makeText(context, "Lỗi: Không tìm thấy Activity để mở giao diện Google", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val clientId = googleClientId ?: "748872611293-hlqat4fvhtvmjt7bbq1hodd2b16174af.apps.googleusercontent.com"
                                    isSigningIn = true
                                    coroutineScope.launch {
                                        val res = viewModel.authManager.signIn(activity, clientId)
                                        isSigningIn = false
                                        res.onSuccess { user ->
                                            Toast.makeText(context, "Đăng nhập thành công: ${user.email}", Toast.LENGTH_SHORT).show()
                                        }.onFailure { err ->
                                            val msg = err.message ?: "Lỗi đăng nhập"
                                            if (msg.contains("Không tìm thấy") || msg.contains("NoCredential")) {
                                                showNoAccountDialog = true
                                            } else {
                                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                enabled = !isSigningIn,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isSigningIn) {
                                    CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Đang kết nối Google...")
                                } else {
                                    Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("🔑 Đăng nhập Google (Tài khoản máy)")
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 2. Web Login (Enter Gmail email + password in browser)
                            Button(
                                onClick = {
                                    try {
                                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://torog-scanner-sg.onrender.com/login.html"))
                                        context.startActivity(webIntent)
                                        Toast.makeText(context, "Đang mở trình duyệt để nhập email/mật khẩu Google...", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Không mở được trình duyệt: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan.copy(alpha = 0.2f), contentColor = AccentCyan),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("🌐 Đăng nhập qua Web (Nhập email & MK)", fontWeight = FontWeight.SemiBold)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 3. Add Google account to device OS
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val addAccountIntent = Intent(Settings.ACTION_ADD_ACCOUNT).apply {
                                            putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
                                        }
                                        context.startActivity(addAccountIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Không mở được cài đặt tài khoản: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("📱 Thêm tài khoản Google vào máy", fontSize = 13.sp)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 4. Manual token paste fallback
                            OutlinedButton(
                                onClick = { showTokenDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("✏️ Nhập / Dán Token xác thực thủ công", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog nhập Token thủ công (dự phòng)
    if (showTokenDialog) {
        AlertDialog(
            onDismissRequest = { showTokenDialog = false },
            title = { Text("Nhập Google ID Token", color = TextPrimary) },
            text = {
                Column {
                    Text(
                        "Dán Google ID Token để xác thực trực tiếp nếu máy chưa cấu hình Google Play Services:",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        label = { Text("Google ID Token (ey...)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = DarkCard
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val token = tokenInput.trim()
                        if (token.isEmpty()) return@Button
                        showTokenDialog = false
                        coroutineScope.launch {
                            val res = viewModel.authManager.verifyAndSaveUser(token)
                            res.onSuccess {
                                Toast.makeText(context, "Xác thực thành công: ${it.email}", Toast.LENGTH_SHORT).show()
                            }.onFailure {
                                Toast.makeText(context, "Lỗi: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Xác thực")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showTokenDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Dialog thông báo chưa có tài khoản Google trên máy
    if (showNoAccountDialog) {
        AlertDialog(
            onDismissRequest = { showNoAccountDialog = false },
            title = { Text("Chưa có tài khoản Google", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Điện thoại chưa được cài đặt tài khoản Google trong Cài đặt của Android.\n\nBạn có thể mở trang Web để đăng nhập (nhập email & mật khẩu), hoặc thêm tài khoản Google vào cài đặt máy.",
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNoAccountDialog = false
                        try {
                            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://torog-scanner-sg.onrender.com/login.html"))
                            context.startActivity(webIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Lỗi mở Web: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("🌐 Đăng nhập qua Web")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showNoAccountDialog = false
                        try {
                            val addAccountIntent = Intent(Settings.ACTION_ADD_ACCOUNT).apply {
                                putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
                            }
                            context.startActivity(addAccountIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("📱 Thêm vào máy")
                }
            }
        )
    }
}
