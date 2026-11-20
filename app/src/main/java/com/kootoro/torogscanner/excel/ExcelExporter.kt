package com.kootoro.torogscanner.excel

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.kootoro.torogscanner.data.model.ScannedDeviceItem
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelExporter {

    /**
     * Xuất danh sách thiết bị quét sang file Excel (.xlsx) và mở bảng chia sẻ (Zalo/Telegram/Drive)
     */
    fun exportAndShareExcel(context: Context, items: List<ScannedDeviceItem>): Result<File> {
        return try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Danh Sách Thiết Bị")

            // Header Style
            val headerFont = workbook.createFont().apply {
                bold = true
                color = IndexedColors.WHITE.index
            }
            val headerStyle = workbook.createCellStyle().apply {
                setFont(headerFont)
                fillForegroundColor = IndexedColors.ROYAL_BLUE.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
                alignment = HorizontalAlignment.CENTER
            }

            // Text Cell Style (Để không mất số 0 đầu)
            val textFormat = workbook.createDataFormat().getFormat("@")
            val textStyle = workbook.createCellStyle().apply {
                dataFormat = textFormat
            }

            val headers = listOf(
                "STT",
                "Thời Gian Quét",
                "Mã Serial",
                "Terminal ID (TID)",
                "Merchant ID (MID)",
                "Tên Merchant",
                "Tên Cửa Hàng",
                "Trạng Thái TID",
                "Kho Hiện Tại",
                "Trạng Thái Máy",
                "Ticket (180 Ngày)",
                "Link Ticket Gần Nhất"
            )

            // Create Header Row
            val headerRow = sheet.createRow(0)
            headers.forEachIndexed { index, title ->
                val cell = headerRow.createCell(index)
                cell.setCellValue(title)
                cell.cellStyle = headerStyle
            }

            // Create Data Rows
            val timeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            items.forEachIndexed { index, item ->
                val row = sheet.createRow(index + 1)

                // 0. STT
                row.createCell(0).setCellValue((index + 1).toDouble())

                // 1. Thời gian
                row.createCell(1).setCellValue(timeFormat.format(Date(item.timestamp)))

                // 2. Serial (ép Text)
                val cSerial = row.createCell(2)
                cSerial.cellStyle = textStyle
                cSerial.setCellValue(item.serial)

                // 3. TID (ép Text)
                val cTid = row.createCell(3)
                cTid.cellStyle = textStyle
                cTid.setCellValue(item.tid ?: if (item.hasTid) "Đã gán TID" else "Chưa gán")

                // 4. MID (ép Text)
                val cMid = row.createCell(4)
                cMid.cellStyle = textStyle
                cMid.setCellValue(item.mid ?: "")

                // 5. Merchant
                row.createCell(5).setCellValue(item.merchantName ?: "")

                // 6. Cửa hàng
                row.createCell(6).setCellValue(item.storeName ?: "")

                // 7. Trạng thái TID
                row.createCell(7).setCellValue(if (item.hasTid) "Đã gán TID" else "Chưa gán TID")

                // 8. Kho
                row.createCell(8).setCellValue(item.warehouse ?: "")

                // 9. Trạng thái máy
                row.createCell(9).setCellValue(item.deviceStatus ?: "")

                // 10. Ticket
                row.createCell(10).setCellValue("${item.ticketCount} ticket")

                // 11. Link Ticket
                row.createCell(11).setCellValue(item.ticketUrl ?: "")
            }

            // Auto-size columns
            for (i in headers.indices) {
                sheet.autoSizeColumn(i)
            }

            // Create export file in cache directory
            val exportDir = File(context.cacheDir, "excel").apply { mkdirs() }
            val fileName = "DanhSach_Serial_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.xlsx"
            val file = File(exportDir, fileName)

            FileOutputStream(file).use { out ->
                workbook.write(out)
            }
            workbook.close()

            // Share via Android Intent
            shareFile(context, file)

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Danh sách thiết bị quét ToroG")
            putExtra(Intent.EXTRA_TEXT, "Gửi danh sách ${file.name} từ ToroG Scanner.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooser = Intent.createChooser(intent, "Chia sẻ danh sách Excel qua").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
