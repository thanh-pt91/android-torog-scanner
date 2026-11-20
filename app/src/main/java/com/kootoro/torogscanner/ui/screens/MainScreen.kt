package com.kootoro.torogscanner.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.kootoro.torogscanner.ui.viewmodel.ScannerViewModel
import com.kootoro.torogscanner.ui.viewmodel.UiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: ScannerViewModel) {
    val context = LocalContext.current

    val selectedTab by viewModel.selectedTab.collectAsState()
    val isTorchOn by viewModel.isTorchOn.collectAsState()
    val zoomRatio by viewModel.zoomRatio.collectAsState()
    val isCameraPaused by viewModel.isCameraPaused.collectAsState()
    val isFilterEnabled by viewModel.isFilterEnabled.collectAsState()
    val allowedPrefixes by viewModel.allowedPrefixes.collectAsState()
    val batchList by viewModel.batchList.collectAsState()
    val userState by viewModel.authManager.userState.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showManualSearchDialog by remember { mutableStateOf(false) }
    var manualSerialInput by remember { mutableStateOf("") }

    var pushSheetItems by remember { mutableStateOf<List<ScannedDeviceItem>?>(null) }

    // Toast listener
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is UiEvent.OpenUrl -> {
                    // Handled if needed
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 2.dp)
                    ) {
                        Text(
                            text = "ToroG POS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "v1.0",
                            fontSize = 10.sp,
                            color = AccentCyan,
                            modifier = Modifier
                                .background(AccentCyan.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                },
                actions = {
                    // Camera Cooling / Power toggle
                    IconButton(
                        onClick = { viewModel.toggleCameraPause() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isCameraPaused) Icons.Default.VideocamOff else Icons.Default.Videocam,
                            contentDescription = "Làm mát Camera",
                            tint = if (isCameraPaused) DangerRed else SuccessGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Flash / Torch toggle
                    IconButton(
                        onClick = { viewModel.toggleTorch() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flash",
                            tint = if (isTorchOn) Color(0xFFFACC15) else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Zoom toggle
                    Box(
                        modifier = Modifier
                            .clickable { viewModel.cycleZoom() }
                            .padding(horizontal = 2.dp)
                            .background(DarkCard, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${zoomRatio}x",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan
                        )
                    }

                    // Filter toggle chip
                    Box(
                        modifier = Modifier
                            .clickable { viewModel.toggleFilter() }
                            .padding(horizontal = 2.dp)
                            .background(
                                if (isFilterEnabled) PrimaryBlue.copy(alpha = 0.2f) else DarkCard,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isFilterEnabled) "🛡️ ${allowedPrefixes.size}" else "🛡️ Tắt",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isFilterEnabled) AccentCyan else TextMuted
                        )
                    }

                    // Manual Search
                    IconButton(
                        onClick = { showManualSearchDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Settings & Auth
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            // Auth Indicator Dot
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .align(Alignment.BottomEnd)
                                    .background(
                                        if (userState.isSignedIn && userState.isAllowed) SuccessGreen
                                        else if (userState.isSignedIn) DangerRed
                                        else Color.Transparent,
                                        CircleShape
                                    )
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        bottomBar = {
            Surface(
                color = DarkSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Tab 0: Tra cứu đơn
                    Button(
                        onClick = { viewModel.selectTab(0) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 0) PrimaryBlue else DarkCard
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "🔍 Tra cứu đơn",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            color = if (selectedTab == 0) Color.White else TextMuted
                        )
                    }

                    // Tab 1: Quét hàng loạt
                    Button(
                        onClick = { viewModel.selectTab(1) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 1) PrimaryBlue else DarkCard
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "📦 Quét hàng loạt",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (selectedTab == 1) Color.White else TextMuted
                            )
                            if (batchList.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(AccentCyan, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "${batchList.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = DarkBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> SingleScanView(
                    viewModel = viewModel,
                    onOpenPushSheet = { item -> pushSheetItems = listOf(item) },
                    onOpenManualSearch = { showManualSearchDialog = true }
                )
                1 -> BatchScanView(
                    viewModel = viewModel,
                    onOpenPushSheetModal = { items -> pushSheetItems = items },
                    onSelectItemDetail = { item -> viewModel.lookupSingleSerial(item.serial) }
                )
            }
        }
    }

    // Manual Search Dialog
    if (showManualSearchDialog) {
        AlertDialog(
            onDismissRequest = { showManualSearchDialog = false },
            title = { Text("Nhập mã Serial thủ công", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = manualSerialInput,
                    onValueChange = { manualSerialInput = it },
                    label = { Text("Mã Serial (VD: N820W706025)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = DarkCard
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = manualSerialInput.trim().uppercase()
                        if (trimmed.isNotEmpty()) {
                            showManualSearchDialog = false
                            manualSerialInput = ""
                            viewModel.onBarcodeDetected(trimmed)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Tra cứu ngay")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showManualSearchDialog = false }) {
                    Text("Hủy")
                }
            },
            containerColor = DarkSurface
        )
    }

    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettingsDialog = false }
        )
    }

    // Push Sheet BottomSheet Modal
    if (pushSheetItems != null) {
        PushSheetBottomSheet(
            items = pushSheetItems!!,
            viewModel = viewModel,
            onDismiss = { pushSheetItems = null }
        )
    }
}
