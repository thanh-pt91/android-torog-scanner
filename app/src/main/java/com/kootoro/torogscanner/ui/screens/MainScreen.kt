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
    val isFilterEnabled by viewModel.isFilterEnabled.collectAsState()
    val allowedPrefixes by viewModel.allowedPrefixes.collectAsState()
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ToroG POS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "v1.0",
                            fontSize = 11.sp,
                            color = AccentCyan,
                            modifier = Modifier
                                .background(AccentCyan.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                },
                actions = {
                    // Flash / Torch toggle
                    IconButton(onClick = { viewModel.toggleTorch() }) {
                        Icon(
                            imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flash",
                            tint = if (isTorchOn) Color(0xFFFACC15) else TextMuted
                        )
                    }

                    // Zoom toggle
                    Box(
                        modifier = Modifier
                            .clickable { viewModel.cycleZoom() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .background(DarkCard, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${zoomRatio}x",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan
                        )
                    }

                    // Filter toggle chip
                    Box(
                        modifier = Modifier
                            .clickable { viewModel.toggleFilter() }
                            .padding(horizontal = 4.dp)
                            .background(
                                if (isFilterEnabled) PrimaryBlue.copy(alpha = 0.2f) else DarkCard,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isFilterEnabled) "🛡️ Lọc (${allowedPrefixes.size})" else "🛡️ Tắt",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isFilterEnabled) AccentCyan else TextMuted
                        )
                    }

                    // Manual Search
                    IconButton(onClick = { showManualSearchDialog = true }) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = TextPrimary)
                    }

                    // Settings & Auth
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Box {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = TextPrimary)
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
        containerColor = DarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tabs: Tra cứu đơn vs Quét hàng loạt
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurface,
                contentColor = PrimaryBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = PrimaryBlue,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = {
                        Text(
                            text = "🔍 Tra cứu đơn",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp,
                            color = if (selectedTab == 0) TextPrimary else TextMuted
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = {
                        Text(
                            text = "📦 Quét hàng loạt",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp,
                            color = if (selectedTab == 1) TextPrimary else TextMuted
                        )
                    }
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> SingleScanView(
                    viewModel = viewModel,
                    onOpenPushSheet = { item -> pushSheetItems = listOf(item) }
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
