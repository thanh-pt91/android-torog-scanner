package com.kootoro.torogscanner.data.model

import com.google.gson.annotations.SerializedName

// ─── TOROG SERIAL CHECK MODELS ───────────────────────────────────────────────

data class SerialCheckRequest(
    val serial: String,
    val idToken: String? = null
)

data class SerialCheckResponse(
    val success: Boolean = false,
    val serial: String? = null,
    val tpbank: TPBankInfo? = null,
    val tickets: TicketListInfo? = null,
    val warehouse: WarehouseInfo? = null,
    val error: String? = null
)

data class TPBankInfo(
    val hasTid: Boolean = false,
    val tid: String? = null,
    val mid: String? = null,
    val merchantName: String? = null,
    val storeName: String? = null,
    val status: String? = null
)

data class TicketListInfo(
    val total: Int = 0,
    val tickets: List<TicketItem> = emptyList(),
    val rangeNote: String? = null
)

data class TicketItem(
    val id: String? = null,
    val code: String? = null,
    val status: Int? = null,
    val statusName: String? = null,
    val title: String? = null,
    val merchantName: String? = null,
    val storeName: String? = null,
    val createdDate: Any? = null,
    val ticketUrl: String? = null
)

data class WarehouseInfo(
    val inInventory: Boolean = false,
    val warehouseName: String? = null,
    val warehouseCode: String? = null,
    val deviceStatus: String? = null,
    val deviceGroupName: String? = null,
    val deviceTypeName: String? = null
)

// ─── AUTH & RBAC MODELS ───────────────────────────────────────────────────────

data class AuthConfigResponse(
    val enabled: Boolean = false,
    val clientId: String? = null,
    val allowedEmails: List<String> = emptyList()
)

data class VerifyUserRequest(
    val idToken: String
)

data class VerifyUserResponse(
    val success: Boolean = false,
    val isAllowed: Boolean = false,
    val email: String? = null,
    val name: String? = null,
    val picture: String? = null,
    val error: String? = null
)

// ─── PUSH TO GOOGLE SHEET MODELS (13 COLUMNS A:M) ────────────────────────────

data class PushSheetRequest(
    val sheetId: String? = null,
    val sheetName: String? = null,
    val idToken: String? = null,
    val records: List<SheetRecordItem>
)

data class SheetRecordItem(
    val stt: String = "",
    val videoLink: String = "",
    val unpackDate: String = "",
    var tid: String = "",
    val serial: String = "",
    var ticketUrl: String = "",
    var loai: String = "",
    var thietBi: String = "",
    var note1: String = "",
    var boSac: String = "",
    var note2: String = "",
    var mailThuHoi: String = "",
    var guiLaiMay: String = "",
    // Extra metadata for internal checking
    var mid: String = "",
    var merchantName: String = "",
    var warehouse: String = "",
    var checked: Boolean = false
)

data class PushSheetResponse(
    val success: Boolean = false,
    val updatedRows: Int = 0,
    val error: String? = null
)

// ─── UI ITEM STATE ────────────────────────────────────────────────────────────

data class ScannedDeviceItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val serial: String,
    val format: String = "1D Barcode",
    val timestamp: Long = System.currentTimeMillis(),
    var checked: Boolean = false,
    var hasTid: Boolean = false,
    var tid: String? = null,
    var mid: String? = null,
    var merchantName: String? = null,
    var storeName: String? = null,
    var ticketCount: Int = 0,
    var tickets: List<TicketItem> = emptyList(),
    var ticketUrl: String? = null,
    var warehouse: String? = null,
    var deviceStatus: String? = null,
    var inInventory: Boolean = false,
    var error: String? = null
)
