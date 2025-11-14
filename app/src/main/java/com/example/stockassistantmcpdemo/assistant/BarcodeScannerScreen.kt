package com.example.stockassistantmcpdemo.assistant

import android.annotation.SuppressLint
import android.content.Context
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.common.InputImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.Alignment

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun BarcodeScannerScreen(
    onScanResult: (String) -> Unit,
    onClose: (() -> Unit)? = null
) {
    // Optional: configure the formats you care about (EAN-13/UPC common in retail)
    val options = remember {
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_QR_CODE
            )
            .build()
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                val previewView = PreviewView(context)
                startCamera(context, previewView, options, onScanResult)
                previewView
            }
        )

        // (Optional) overlay close button
        if (onClose != null) {
            FloatingActionButton(
                onClick = onClose,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = androidx.compose.ui.Modifier
                    .align(Alignment.TopEnd)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
private fun startCamera(
    context: Context,
    previewView: PreviewView,
    options: BarcodeScannerOptions,
    onScanResult: (String) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder()
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        val analysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        val scanner = BarcodeScanning.getClient(options)

        analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        // Read the first useful barcode found; you can expand to multi-scan later
                        for (barcode in barcodes) {
                            val raw = barcode.rawValue
                            if (!raw.isNullOrBlank()) {
                                onScanResult(raw)
                                break // stop after first
                            }
                        }
                    }
                    .addOnCompleteListener { imageProxy.close() }
            } else {
                imageProxy.close()
            }
        }

        val selector = CameraSelector.DEFAULT_BACK_CAMERA
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                context as androidx.lifecycle.LifecycleOwner,
                selector,
                preview,
                analysis
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }, ContextCompat.getMainExecutor(context))
}
