package com.kootoro.torogscanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kootoro.torogscanner.data.model.ScannedDeviceItem
import com.kootoro.torogscanner.ui.theme.AccentCyan
import com.kootoro.torogscanner.ui.theme.DangerRed
import com.kootoro.torogscanner.ui.theme.DarkCard
import com.kootoro.torogscanner.ui.theme.DarkSurface
import com.kootoro.torogscanner.ui.theme.PrimaryBlue
import com.kootoro.torogscanner.ui.theme.SuccessGreen
import com.kootoro.torogscanner.ui.theme.TextMuted
import com.kootoro.torogscanner.ui.theme.TextPrimary
import com.kootoro.torogscanner.ui.viewmodel.ScannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PushSheetBottomSheet(
    items: List<ScannedDeviceItem>,
    viewModel: ScannerViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val userState by viewModel.authManager.userState.collectAsState()

    var loai by remember { mutableStateOf("") }
    var thietBi by remember { mutableStateOf("Hoạt động bình thường") }
    var note1 by remember { mutableStateOf("") }
    var boSac by remember { mutableStateOf("") }
    var note2 by remember { mutableStateOf("") }
    var mailThuHoi by remember { mutableStateOf("") }
    var guiLaiMay by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Text(
                text = "ĐẨY DỮ LIỆU LÊN GOOGLE SHEETS",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "${items.size} thiết bị chuẩn bị đẩy vào bảng tính Danh sách khách hàng gửi về",
                fontSize = 12.sp,
                color = AccentCyan,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Google Auth Status Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (userState.isSignedIn && userState.isAllowed) SuccessGreen else DangerRed,
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        if (userState.isSignedIn) {
                            Text(
                                text = "Người đẩy: ${userState.email}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (userState.isAllowed) "✅ Đã được cấp quyền ghi Sheet" else "⛔ Email chưa được cấp quyền ghi Sheet",
                                fontSize = 11.sp,
                                color = if (userState.isAllowed) SuccessGreen else DangerRed
                            )
                        } else {
                            Text(
                                text = "🔒 Chưa đăng nhập tài khoản Google",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DangerRed
                            )
                            Text(
                                text = "Bấm biểu tượng ⚙️ Cài đặt để đăng nhập Google trước khi đẩy",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. LOẠI
            FormDropdown(
                label = "LOẠI",
                selected = loai,
                options = listOf("", "Thu hồi", "Máy lỗi"),
                onSelected = { loai = it }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. THIẾT BỊ
            FormDropdown(
                label = "TÌNH TRẠNG THIẾT BỊ",
                selected = thietBi,
                options = listOf("", "Hoạt động bình thường", "Hư hỏng", "Đang kiểm tra lại"),
                onSelected = { thietBi = it }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. NOTE 1
            OutlinedTextField(
                value = note1,
                onValueChange = { note1 = it },
                label = { Text("NOTE 1 (Ghi chú chung)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = DarkCard
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 4. BỘ SẠC
            FormDropdown(
                label = "TÌNH TRẠNG BỘ SẠC",
                selected = boSac,
                options = listOf(
                    "",
                    "Hoạt động bình thường",
                    "Hư hỏng",
                    "Thiếu cả bộ sạc",
                    "Thiếu củ sạc",
                    "Thiếu dây sạc",
                    "Sai bộ sạc"
                ),
                onSelected = { boSac = it }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 5. NOTE 2
            OutlinedTextField(
                value = note2,
                onValueChange = { note2 = it },
                label = { Text("NOTE 2 (Ghi chú thêm)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = DarkCard
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 6. MAIL THU HỒI
            FormDropdown(
                label = "MAIL THU HỒI",
                selected = mailThuHoi,
                options = listOf("", "Chưa mail", "Đã mail"),
                onSelected = { mailThuHoi = it }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 7. GỬI LẠI MÁY
            FormDropdown(
                label = "GỬI LẠI MÁY",
                selected = guiLaiMay,
                options = listOf("", "Chưa gửi", "Đã gửi"),
                onSelected = { guiLaiMay = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            val canSubmit = userState.isSignedIn && userState.isAllowed && !isSubmitting
            Button(
                onClick = {
                    isSubmitting = true
                    viewModel.pushRecordsToSheet(
                        items = items,
                        loai = loai,
                        thietBi = thietBi,
                        note1 = note1,
                        boSac = boSac,
                        note2 = note2,
                        mailThuHoi = mailThuHoi,
                        guiLaiMay = guiLaiMay
                    ) { success ->
                        isSubmitting = false
                        if (success) {
                            onDismiss()
                        }
                    }
                },
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đang đẩy vào Google Sheet...")
                } else {
                    Icon(imageVector = Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (!userState.isSignedIn) "🔒 Cần đăng nhập Google"
                        else if (!userState.isAllowed) "⛔ Chưa được cấp quyền"
                        else "Xác nhận đẩy ${items.size} dòng lên Sheet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun FormDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = if (selected.isEmpty()) "— Để trống —" else selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = DarkCard
            )
        )

        // Invisible click overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(androidx.compose.ui.graphics.Color.Transparent)
                .clickable { expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(DarkSurface)
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(if (opt.isEmpty()) "— Để trống —" else opt, color = TextPrimary) },
                    onClick = {
                        onSelected(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}
