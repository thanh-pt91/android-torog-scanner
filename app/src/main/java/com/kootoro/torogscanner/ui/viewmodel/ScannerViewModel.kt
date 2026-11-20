package com.kootoro.torogscanner.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kootoro.torogscanner.data.api.RetrofitClient
import com.kootoro.torogscanner.data.auth.GoogleAuthManager
import com.kootoro.torogscanner.data.model.ScannedDeviceItem
import com.kootoro.torogscanner.data.model.SheetRecordItem
import com.kootoro.torogscanner.data.repository.ToroGRepository
import com.kootoro.torogscanner.excel.ExcelExporter
import com.kootoro.torogscanner.util.BarcodeCleaner
import com.kootoro.torogscanner.util.FeedbackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    data class OpenUrl(val url: String) : UiEvent()
}

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ToroGRepository()
    val authManager = GoogleAuthManager(application)
    val feedbackManager = FeedbackManager(application)

    private val prefs = application.getSharedPreferences("torog_scanner_settings", Context.MODE_PRIVATE)

    // ─── TAB SELECTION ────────────────────────────────────────────────────────
    private val _selectedTab = MutableStateFlow(0) // 0: Single, 1: Batch
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // ─── CAMERA CONTROLS ──────────────────────────────────────────────────────
    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _zoomRatio = MutableStateFlow(1.0f)
    val zoomRatio: StateFlow<Float> = _zoomRatio.asStateFlow()

    private val _isCameraPaused = MutableStateFlow(false)
    val isCameraPaused: StateFlow<Boolean> = _isCameraPaused.asStateFlow()

    fun toggleCameraPause() {
        _isCameraPaused.value = !_isCameraPaused.value
    }

    // 0: Small (100dp), 1: Medium (140dp), 2: Large (180dp)
    private val _viewfinderSizeIndex = MutableStateFlow(1)
    val viewfinderSizeIndex: StateFlow<Int> = _viewfinderSizeIndex.asStateFlow()

    fun cycleViewfinderSize() {
        _viewfinderSizeIndex.value = (_viewfinderSizeIndex.value + 1) % 3
    }

    private val _lastScannedItem = MutableStateFlow<ScannedDeviceItem?>(null)
    val lastScannedItem: StateFlow<ScannedDeviceItem?> = _lastScannedItem.asStateFlow()

    fun reopenLastItem() {
        _lastScannedItem.value?.let { item ->
            _singleResult.value = item
            _showSingleSheet.value = true
        }
    }

    // ─── PREFIX FILTER ────────────────────────────────────────────────────────
    private val _isFilterEnabled = MutableStateFlow(prefs.getBoolean("filter_enabled", true))
    val isFilterEnabled: StateFlow<Boolean> = _isFilterEnabled.asStateFlow()

    private val _allowedPrefixes = MutableStateFlow(
        prefs.getString("allowed_prefixes", null)?.split(",")?.map { it.trim().uppercase() }?.filter { it.isNotEmpty() }
            ?: BarcodeCleaner.DEFAULT_PREFIXES
    )
    val allowedPrefixes: StateFlow<List<String>> = _allowedPrefixes.asStateFlow()

    // ─── SERVER CONFIG ────────────────────────────────────────────────────────
    private val _serverUrl = MutableStateFlow(prefs.getString("server_url", RetrofitClient.DEFAULT_SERVER_URL) ?: RetrofitClient.DEFAULT_SERVER_URL)
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _googleClientId = MutableStateFlow<String?>(null)
    val googleClientId: StateFlow<String?> = _googleClientId.asStateFlow()

    // ─── SINGLE SCAN STATE ────────────────────────────────────────────────────
    private val _singleSerial = MutableStateFlow<String?>(null)
    val singleSerial: StateFlow<String?> = _singleSerial.asStateFlow()

    private val _isSingleLoading = MutableStateFlow(false)
    val isSingleLoading: StateFlow<Boolean> = _isSingleLoading.asStateFlow()

    private val _singleResult = MutableStateFlow<ScannedDeviceItem?>(null)
    val singleResult: StateFlow<ScannedDeviceItem?> = _singleResult.asStateFlow()

    private val _showSingleSheet = MutableStateFlow(false)
    val showSingleSheet: StateFlow<Boolean> = _showSingleSheet.asStateFlow()

    // ─── BATCH SCAN STATE ─────────────────────────────────────────────────────
    private val _batchList = MutableStateFlow<List<ScannedDeviceItem>>(emptyList())
    val batchList: StateFlow<List<ScannedDeviceItem>> = _batchList.asStateFlow()

    private val _isBatchChecking = MutableStateFlow(false)
    val isBatchChecking: StateFlow<Boolean> = _isBatchChecking.asStateFlow()

    private val _batchProgress = MutableStateFlow(0f) // 0.0 to 1.0
    val batchProgress: StateFlow<Float> = _batchProgress.asStateFlow()

    private val _batchProgressText = MutableStateFlow("")
    val batchProgressText: StateFlow<String> = _batchProgressText.asStateFlow()

    // ─── UI EVENTS ────────────────────────────────────────────────────────────
    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    init {
        RetrofitClient.updateBaseUrl(_serverUrl.value)
        fetchAuthConfig()
    }

    fun selectTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun toggleTorch() {
        _isTorchOn.value = !_isTorchOn.value
    }

    fun cycleZoom() {
        _zoomRatio.value = when (_zoomRatio.value) {
            1.0f -> 1.5f
            1.5f -> 2.0f
            else -> 1.0f
        }
    }

    fun toggleFilter() {
        val next = !_isFilterEnabled.value
        _isFilterEnabled.value = next
        prefs.edit().putBoolean("filter_enabled", next).apply()
    }

    fun updatePrefixes(newPrefixes: List<String>) {
        _allowedPrefixes.value = newPrefixes
        prefs.edit().putString("allowed_prefixes", newPrefixes.joinToString(",")).apply()
    }

    fun updateServerUrl(url: String) {
        _serverUrl.value = url
        prefs.edit().putString("server_url", url).apply()
        RetrofitClient.updateBaseUrl(url)
        fetchAuthConfig()
    }

    private fun fetchAuthConfig() {
        viewModelScope.launch {
            repository.getAuthConfig().onSuccess { cfg ->
                if (cfg.enabled && !cfg.clientId.isNullOrEmpty()) {
                    _googleClientId.value = cfg.clientId
                }
            }
        }
    }

    // ─── SCAN LOGIC ───────────────────────────────────────────────────────────

    fun onBarcodeDetected(cleanedSerial: String) {
        if (_selectedTab.value == 0) {
            // SINGLE MODE
            if (_showSingleSheet.value && _singleSerial.value == cleanedSerial) {
                return // Already showing
            }
            feedbackManager.triggerHaptic(40)
            feedbackManager.playBeep()
            lookupSingleSerial(cleanedSerial)
        } else {
            // BATCH MODE
            val current = _batchList.value
            if (current.none { it.serial.equals(cleanedSerial, ignoreCase = true) }) {
                feedbackManager.triggerHaptic(40)
                feedbackManager.playBeep()
                val newItem = ScannedDeviceItem(serial = cleanedSerial)
                _batchList.value = listOf(newItem) + current
            }
        }
    }

    fun lookupSingleSerial(serial: String) {
        _singleSerial.value = serial
        _showSingleSheet.value = true
        _isSingleLoading.value = true

        val item = ScannedDeviceItem(serial = serial)
        _singleResult.value = item

        viewModelScope.launch {
            val user = authManager.userState.value
            val idToken = user.idToken

            repository.checkSerial(serial, idToken)
                .onSuccess { res ->
                    _isSingleLoading.value = false
                    if (res.success) {
                        item.checked = true
                        item.hasTid = res.tpbank?.hasTid ?: false
                        item.tid = res.tpbank?.tid ?: if (item.hasTid) "Đã gán TID" else "Chưa gán"
                        item.mid = res.tpbank?.mid
                        item.merchantName = res.tpbank?.merchantName
                        item.storeName = res.tpbank?.storeName
                        item.ticketCount = res.tickets?.total ?: 0
                        item.tickets = res.tickets?.tickets ?: emptyList()
                        item.ticketUrl = item.tickets.firstOrNull()?.ticketUrl
                            ?: item.tickets.firstOrNull()?.id?.let { "https://torog-admin.kootoro.com/ticket-processing/$it" }
                        item.warehouse = res.warehouse?.warehouseName
                            ?: if (res.warehouse?.inInventory == true) "Có trong kho" else "Chưa có"
                        item.deviceStatus = res.warehouse?.deviceStatus
                        item.inInventory = res.warehouse?.inInventory ?: false
                        _singleResult.value = item.copy()
                        _lastScannedItem.value = item.copy()
                    } else {
                        item.error = res.error ?: "Lỗi tra cứu ToroG"
                        _singleResult.value = item.copy()
                        _lastScannedItem.value = item.copy()
                    }
                }
                .onFailure { err ->
                    _isSingleLoading.value = false
                    item.error = err.message ?: "Lỗi kết nối máy chủ"
                    _singleResult.value = item.copy()
                    _lastScannedItem.value = item.copy()
                }
        }
    }

    fun decodeImageUri(context: Context, uri: Uri, isBatch: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputImage = com.google.mlkit.vision.common.InputImage.fromFilePath(context, uri)
                val scanner = com.google.mlkit.vision.barcode.BarcodeScanning.getClient()
                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        if (barcodes.isEmpty()) {
                            viewModelScope.launch {
                                _uiEvents.emit(UiEvent.ShowToast("Không tìm thấy mã vạch nào trong ảnh"))
                            }
                            return@addOnSuccessListener
                        }
                        val validList = mutableListOf<String>()
                        barcodes.forEach { b ->
                            val raw = b.rawValue ?: ""
                            val cleaned = BarcodeCleaner.clean(raw)
                            val isValid = if (_isFilterEnabled.value) {
                                BarcodeCleaner.isValidSerial(cleaned, _allowedPrefixes.value)
                            } else cleaned.isNotEmpty()
                            if (isValid && !validList.contains(cleaned)) {
                                validList.add(cleaned)
                            }
                        }
                        if (validList.isEmpty()) {
                            viewModelScope.launch {
                                _uiEvents.emit(UiEvent.ShowToast("Không có mã nào khớp bộ lọc tiền tố"))
                            }
                            return@addOnSuccessListener
                        }
                        if (!isBatch) {
                            lookupSingleSerial(validList.first())
                        } else {
                            val currentList = _batchList.value.toMutableList()
                            var added = 0
                            validList.forEach { s ->
                                if (currentList.none { it.serial.equals(s, ignoreCase = true) }) {
                                    currentList.add(0, ScannedDeviceItem(serial = s))
                                    added++
                                }
                            }
                            _batchList.value = currentList
                            feedbackManager.onBatchItemAdded()
                            viewModelScope.launch {
                                _uiEvents.emit(UiEvent.ShowToast("Đã trích xuất và thêm $added mã vào danh sách"))
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        viewModelScope.launch {
                            _uiEvents.emit(UiEvent.ShowToast("Lỗi đọc ảnh: ${e.message}"))
                        }
                    }
            } catch (e: Exception) {
                viewModelScope.launch {
                    _uiEvents.emit(UiEvent.ShowToast("Lỗi mở ảnh: ${e.message}"))
                }
            }
        }
    }

    fun closeSingleSheet() {
        _showSingleSheet.value = false
    }

    fun deleteBatchItem(id: String) {
        _batchList.value = _batchList.value.filter { it.id != id }
    }

    fun clearBatchList() {
        _batchList.value = emptyList()
    }

    // ─── BATCH TOROG CHECK ────────────────────────────────────────────────────

    fun runBatchCheck() {
        val unchecked = _batchList.value.filter { !it.checked }
        if (unchecked.isEmpty()) {
            emitToast("Tất cả thiết bị đã được tra cứu!")
            return
        }

        val user = authManager.userState.value
        if (!user.isSignedIn) {
            emitToast("🔒 Vui lòng đăng nhập Google để tra cứu danh sách ToroG!")
            return
        }
        if (!user.isAllowed) {
            emitToast("⛔ Tài khoản ${user.email} chưa được cấp quyền tra cứu ToroG!")
            return
        }

        _isBatchChecking.value = true
        _batchProgress.value = 0f
        _batchProgressText.value = "Đang kiểm tra: 0/${unchecked.size}..."

        viewModelScope.launch(Dispatchers.IO) {
            val total = unchecked.size
            var done = 0

            val chunkSize = 3
            val chunks = unchecked.chunked(chunkSize)

            for (chunk in chunks) {
                chunk.map { item ->
                    async {
                        val res = repository.checkSerial(item.serial, user.idToken)
                        res.onSuccess { data ->
                            if (data.success) {
                                item.checked = true
                                item.hasTid = data.tpbank?.hasTid ?: false
                                item.tid = data.tpbank?.tid ?: if (item.hasTid) "Đã gán TID" else "Chưa gán"
                                item.mid = data.tpbank?.mid
                                item.merchantName = data.tpbank?.merchantName
                                item.storeName = data.tpbank?.storeName
                                item.ticketCount = data.tickets?.total ?: 0
                                item.tickets = data.tickets?.tickets ?: emptyList()
                                item.ticketUrl = item.tickets.firstOrNull()?.ticketUrl
                                    ?: item.tickets.firstOrNull()?.id?.let { "https://torog-admin.kootoro.com/ticket-processing/$it" }
                                item.warehouse = data.warehouse?.warehouseName
                                    ?: if (data.warehouse?.inInventory == true) "Có trong kho" else "Chưa có"
                                item.deviceStatus = data.warehouse?.deviceStatus
                                item.inInventory = data.warehouse?.inInventory ?: false
                            } else {
                                item.error = data.error
                            }
                        }.onFailure { err ->
                            item.error = err.message
                        }
                    }
                }.awaitAll()

                done += chunk.size
                _batchProgress.value = done.toFloat() / total
                _batchProgressText.value = "Đang kiểm tra: $done/$total..."
                // Force state update
                _batchList.value = _batchList.value.map { it.copy() }
            }

            _isBatchChecking.value = false
            emitToast("✅ Đã hoàn thành tra cứu toàn bộ danh sách!")
        }
    }

    // ─── EXCEL EXPORT ─────────────────────────────────────────────────────────

    fun exportExcel(context: Context) {
        val list = _batchList.value
        if (list.isEmpty()) {
            emitToast("Danh sách đang trống, chưa có mã để xuất Excel!")
            return
        }
        ExcelExporter.exportAndShareExcel(context, list)
            .onSuccess { file ->
                emitToast("✅ Đã tạo file ${file.name} và mở chia sẻ Zalo!")
            }
            .onFailure { err ->
                emitToast("❌ Lỗi xuất file Excel: ${err.message}")
            }
    }

    // ─── GOOGLE SHEETS PUSH ───────────────────────────────────────────────────

    fun pushRecordsToSheet(
        items: List<ScannedDeviceItem>,
        loai: String,
        thietBi: String,
        note1: String,
        boSac: String,
        note2: String,
        mailThuHoi: String,
        guiLaiMay: String,
        onComplete: (Boolean) -> Unit
    ) {
        val user = authManager.userState.value
        if (!user.isSignedIn) {
            emitToast("🔒 Vui lòng đăng nhập Google trước khi đẩy dữ liệu vào Sheet!")
            onComplete(false)
            return
        }
        if (!user.isAllowed) {
            emitToast("⛔ Tài khoản ${user.email} chưa được cấp quyền đẩy Sheet!")
            onComplete(false)
            return
        }

        viewModelScope.launch {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val todayStr = dateFormat.format(Date())

            val records = items.map { item ->
                SheetRecordItem(
                    stt = "",
                    videoLink = "",
                    unpackDate = todayStr,
                    tid = item.tid ?: if (item.hasTid) "Đã gán TID" else "",
                    serial = item.serial,
                    ticketUrl = item.ticketUrl ?: "",
                    loai = loai,
                    thietBi = thietBi,
                    note1 = note1,
                    boSac = boSac,
                    note2 = note2,
                    mailThuHoi = mailThuHoi,
                    guiLaiMay = guiLaiMay,
                    mid = item.mid ?: "",
                    merchantName = item.merchantName ?: "",
                    warehouse = item.warehouse ?: "",
                    checked = item.checked
                )
            }

            repository.pushToSheet(records, user.idToken)
                .onSuccess { res ->
                    emitToast("✅ Đã đẩy thành công ${res.updatedRows} dòng lên Google Sheet!")
                    feedbackManager.triggerHaptic(60)
                    feedbackManager.playBeep()
                    onComplete(true)
                }
                .onFailure { err ->
                    emitToast("❌ Lỗi: ${err.message}")
                    onComplete(false)
                }
        }
    }

    private fun emitToast(msg: String) {
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShowToast(msg))
        }
    }

    override fun onCleared() {
        super.onCleared()
        feedbackManager.release()
    }
}
