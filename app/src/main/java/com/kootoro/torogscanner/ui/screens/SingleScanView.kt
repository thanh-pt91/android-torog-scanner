package com.kootoro.torogscanner.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kootoro.torogscanner.camera.CameraPreview
import com.kootoro.torogscanner.data.model.ScannedDeviceItem
import com.kootoro.torogscanner.ui.theme.AccentCyan
import com.kootoro.torogscanner.ui.theme.DangerRed
import com.kootoro.torogscanner.ui.theme.DarkBg
import com.kootoro.torogscanner.ui.theme.DarkCard
import com.kootoro.torogscanner.ui.theme.DarkSurface
import com.kootoro.torogscanner.ui.theme.PrimaryBlue
import com.kootoro.torogscanner.ui.theme.SuccessGreen
import com.kootoro.torogscanner.ui.theme.TextMuted
import com.kootoro.torogscanner.ui.theme.TextPrimary
import com.kootoro.torogscanner.ui.theme.WarningYellow
import com.kootoro.torogscanner.ui.viewmodel.ScannerViewModel
import com.kootoro.torogscanner.util.BarcodeCleaner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleScanView(
    viewModel: ScannerViewModel,
    onOpenPushSheet: (ScannedDeviceItem) -> Unit,
    onOpenManualSearch: () -> Unit
) {
    val isTorchOn by viewModel.isTorchOn.collectAsState()
    val zoomRatio by viewModel.zoomRatio.collectAsState()
    val isCameraPaused by viewModel.isCameraPaused.collectAsState()
    val vfSizeIndex by viewModel.viewfinderSizeIndex.collectAsState()
    val allowedPrefixes by viewModel.allowedPrefixes.collectAsState()
    val isFilterEnabled by viewModel.isFilterEnabled.collectAsState()

    val lastItem by viewModel.lastScannedItem.collectAsState()
    val showSheet by viewModel.showSingleSheet.collectAsState()
    val singleResult by viewModel.singleResult.collectAsState()
    val isLoading by viewModel.isSingleLoading.collectAsState()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    // Photo Gallery Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.decodeImageUri(context, it, isBatch = false) }
    }

    // Viewfinder dimensions
    val (vfHeight, vfPadding, vfLabel) = when (vfSizeIndex) {
        0 -> Triple(95.dp, 55.dp, "Nhỏ")
        1 -> Triple(125.dp, 35.dp, "Vừa")
        else -> Triple(155.dp, 20.dp, "Lớn")
    }

    // Laser beam vertical animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // ─── UPPER ZONE (44%): CAMERA PREVIEW & VIEWFINDER ────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.44f)
                .background(Color.Black)
        ) {
            if (!isCameraPaused) {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    isTorchOn = isTorchOn,
                    zoomRatio = zoomRatio,
                    allowedPrefixes = allowedPrefixes,
                    isFilterEnabled = isFilterEnabled,
                    onBarcodeScanned = { barcodes ->
                        barcodes.firstOrNull()?.let {
                            viewModel.onBarcodeDetected(it.cleanedValue)
                        }
                    }
                )

                // Viewfinder Reticle Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = vfPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(vfHeight)
                            .border(2.dp, PrimaryBlue.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
                    ) {
                        // Laser Beam
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .offset(y = vfHeight * laserOffsetY)
                                .background(AccentCyan)
                        )

                        // Instruction Tag
                        Text(
                            text = "Căn chỉnh tem Serial vào khung",
                            color = TextPrimary.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 6.dp)
                                .background(DarkBg.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // Floating Corner Button: Zoom (Bottom-Left)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBg.copy(alpha = 0.75f))
                        .clickable { viewModel.cycleZoom() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${zoomRatio}x",
                        color = AccentCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Floating Corner Button: Viewfinder Size (Bottom-Right)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBg.copy(alpha = 0.75f))
                        .clickable { viewModel.cycleViewfinderSize() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "⛶ $vfLabel",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                // Cooling Mode Screen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBg),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Camera đang tạm nghỉ",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Đã ngắt luồng cảm biến để làm mát máy & tiết kiệm pin",
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { viewModel.toggleCameraPause() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Chạm để bật lại Camera", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ─── LOWER ZONE (56%): THUMB CONTROL ZONE ─────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.56f)
                .background(DarkBg)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Status Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PrimaryBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Sẵn sàng quét mã Serial",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Căn chỉnh tem vào khung. Hệ thống sẽ tự động tra cứu TID, Ticket 90 ngày và Tồn kho trên ToroG Admin.",
                            fontSize = 12.sp,
                            color = TextMuted,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Quick Actions Block
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Reopen Last Result Button (if available)
                if (lastItem != null) {
                    Button(
                        onClick = { viewModel.reopenLastItem() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentCyan.copy(alpha = 0.12f),
                            contentColor = AccentCyan
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Xem lại kết quả gần nhất: ${lastItem!!.serial}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Row with 2 utility buttons: Gallery & Manual Search
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Thư viện ảnh
                    Button(
                        onClick = { galleryLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .border(1.dp, DarkCard, RoundedCornerShape(12.dp))
                    ) {
                        Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Thư viện ảnh", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // 2. Nhập tay
                    Button(
                        onClick = onOpenManualSearch,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .border(1.dp, DarkCard, RoundedCornerShape(12.dp))
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Nhập tay", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Camera Power / Cooling toggle button
                OutlinedButton(
                    onClick = { viewModel.toggleCameraPause() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isCameraPaused) SuccessGreen else TextMuted
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        tint = if (isCameraPaused) SuccessGreen else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCameraPaused) "Bật lại Camera" else "Tắt Camera (Làm mát máy & tiết kiệm pin)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // ─── BOTTOM SHEET: TOROG RESULT ───────────────────────────────────────
        if (showSheet && singleResult != null) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.closeSingleSheet() },
                sheetState = sheetState,
                containerColor = DarkSurface,
                dragHandle = null
            ) {
                val item = singleResult!!
                val modelTag = BarcodeCleaner.getDeviceModel(item.serial)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header: Serial + Model
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.serial,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Serial", item.serial))
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = AccentCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            modelTag?.let {
                                Text(
                                    text = it.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(it.colorHex)
                                )
                            }
                        }

                        // Recheck Button
                        IconButton(
                            onClick = { viewModel.lookupSingleSerial(item.serial) },
                            enabled = !isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Tra cứu lại",
                                tint = AccentCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = PrimaryBlue, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Đang tra cứu dữ liệu ToroG Admin...", color = TextMuted, fontSize = 13.sp)
                            }
                        }
                    } else if (item.error != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DangerRed.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠️ ${item.error}",
                                color = DangerRed,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else {
                        // 1. TPBank EDC Info Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkCard),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Cấu hình TPBank EDC", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                    val isAssigned = item.hasTid
                                    Text(
                                        text = if (isAssigned) "ĐÃ GÁN TID" else "CHƯA GÁN TID",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAssigned) SuccessGreen else Color.Gray,
                                        modifier = Modifier
                                            .background(
                                                if (isAssigned) SuccessGreen.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
                                                CircleShape
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                InfoRow("Terminal ID (TID):", item.tid ?: "Chưa có", isHighlight = item.hasTid)
                                item.mid?.let { InfoRow("Merchant ID (MID):", it) }
                                item.merchantName?.let { InfoRow("Tên Merchant:", it) }
                                item.storeName?.let { InfoRow("Cửa hàng:", it) }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 2. Ticket 180 Days Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkCard),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Lịch sử Ticket sự cố", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                    val count = item.ticketCount
                                    Text(
                                        text = "$count TICKET",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (count > 0) DangerRed else SuccessGreen,
                                        modifier = Modifier
                                            .background(
                                                if (count > 0) DangerRed.copy(alpha = 0.15f) else SuccessGreen.copy(alpha = 0.15f),
                                                CircleShape
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }

                                if (item.tickets.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    item.tickets.take(3).forEach { tk ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "#${tk.code ?: tk.id ?: ""} • ${tk.title ?: "Sự cố"}",
                                                    fontSize = 12.sp,
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "Trạng thái: ${tk.statusName ?: "Mới"}",
                                                    fontSize = 11.sp,
                                                    color = TextMuted
                                                )
                                            }

                                            tk.ticketUrl?.let { url ->
                                                IconButton(
                                                    onClick = {
                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        }
                                                        context.startActivity(intent)
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.OpenInBrowser,
                                                        contentDescription = "Open",
                                                        tint = PrimaryBlue,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3. Warehouse Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkCard),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Thông tin Tồn kho", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                Spacer(modifier = Modifier.height(8.dp))
                                InfoRow("Kho hiện tại:", item.warehouse ?: "Chưa có thông tin")
                                item.deviceStatus?.let { InfoRow("Trạng thái máy:", it) }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action: Push to Google Sheet
                    Button(
                        onClick = {
                            viewModel.closeSingleSheet()
                            onOpenPushSheet(item)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Đẩy 1 dòng lên Google Sheet", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = TextMuted)
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            color = if (isHighlight) AccentCyan else TextPrimary
        )
    }
}
