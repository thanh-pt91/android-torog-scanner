package com.kootoro.torogscanner.ui.screens

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
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
                                onClick = { viewModel.authManager.signOut() },
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

                            Button(
                                onClick = {
                                    val clientId = googleClientId ?: "748872611293-hlqat4fvhtvmjt7bbq1hodd2b16174af.apps.googleusercontent.com"
                                    isSigningIn = true
                                    coroutineScope.launch {
                                        viewModel.authManager.signIn(clientId)
                                        isSigningIn = false
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
                                    Text("🔑 Đăng nhập Google")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
