package com.kootoro.torogscanner.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kootoro.torogscanner.camera.CameraPreview
import com.kootoro.torogscanner.data.model.ScannedDeviceItem
import com.kootoro.torogscanner.ui.theme.AccentCyan
import com.kootoro.torogscanner.ui.theme.DangerRed
import com.kootoro.torogscanner.ui.theme.DarkBg
import com.kootoro.torogscanner.ui.theme.DarkCard
import com.kootoro.torogscanner.ui.theme.PrimaryBlue
import com.kootoro.torogscanner.ui.theme.SuccessGreen
import com.kootoro.torogscanner.ui.theme.TextMuted
import com.kootoro.torogscanner.ui.theme.TextPrimary
import com.kootoro.torogscanner.ui.theme.WarningYellow
import com.kootoro.torogscanner.ui.viewmodel.ScannerViewModel
import com.kootoro.torogscanner.util.BarcodeCleaner

@Composable
fun BatchScanView(
    viewModel: ScannerViewModel,
    onOpenPushSheetModal: (List<ScannedDeviceItem>) -> Unit,
    onSelectItemDetail: (ScannedDeviceItem) -> Unit
) {
    val isTorchOn by viewModel.isTorchOn.collectAsState()
    val zoomRatio by viewModel.zoomRatio.collectAsState()
    val allowedPrefixes by viewModel.allowedPrefixes.collectAsState()
    val isFilterEnabled by viewModel.isFilterEnabled.collectAsState()

    val batchList by viewModel.batchList.collectAsState()
    val isChecking by viewModel.isBatchChecking.collectAsState()
    val progress by viewModel.batchProgress.collectAsState()
    val progressText by viewModel.batchProgressText.collectAsState()

    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.decodeImageUri(context, it, isBatch = true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Upper: Camera View (45% height)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.42f)
        ) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                isTorchOn = isTorchOn,
                zoomRatio = zoomRatio,
                allowedPrefixes = allowedPrefixes,
                isFilterEnabled = isFilterEnabled,
                onBarcodeScanned = { barcodes ->
                    barcodes.forEach {
                        viewModel.onBarcodeDetected(it.cleanedValue)
                    }
                }
            )

            // Scanning indicator chip
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .background(DarkBg.copy(alpha = 0.75f), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(SuccessGreen, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Quét đa mã liên tục: ${batchList.size} thiết bị",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Floating Gallery Button (Bottom-Center)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkBg.copy(alpha = 0.85f))
                    .clickable { galleryLauncher.launch("image/*") }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Nạp ảnh tem thùng từ Album", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Lower: Batch List & Actions (58% height)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.58f)
                .background(DarkBg)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Header: Count + Clear button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DANH SÁCH THIẾT BỊ (${batchList.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentCyan
                )

                if (batchList.isNotEmpty()) {
                    Text(
                        text = "Xóa tất cả",
                        fontSize = 12.sp,
                        color = DangerRed,
                        modifier = Modifier.clickable { viewModel.clearBatchList() }
                    )
                }
            }

            // Progress Bar if checking
            if (isChecking) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = PrimaryBlue,
                    trackColor = DarkCard
                )
                Text(
                    text = progressText,
                    fontSize = 11.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scanned List
            if (batchList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "📦", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Chưa có thiết bị nào trong danh sách.\nHướng camera vào các tem Serial để gom mã.",
                            fontSize = 13.sp,
                            color = TextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    itemsIndexed(
                        items = batchList,
                        key = { _, item -> item.id }
                    ) { index, item ->
                        val itemNumber = batchList.size - index
                        val modelTag = BarcodeCleaner.getDeviceModel(item.serial)

                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkCard),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onSelectItemDetail(item) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$itemNumber.",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextMuted,
                                        modifier = Modifier.width(26.dp)
                                    )

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = item.serial,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            modelTag?.let {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = it.name,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(it.colorHex),
                                                    modifier = Modifier
                                                        .background(Color(it.colorHex).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Status badges
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (!item.checked) {
                                                StatusPill("Chưa kiểm tra", Color.Gray)
                                            } else if (item.error != null) {
                                                StatusPill("Lỗi tra cứu", DangerRed)
                                            } else {
                                                if (item.ticketCount > 0) {
                                                    StatusPill("🎫 ${item.ticketCount} Ticket", DangerRed)
                                                }
                                                if (item.hasTid) {
                                                    StatusPill("TID: ${item.tid ?: "Đã gán"}", SuccessGreen)
                                                } else {
                                                    StatusPill("Chưa gán TID", WarningYellow)
                                                }
                                                if (item.inInventory && !item.warehouse.isNullOrEmpty()) {
                                                    StatusPill("Kho: ${item.warehouse}", WarningYellow)
                                                }
                                            }
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.deleteBatchItem(item.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete",
                                        tint = TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3 Bottom Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Button 1: Kiểm tra ToroG
                OutlinedButton(
                    onClick = { viewModel.runBatchCheck() },
                    enabled = !isChecking && batchList.isNotEmpty(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kiểm tra (${batchList.count { !it.checked }})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Button 2: Xuất Excel & Zalo
                OutlinedButton(
                    onClick = { viewModel.exportExcel(context) },
                    enabled = batchList.isNotEmpty(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = SuccessGreen)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Xuất Zalo", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                }

                // Button 3: Đẩy Sheet
                Button(
                    onClick = { onOpenPushSheetModal(batchList) },
                    enabled = batchList.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(imageVector = Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Đẩy Sheet", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StatusPill(text: String, color: Color) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
