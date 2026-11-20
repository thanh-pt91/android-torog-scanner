package com.kootoro.torogscanner.util

data class DeviceModelInfo(
    val name: String,
    val colorHex: Long
)

object BarcodeCleaner {

    val DEFAULT_PREFIXES = listOf("PE", "PB", "N82", "F200")

    /**
     * Làm sạch chuỗi giải mã từ mã vạch (xóa FSN, S/N, SN: và khoảng trắng)
     */
    fun cleanBarcode(raw: String): String {
        return raw.trim().uppercase()
            .replace(Regex("^(FSN|S/N|SN)\\s*[:\\-]?\\s*", RegexOption.IGNORE_CASE), "")
            .replace("\\s+".toRegex(), "")
    }

    /**
     * Kiểm tra chuỗi có hợp lệ theo bộ lọc tiền tố hay không
     */
    fun isValidSerial(serial: String, allowedPrefixes: List<String>, isFilterEnabled: Boolean): Boolean {
        val cleaned = cleanBarcode(serial)
        if (cleaned.length < 6 || cleaned.length > 30) return false

        // Bỏ qua nếu là số IMEI
        if (cleaned.startsWith("IMEI") || (cleaned.length == 15 && cleaned.all { it.isDigit() })) {
            return false
        }

        if (!isFilterEnabled || allowedPrefixes.isEmpty()) {
            return true
        }

        return allowedPrefixes.any { prefix ->
            cleaned.startsWith(prefix.trim().uppercase())
        }
    }

    /**
     * Nhận diện dòng máy dựa trên tiền tố Serial
     */
    fun getDeviceModel(serial: String): DeviceModelInfo? {
        val s = serial.uppercase()
        return when {
            s.startsWith("PE") -> DeviceModelInfo("Pax PE", 0xFF38BDF8)
            s.startsWith("PB") -> DeviceModelInfo("Pax PB", 0xFF818CF8)
            s.startsWith("N82") -> DeviceModelInfo("Nexgo N82", 0xFF34D399)
            s.startsWith("F200") -> DeviceModelInfo("Feitian F200", 0xFFFBBF24)
            else -> null
        }
    }
}
