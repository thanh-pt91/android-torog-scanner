package com.kootoro.torogscanner.camera

import android.graphics.Rect
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.kootoro.torogscanner.util.BarcodeCleaner

data class ScannedBarcodeResult(
    val rawValue: String,
    val cleanedValue: String,
    val format: Int,
    val boundingBox: Rect?,
    val imageWidth: Int,
    val imageHeight: Int,
    val rotationDegrees: Int
)

class BarcodeAnalyzer(
    private val allowedPrefixes: () -> List<String>,
    private val isFilterEnabled: () -> Boolean,
    private val onBarcodesDetected: (List<ScannedBarcodeResult>) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_ITF
        )
        .build()

    private val scanner = BarcodeScanning.getClient(options)
    private var isBusy = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null && !isBusy) {
            isBusy = true
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    val results = mutableListOf<ScannedBarcodeResult>()
                    val prefixes = allowedPrefixes()
                    val filterEnabled = isFilterEnabled()

                    for (barcode in barcodes) {
                        val raw = barcode.rawValue ?: continue
                        val cleaned = BarcodeCleaner.cleanBarcode(raw)
                        if (BarcodeCleaner.isValidSerial(cleaned, prefixes, filterEnabled)) {
                            results.add(
                                ScannedBarcodeResult(
                                    rawValue = raw,
                                    cleanedValue = cleaned,
                                    format = barcode.format,
                                    boundingBox = barcode.boundingBox,
                                    imageWidth = inputImage.width,
                                    imageHeight = inputImage.height,
                                    rotationDegrees = rotationDegrees
                                )
                            )
                        }
                    }

                    if (results.isNotEmpty()) {
                        onBarcodesDetected(results)
                    }
                }
                .addOnFailureListener {
                    // Ignore transient analysis error
                }
                .addOnCompleteListener {
                    isBusy = false
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
